/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.featureflags.launchdarkly;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateLDClientToOpenFeature extends Recipe {

    private static final MethodMatcher NEW_CLIENT = new MethodMatcher("com.launchdarkly.sdk.server.LDClient <constructor>(..)");

    String displayName = "Migrate LaunchDarkly `LDClient` construction to OpenFeature";

    String description = "Replace `new LDClient(...)` with `OpenFeatureAPI.getInstance().getClient()`. " +
            "When the client is assigned to a local variable, the one-time provider bootstrap " +
            "`OpenFeatureAPI.getInstance().setProviderAndWait(new Provider(...))` is generated from the original " +
            "SDK key and configuration. In other positions (such as fields) the original configuration is preserved " +
            "in a `TODO` comment instead, as a statement cannot be inserted there.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.launchdarkly.sdk.server.LDClient", null), new JavaVisitor<ExecutionContext>() {

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = (J.Block) super.visitBlock(block, ctx);
                if (getCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration) {
                    // Class body: a field initializer cannot be preceded by a bare statement, so leave the
                    // construction to the comment fallback in visitNewClass.
                    return b;
                }
                return b.withStatements(ListUtils.flatMap(b.getStatements(), statement -> {
                    if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                        if (vd.getVariables().size() == 1) {
                            Expression init = vd.getVariables().get(0).getInitializer();
                            if (init instanceof J.NewClass && NEW_CLIENT.matches(init)) {
                                return generateProviderBootstrap(vd, (J.NewClass) init, ctx);
                            }
                        }
                    }
                    return statement;
                }));
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_CLIENT.matches(nc) && !isLocalVariableInitializer()) {
                    maybeAddImport("dev.openfeature.sdk.OpenFeatureAPI");
                    doAfterVisit(new ChangeType("com.launchdarkly.sdk.server.LDClient", "dev.openfeature.sdk.Client", null).getVisitor());
                    J applied = getClient(ctx).apply(getCursor(), nc.getCoordinates().replace());
                    // The LaunchDarkly configuration cannot be carried over automatically here; surface it so the
                    // one-time OpenFeature provider bootstrap can be wired up by hand instead of being lost.
                    TextComment comment = new TextComment(true,
                            " TODO Configure the OpenFeature provider, e.g. " +
                                    "OpenFeatureAPI.getInstance().setProviderAndWait(new Provider(" + arguments(nc) + ")) ",
                            " ", Markers.EMPTY);
                    Space prefix = nc.getPrefix();
                    return applied.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), comment)));
                }
                return nc;
            }

            private List<Statement> generateProviderBootstrap(J.VariableDeclarations vd, J.NewClass init, ExecutionContext ctx) {
                maybeAddImport("dev.openfeature.sdk.OpenFeatureAPI");
                maybeAddImport("com.launchdarkly.openfeature.serverprovider.Provider");
                doAfterVisit(new ChangeType("com.launchdarkly.sdk.server.LDClient", "dev.openfeature.sdk.Client", null).getVisitor());

                List<Expression> ldArgs = init.getArguments().stream()
                        .filter(a -> !(a instanceof J.Empty))
                        .collect(Collectors.toList());
                // LDClient and Provider share the same parameter order: (String sdkKey[, LDConfig config]).
                // Type the placeholders so the Provider constructor resolves and the new expression is attributed.
                String placeholders = IntStream.range(0, ldArgs.size())
                        .mapToObj(i -> i == 0 ? "#{any(java.lang.String)}" : "#{any(com.launchdarkly.sdk.server.LDConfig)}")
                        .collect(Collectors.joining(", "));
                Statement providerSetup = JavaTemplate.builder(
                                "OpenFeatureAPI.getInstance().setProviderAndWait(new Provider(" + placeholders + "))")
                        .imports("dev.openfeature.sdk.OpenFeatureAPI", "com.launchdarkly.openfeature.serverprovider.Provider")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx,
                                "sdk-1.+", "launchdarkly-openfeature-serverprovider-1.+", "launchdarkly-java-server-sdk-6.+"))
                        .build()
                        .apply(new Cursor(getCursor(), vd), vd.getCoordinates().replace(), ldArgs.toArray());

                J.VariableDeclarations getClientVd = getClient(ctx)
                        .apply(new Cursor(getCursor(), vd), init.getCoordinates().replace());

                return asList(providerSetup, getClientVd);
            }

            private JavaTemplate getClient(ExecutionContext ctx) {
                return JavaTemplate.builder("OpenFeatureAPI.getInstance().getClient()")
                        .imports("dev.openfeature.sdk.OpenFeatureAPI")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                        .build();
            }

            private boolean isLocalVariableInitializer() {
                // new LDClient(...) -> NamedVariable -> VariableDeclarations -> Block whose parent is not a class.
                if (!(getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable)) {
                    return false;
                }
                Cursor block = getCursor().dropParentUntil(p -> p instanceof J.Block || p instanceof J.ClassDeclaration);
                return block.getValue() instanceof J.Block &&
                        !(block.getParentTreeCursor().getValue() instanceof J.ClassDeclaration);
            }

            private String arguments(J.NewClass nc) {
                return nc.getArguments().stream()
                        .filter(a -> !(a instanceof J.Empty))
                        .map(a -> a.printTrimmed(getCursor()))
                        .collect(Collectors.joining(", "));
            }
        });
    }
}
