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
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateLDValueToValue extends Recipe {

    private static final MethodMatcher OF_STRING = new MethodMatcher("com.launchdarkly.sdk.LDValue of(String)");
    private static final MethodMatcher OF_BOOLEAN = new MethodMatcher("com.launchdarkly.sdk.LDValue of(boolean)");
    private static final MethodMatcher OF_INT = new MethodMatcher("com.launchdarkly.sdk.LDValue of(int)");
    private static final MethodMatcher OF_DOUBLE = new MethodMatcher("com.launchdarkly.sdk.LDValue of(double)");
    private static final MethodMatcher OF_NULL = new MethodMatcher("com.launchdarkly.sdk.LDValue ofNull()");
    private static final MethodMatcher JSON_VARIATION = new MethodMatcher("com.launchdarkly.sdk.server.LDClient jsonValueVariation(String, com.launchdarkly.sdk.LDContext, com.launchdarkly.sdk.LDValue)");
    private static final MethodMatcher JSON_VARIATION_DETAIL = new MethodMatcher("com.launchdarkly.sdk.server.LDClient jsonValueVariationDetail(String, com.launchdarkly.sdk.LDContext, com.launchdarkly.sdk.LDValue)");

    String displayName = "Migrate LaunchDarkly `LDValue` and `jsonValueVariation` to OpenFeature";

    String description = "Migrate `jsonValueVariation`/`jsonValueVariationDetail` to OpenFeature's `getObjectValue`/" +
            "`getObjectDetails` (reordering the context argument to last) and convert scalar `LDValue.of(...)` and " +
            "`LDValue.ofNull()` defaults to `dev.openfeature.sdk.Value`. To keep the result compilable, this recipe " +
            "is skipped for files that use the structured builders `LDValue.buildObject()`, `LDValue.buildArray()`, " +
            "`LDValue.parse(...)` or `LDValue.of(long)`, which require manual migration to `Structure`/`List<Value>`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.and(
                new UsesType<>("com.launchdarkly.sdk.LDValue", null),
                Preconditions.not(new UsesMethod<>("com.launchdarkly.sdk.LDValue buildObject()")),
                Preconditions.not(new UsesMethod<>("com.launchdarkly.sdk.LDValue buildArray()")),
                Preconditions.not(new UsesMethod<>("com.launchdarkly.sdk.LDValue parse(..)")),
                Preconditions.not(new UsesMethod<>("com.launchdarkly.sdk.LDValue of(long)")));

        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Register after every supported LDValue.of(...) has become a new Value(...), so only bare type
                // references remain to retype. Gated by the precondition, so unsupported builders are never reached.
                doAfterVisit(new ChangeType("com.launchdarkly.sdk.LDValue", "dev.openfeature.sdk.Value", null).getVisitor());
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (OF_STRING.matches(m)) {
                    return newValue(m, ctx, "java.lang.String");
                }
                if (OF_BOOLEAN.matches(m)) {
                    return newValue(m, ctx, "boolean");
                }
                if (OF_INT.matches(m)) {
                    return newValue(m, ctx, "int");
                }
                if (OF_DOUBLE.matches(m)) {
                    return newValue(m, ctx, "double");
                }
                if (OF_NULL.matches(m)) {
                    maybeAddImport("dev.openfeature.sdk.Value");
                    return JavaTemplate.builder("new Value()")
                            .imports("dev.openfeature.sdk.Value")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace())
                            .withPrefix(m.getPrefix());
                }
                if (JSON_VARIATION.matches(m)) {
                    return contextLast(m, "getObjectValue");
                }
                if (JSON_VARIATION_DETAIL.matches(m)) {
                    return contextLast(m, "getObjectDetails");
                }
                return m;
            }

            private J newValue(J.MethodInvocation m, ExecutionContext ctx, String parameterType) {
                maybeAddImport("dev.openfeature.sdk.Value");
                return JavaTemplate.builder("new Value(#{any(" + parameterType + ")})")
                        .imports("dev.openfeature.sdk.Value")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0))
                        .withPrefix(m.getPrefix());
            }

            // jsonValueVariation(key, context, value) -> getObjectValue(key, value, context)
            private J.MethodInvocation contextLast(J.MethodInvocation m, String newName) {
                List<Expression> args = m.getArguments();
                Expression key = args.get(0);
                Expression context = args.get(1);
                Expression value = args.get(2);
                List<Expression> reordered = Arrays.asList(
                        key,
                        value.withPrefix(context.getPrefix()),
                        context.withPrefix(value.getPrefix()));
                return m.withName(m.getName().withSimpleName(newName)).withArguments(reordered);
            }
        });
    }
}
