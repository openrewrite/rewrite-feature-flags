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
package org.openrewrite.launchdarkly;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.RemoveUnusedLocalVariables;
import org.openrewrite.staticanalysis.RemoveUnusedPrivateFields;
import org.openrewrite.staticanalysis.SimplifyConstantIfBranchExecution;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveBoolVariation extends Recipe {

    private static final String METHOD_PATTERN_BOOLVARIATION = "com.launchdarkly.sdk.server.LDClient boolVariation(String, com.launchdarkly.sdk.*, boolean)";

    @Override
    public String getDisplayName() {
        return "Remove `boolVariation` for feature key";
    }

    @Override
    public String getDescription() {
        return "Replace `boolVariation` invocations for feature key with value, and simplify constant if branch execution.";
    }

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "true")
    Boolean replacementValue;

    @Option(displayName = "Method pattern",
            description = "A method pattern to match against. If not specified, will match `LDClient` `boolVariation`. " +
                          "The first argument must be the feature key as `String`.",
            example = METHOD_PATTERN_BOOLVARIATION,
            required = false)
    @Nullable
    String methodPattern;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String pattern = Optional.ofNullable(methodPattern).filter(StringUtils::isNotEmpty).orElse(METHOD_PATTERN_BOOLVARIATION);
        final MethodMatcher methodMatcher = new MethodMatcher(pattern, true);
        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                boolean isFirstArgumentFeatureKey =
                        CursorUtil
                                .findCursorForTree(getCursor(), mi.getArguments().get(0))
                                .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class))
                                .map(featureKey::equals)
                                .orSome(false);
                if (methodMatcher.matches(mi) && isFirstArgumentFeatureKey) {
                    doAfterVisit(new SimplifyConstantIfBranchExecution().getVisitor());
                    doAfterVisit(new RemoveUnusedLocalVariables(null).getVisitor());
                    doAfterVisit(new RemoveUnusedPrivateFields().getVisitor());
                    return new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, replacementValue, String.valueOf(replacementValue), null, JavaType.Primitive.Boolean);
                }
                return mi;
            }
        };
        return Preconditions.check(new UsesMethod<>(methodMatcher), visitor);
    }
}
