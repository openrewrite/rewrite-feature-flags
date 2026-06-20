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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateLDContextToEvaluationContext extends Recipe {

    private static final MethodMatcher CREATE = new MethodMatcher("com.launchdarkly.sdk.LDContext create(String)");
    private static final MethodMatcher BUILDER = new MethodMatcher("com.launchdarkly.sdk.LDContext builder(String)");
    private static final MethodMatcher SET = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder set(String, ..)");
    private static final MethodMatcher NAME = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder name(String)");
    private static final MethodMatcher BUILD = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder build()");

    String displayName = "Migrate LaunchDarkly `LDContext` to OpenFeature `MutableContext`";

    String description = "Convert `LDContext.create(...)` and `LDContext.builder(...)` construction to OpenFeature's " +
            "`MutableContext`, mapping `name(...)` and `set(...)` attributes to `add(...)` and dropping the terminal " +
            "`build()` call. Targeting `kind`, multi-context and private attributes are left untouched for manual review.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.launchdarkly.sdk.LDContext", null), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (CREATE.matches(m) || BUILDER.matches(m)) {
                    maybeAddImport("dev.openfeature.sdk.MutableContext");
                    doAfterVisit(new ChangeType("com.launchdarkly.sdk.LDContext", "dev.openfeature.sdk.EvaluationContext", null).getVisitor());
                    doAfterVisit(new ChangeType("com.launchdarkly.sdk.ContextBuilder", "dev.openfeature.sdk.MutableContext", null).getVisitor());
                    J applied = JavaTemplate.builder("new MutableContext(#{any(String)})")
                            .imports("dev.openfeature.sdk.MutableContext")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                    return applied.withPrefix(m.getPrefix());
                }
                if (NAME.matches(m)) {
                    J.Literal nameLiteral = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "name", "\"name\"", null, JavaType.Primitive.String);
                    Expression value = m.getArguments().get(0).withPrefix(Space.SINGLE_SPACE);
                    return m.withName(m.getName().withSimpleName("add")).withArguments(Arrays.asList(nameLiteral, value));
                }
                if (SET.matches(m)) {
                    return m.withName(m.getName().withSimpleName("add"));
                }
                if (BUILD.matches(m) && m.getSelect() != null) {
                    // Drop the terminal `build()`; OpenFeature's MutableContext is itself the EvaluationContext.
                    // The build() node carries the whole expression's prefix, so transfer it to the new tail.
                    return m.getSelect().withPrefix(m.getPrefix());
                }
                return m;
            }
        });
    }
}
