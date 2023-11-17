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
import org.openrewrite.internal.lang.NonNull;
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

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveBoolVariation extends Recipe {
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
    @NonNull
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "true")
    @NonNull
    Boolean replacementValue;

    private static final MethodMatcher methodMatcher = new MethodMatcher("com.launchdarkly.sdk.server.LDClient boolVariation(String, com.launchdarkly.sdk.*, boolean)", true);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (methodMatcher.matches(mi) && J.Literal.isLiteralValue(mi.getArguments().get(0), featureKey)) {
                    doAfterVisit(new SimplifyConstantIfBranchExecution().getVisitor());
                    doAfterVisit(new RemoveUnusedLocalVariables(null).getVisitor());
                    doAfterVisit(new RemoveUnusedPrivateFields().getVisitor());
                    return new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, replacementValue, String.valueOf(replacementValue), null, JavaType.Primitive.Boolean);
                }
                return mi;
            }
        };
        return Preconditions.check(new UsesMethod<>(methodMatcher), visitor);
    }
}
