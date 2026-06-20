/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

@EqualsAndHashCode(callSuper = false)
@Value
public class MarkIncompatibleEvaluationDetailAccessors extends Recipe {

    private static final String SENTINEL = "OpenFeature migration:";

    private static final MethodMatcher GET_VARIATION_INDEX = new MethodMatcher("com.launchdarkly.sdk.EvaluationDetail getVariationIndex()");
    private static final MethodMatcher IS_DEFAULT_VALUE = new MethodMatcher("com.launchdarkly.sdk.EvaluationDetail isDefaultValue()");
    private static final MethodMatcher GET_REASON = new MethodMatcher("com.launchdarkly.sdk.EvaluationDetail getReason()");

    String displayName = "Mark incompatible LaunchDarkly `EvaluationDetail` accessors";

    String description = "OpenFeature's `FlagEvaluationDetails` does not offer a direct replacement for every " +
            "`EvaluationDetail` accessor. Add a `TODO` comment on `getVariationIndex()`, `isDefaultValue()` and " +
            "`getReason()` calls so they are migrated by hand, since `getVariationIndex()` and `isDefaultValue()` have " +
            "no equivalent and `getReason()` returns a `String` rather than an `EvaluationReason`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.launchdarkly.sdk.EvaluationDetail", null), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (GET_VARIATION_INDEX.matches(m)) {
                    return mark(m, "`getVariationIndex()` has no OpenFeature equivalent; `FlagEvaluationDetails` exposes `getVariant()` (a String) instead");
                }
                if (IS_DEFAULT_VALUE.matches(m)) {
                    return mark(m, "`isDefaultValue()` has no OpenFeature equivalent; inspect `getReason()` / `getErrorCode()` instead");
                }
                if (GET_REASON.matches(m)) {
                    return mark(m, "`FlagEvaluationDetails.getReason()` returns a String, not an `EvaluationReason`");
                }
                return m;
            }

            private J.MethodInvocation mark(J.MethodInvocation m, String message) {
                Space prefix = m.getPrefix();
                if (prefix.getComments().stream().anyMatch(c -> c instanceof TextComment && ((TextComment) c).getText().contains(SENTINEL))) {
                    return m;
                }
                Comment comment = new TextComment(true, " TODO " + SENTINEL + " " + message + " ", " ", Markers.EMPTY);
                return m.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), comment)));
            }
        });
    }
}
