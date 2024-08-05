/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.featureflags.unleash;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.featureflags.RemoveBooleanFlag;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveIsEnabled extends Recipe {

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "true")
    Boolean replacementValue;

    @Override
    public String getDisplayName() {
        return "Remove Unleash's `isEnabled` for feature key";
    }

    @Override
    public String getDescription() {
        return "Replace `isEnabled()` invocations for `featureKey` with `replacementValue`, and simplify constant if branch execution.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Collections.singletonList(new RemoveBooleanFlag(
                "io.getunleash.Unleash isEnabled(String)",
                featureKey, replacementValue));
    }
}
