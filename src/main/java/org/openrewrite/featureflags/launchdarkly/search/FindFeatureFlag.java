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
package org.openrewrite.featureflags.launchdarkly.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.analysis.InvocationMatcher;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.dataflow.DataFlowNode;
import org.openrewrite.analysis.dataflow.DataFlowSpec;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindFeatureFlag extends Recipe {
    @Option(displayName = "Flag type",
            description = "The feature flag's type.",
            example = "Bool",
            valid = {"Bool", "Double", "Int", "JsonValue", "Migration", "String"},
            required = false)
    @Nullable
    FeatureFlagType flagType;

    @Option(displayName = "Feature key",
            description = "The unique key for the feature flag.",
            example = "flag-key-123abc",
            required = false)
    @Nullable
    String featureKey;

    @Override
    public String getDisplayName() {
        return "Find a LaunchDarkly feature flag";
    }

    @Override
    public String getDescription() {
        return "Find a LaunchDarkly feature flag.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher("com.launchdarkly.sdk.server.LDClient *Variation(..)");
        return Preconditions.check(new UsesMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!methodMatcher.matches(m)) {
                    return m;
                }

                if (flagType != null && featureKey != null) {
                    MethodMatcher flagTypeMatcher = flagType.asMethodMatcher();
                    Boolean matchesFeatureKey = getCursor().getMessage("feature.found");
                    if (flagTypeMatcher.matches(m) && matchesFeatureKey != null && matchesFeatureKey) {
                        return SearchResult.found(m);
                    }
                } else if (flagType != null) {
                    MethodMatcher flagTypeMatcher = flagType.asMethodMatcher();
                    if (flagTypeMatcher.matches(m)) {
                        return SearchResult.found(m);
                    }
                } else if (featureKey != null) {
                    Boolean matchesFeatureKey = getCursor().getMessage("feature.found");
                    if (matchesFeatureKey != null && matchesFeatureKey) {
                        return SearchResult.found(m);
                    }
                } else {
                    return SearchResult.found(m);
                }

                return m;
            }

            @Override
            public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                Expression e = super.visitExpression(expression, ctx);
                if (findFeatureKeyFlow() && methodMatcher.matches(getCursor().firstEnclosing(J.MethodInvocation.class))) {
                    getCursor().putMessageOnFirstEnclosing(J.MethodInvocation.class, "feature.found", true);
                }
                return e;
            }

            private boolean findFeatureKeyFlow() {
                if (StringUtils.isBlank(featureKey)) {
                    return false;
                }

                InvocationMatcher matcher = InvocationMatcher.fromMethodMatcher(methodMatcher);
                return Dataflow.startingAt(getCursor())
                        .findSinks(new DataFlowSpec() {
                            @Override
                            public boolean isSource(DataFlowNode srcNode) {
                                return ConstantFold
                                        .findConstantLiteralValue(srcNode, String.class)
                                        .map(featureKey::equals)
                                        .orSome(false);
                            }

                            @Override
                            public boolean isSink(DataFlowNode sinkNode) {
                                return matcher.advanced().isFirstParameter(sinkNode.getCursor());
                            }
                        }).isSome();
            }
        });
    }

    public enum FeatureFlagType {
        Bool("boolVariation"),
        Double("doubleVariation"),
        Int("intVariation"),
        JsonValue("jsonValueVariation"),
        Migration("migrationVariation"),
        String("stringVariation");

        private final String methodName;

        FeatureFlagType(String methodName) {
            this.methodName = methodName;
        }

        public MethodMatcher asMethodMatcher() {
            return new MethodMatcher("com.launchdarkly.sdk.server.LDClient " + methodName + "(..)");
        }
    }
}
