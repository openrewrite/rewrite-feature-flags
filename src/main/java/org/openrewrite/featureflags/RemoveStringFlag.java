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
package org.openrewrite.featureflags;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.RemoveUnusedLocalVariables;
import org.openrewrite.staticanalysis.RemoveUnusedPrivateFields;
import org.openrewrite.staticanalysis.RemoveUnusedPrivateMethods;
import org.openrewrite.staticanalysis.SimplifyConstantIfBranchExecution;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveStringFlag extends Recipe {

    String displayName = "Remove a String feature flag for feature key";

    String description = "Replace method invocations for feature key with value, and simplify constant if branch execution.";

    @Option(displayName = "Method pattern",
            description = "A method pattern to match against. The first argument must be the feature key as `String`.",
            example = "dev.openfeature.sdk.Client getBooleanValue(String, Boolean)")
    String methodPattern;

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "topic-456")
    String replacementValue;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, true);
        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                if (multiVariable.getVariables().size() == 1 && matches(multiVariable.getVariables().get(0).getInitializer())) {
                    // Remove the variable declaration and inline any references to the variable with the literal value
                    J.Identifier identifierToReplaceWithLiteral = multiVariable.getVariables().get(0).getName();
                    doAfterVisit(new JavaVisitor<ExecutionContext>() {
                        @Override
                        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                            if (SemanticallyEqual.areEqual(ident , identifierToReplaceWithLiteral)) {
                                return buildLiteral().withPrefix(ident.getPrefix());
                            }
                            return ident;
                        }
                    });
                    cleanUpAfterReplacements();
                    return null;
                }
                return super.visitVariableDeclarations(multiVariable, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (matches(mi)) {
                    cleanUpAfterReplacements();
                    return buildLiteral().withPrefix(mi.getPrefix());
                }
                return mi;
            }

            private boolean matches(@Nullable Expression expression) {
                if (methodMatcher.matches(expression)) {
                    Expression firstArgument = ((J.MethodInvocation) expression).getArguments().get(0);
                    return CursorUtil.findCursorForTree(getCursor(), firstArgument)
                            .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class))
                            .map(featureKey::equals)
                            .orSome(false);
                }
                return false;
            }

            private void cleanUpAfterReplacements() {
                doAfterVisit(new SimplifyConstantIfBranchExecution().getVisitor());
                doAfterVisit(Repeat.repeatUntilStable(new RemoveUnusedLocalVariables(null, null, true).getVisitor(), 3));
                doAfterVisit(new RemoveUnusedPrivateFields().getVisitor());
                doAfterVisit(new RemoveUnusedPrivateMethods().getVisitor());
            }

            private J.Literal buildLiteral() {
                return new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, replacementValue, '"' + replacementValue + '"', null, JavaType.Primitive.String);
            }
        };
        return Preconditions.check(new UsesMethod<>(methodMatcher), visitor);
    }
}
