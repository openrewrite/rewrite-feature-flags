/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.*;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.RemoveUnusedLocalVariables;
import org.openrewrite.staticanalysis.RemoveUnusedPrivateFields;
import org.openrewrite.staticanalysis.SimplifyConstantIfBranchExecution;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveBooleanFlag extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove a boolean feature flag for feature key";
    }

    @Override
    public String getDescription() {
        return "Replace method invocations for feature key with value, and simplify constant if branch execution.";
    }

    @Option(displayName = "Method pattern",
            description = "A method pattern to match against. If not specified, will match `LDClient` `boolVariation`. " +
                          "The first argument must be the feature key as `String`.",
            example = "dev.openfeature.sdk.Client getBooleanValue(String, Boolean)")
    String methodPattern;

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "true")
    Boolean replacementValue;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, true);
        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(mi) && isFeatureKey(mi.getArguments().get(0))) {
                    doAfterVisit(new SimplifyConstantIfBranchExecution().getVisitor());
                    doAfterVisit(new RemoveUnusedLocalVariables(null).getVisitor());
                    doAfterVisit(new RemoveUnusedPrivateFields().getVisitor());
                    return new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, replacementValue, String.valueOf(replacementValue), null, JavaType.Primitive.Boolean);
                }
                return mi;
            }

            private boolean isFeatureKey(Expression firstArgument) {
                return CursorUtil.findCursorForTree(getCursor(), firstArgument)
                               .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class))
                               .map(featureKey::equals)
                               .orSome(false);
            }
        };
        return Preconditions.check(new UsesMethod<>(methodMatcher), visitor);
    }
}
