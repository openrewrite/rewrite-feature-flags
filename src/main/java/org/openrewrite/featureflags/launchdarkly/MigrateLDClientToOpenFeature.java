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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateLDClientToOpenFeature extends Recipe {

    private static final MethodMatcher NEW_CLIENT = new MethodMatcher("com.launchdarkly.sdk.server.LDClient <constructor>(..)");

    String displayName = "Migrate LaunchDarkly `LDClient` construction to OpenFeature";

    String description = "Replace `new LDClient(...)` with `OpenFeatureAPI.getInstance().getClient()`. " +
            "Provider setup (`OpenFeatureAPI.setProviderAndWait(...)`) is a one-time bootstrap that must be " +
            "configured manually, so the original SDK key and configuration are preserved in a `TODO` comment " +
            "rather than dropped silently.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.launchdarkly.sdk.server.LDClient", null), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_CLIENT.matches(nc)) {
                    maybeAddImport("dev.openfeature.sdk.OpenFeatureAPI");
                    doAfterVisit(new ChangeType("com.launchdarkly.sdk.server.LDClient", "dev.openfeature.sdk.Client", null).getVisitor());
                    J applied = JavaTemplate.builder("OpenFeatureAPI.getInstance().getClient()")
                            .imports("dev.openfeature.sdk.OpenFeatureAPI")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                            .build()
                            .apply(getCursor(), nc.getCoordinates().replace());
                    // The LaunchDarkly configuration cannot be carried over automatically; surface it so the
                    // one-time OpenFeature provider bootstrap can be wired up by hand instead of being lost.
                    String args = nc.getArguments().stream()
                            .filter(a -> !(a instanceof J.Empty))
                            .map(a -> a.printTrimmed(getCursor()))
                            .collect(Collectors.joining(", "));
                    TextComment comment = new TextComment(true,
                            " TODO Configure the OpenFeature provider, e.g. " +
                                    "OpenFeatureAPI.getInstance().setProviderAndWait(new Provider(" + args + ")) ",
                            " ", Markers.EMPTY);
                    Space prefix = nc.getPrefix();
                    return applied.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), comment)));
                }
                return nc;
            }
        });
    }
}
