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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.featureflags.RemoveIntegerFlag;

import java.util.List;

import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveIntVariation extends Recipe {

    String displayName = "Remove LaunchDarkly's `intVariation` for feature key";

    String description = "Replace `intVariation` invocations for feature key with value, and simplify constant if branch execution.";

    @Option(displayName = "Feature flag key",
            description = "The key of the feature flag to remove.",
            example = "flag-key-123abc")
    String featureKey;

    @Option(displayName = "Replacement value",
            description = "The value to replace the feature flag check with.",
            example = "42")
    Integer replacementValue;

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new RemoveIntegerFlag(
                "com.launchdarkly.sdk.server.LDClient intVariation(String, com.launchdarkly.sdk.*, int)",
                featureKey, replacementValue));
    }
}
