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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeVariationDefault extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change the default value for feature key";
    }

    @Override
    public String getDescription() {
        return "Change the default value for `Variation` invocations for feature key.";
    }

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    @NonNull
    String featureKey;

    @Option(displayName = "Default value",
            description = "The default value to use in feature flag invocations.",
            example = "true")
    @NonNull
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
                if (BOOL_VARIATION_MATCHER.matches(mi) && J.Literal.isLiteralValue(firstArgument, featureKey)) {
                    return changeValue(mi, lastArgument, new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, defaultValue, defaultValue, null, JavaType.Primitive.Boolean));
                }
                if (STRING_VARIATION_MATCHER.matches(mi) && J.Literal.isLiteralValue(firstArgument, featureKey)) {
                    return changeValue(mi, lastArgument, new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, defaultValue, "\"" + defaultValue + "\"", null, JavaType.Primitive.String));
                }
                if (INT_VARIATION_MATCHER.matches(mi) && J.Literal.isLiteralValue(firstArgument, featureKey)) {
                    return changeValue(mi, lastArgument, new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, defaultValue, defaultValue, null, JavaType.Primitive.Int));
                }
                if (DOUBLE_VARIATION_MATCHER.matches(mi) && J.Literal.isLiteralValue(firstArgument, featureKey)) {
                    return changeValue(mi, lastArgument, new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, defaultValue, defaultValue, null, JavaType.Primitive.Double));
                }
                return mi;
            }

            private J.MethodInvocation changeValue(J.MethodInvocation mi, Expression existingValue, J.Literal newValue) {
                if (existingValue instanceof J.Literal && newValue.getValueSource().equals(((J.Literal) existingValue).getValueSource())) {
                    return mi; // No change needed
                }
                return mi.withArguments(ListUtils.mapLast(mi.getArguments(), a -> newValue.withPrefix(a.getPrefix())));
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
