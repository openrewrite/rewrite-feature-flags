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
package org.openrewrite.featureflags.unleash.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindFeatureFlag extends Recipe {

    @Option(displayName = "Feature key",
            description = "The unique key for the feature flag.",
            example = "flag-key-123abc",
            required = false)
    @Nullable
    String featureKey;

    @Override
    public String getDisplayName() {
        return "Find an Unleash feature flag";
    }

    @Override
    public String getDescription() {
        return "Find an Unleash feature flag.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Collections.singletonList(new org.openrewrite.featureflags.search.FindFeatureFlag(
                "io.getunleash.Unleash isEnabled(String, ..)", featureKey));
    }
}
