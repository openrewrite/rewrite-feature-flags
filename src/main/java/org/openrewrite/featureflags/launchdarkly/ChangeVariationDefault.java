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
import org.openrewrite.*;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeVariationDefault extends Recipe {
    String displayName = "Change the default value for feature key";

    String description = "Change the default value for `Variation` invocations for feature key.";

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Default value",
            description = "The default value to use in feature flag invocations.",
            example = "true")
    String defaultValue;

    private static final MethodMatcher BOOL_VARIATION_MATCHER = new MethodMatcher("com.launchdarkly.sdk.server.LDClient boolVariation(String, com.launchdarkly.sdk.*, boolean)", true);
    private static final MethodMatcher STRING_VARIATION_MATCHER = new MethodMatcher("com.launchdarkly.sdk.server.LDClient stringVariation(String, com.launchdarkly.sdk.*, String)", true);
    private static final MethodMatcher INT_VARIATION_MATCHER = new MethodMatcher("com.launchdarkly.sdk.server.LDClient intVariation(String, com.launchdarkly.sdk.*, int)", true);
    private static final MethodMatcher DOUBLE_VARIATION_MATCHER = new MethodMatcher("com.launchdarkly.sdk.server.LDClient doubleVariation(String, com.launchdarkly.sdk.*, double)", true);
    // Not yet handling JSON_VARIATION_MATCHER, as that takes a `com.launchdarkly.sdk.LDValue` argument

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                Expression firstArgument = mi.getArguments().get(0);
                Expression lastArgument = mi.getArguments().get(mi.getArguments().size() - 1);
                if (BOOL_VARIATION_MATCHER.matches(mi) && isFeatureKey(firstArgument)) {
                    return changeValue(mi, lastArgument, defaultValue);
                }
                if (STRING_VARIATION_MATCHER.matches(mi) && isFeatureKey(firstArgument)) {
                    return changeValue(mi, lastArgument, "\"" + defaultValue + "\"");
                }
                if (INT_VARIATION_MATCHER.matches(mi) && isFeatureKey(firstArgument)) {
                    return changeValue(mi, lastArgument, defaultValue);
                }
                if (DOUBLE_VARIATION_MATCHER.matches(mi) && isFeatureKey(firstArgument)) {
                    return changeValue(mi, lastArgument, defaultValue);
                }
                return mi;
            }

            private Boolean isFeatureKey(Expression firstArgument) {
                return CursorUtil.findCursorForTree(getCursor(), firstArgument)
                        .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class))
                        .map(featureKey::equals)
                        .orSome(false);
            }

            private J.MethodInvocation changeValue(J.MethodInvocation mi, Expression existingValue, String newValueSource) {
                if (newValueSource.equals(existingValue.withPrefix(Space.EMPTY).printTrimmed(getCursor()))) {
                    return mi; // No change needed
                }
                // the template gives the new value the tree the parser makes of its text, and the argument keeps its formatting
                J.MethodInvocation changed = JavaTemplate.apply(newValueSource, updateCursor(mi), existingValue.getCoordinates().replace());
                return changed.withArguments(ListUtils.mapLast(changed.getArguments(), a -> a.withPrefix(existingValue.getPrefix())));
            }
        };
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(BOOL_VARIATION_MATCHER),
                        new UsesMethod<>(STRING_VARIATION_MATCHER),
                        new UsesMethod<>(INT_VARIATION_MATCHER),
                        new UsesMethod<>(DOUBLE_VARIATION_MATCHER)),
                visitor);
    }
}
