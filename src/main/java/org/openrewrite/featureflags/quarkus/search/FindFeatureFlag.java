/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.featureflags.quarkus.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindFeatureFlag extends Recipe {

    @Option(displayName = "Feature key",
            description = "The unique key for the feature flag.",
            example = "flag-key-123abc",
            required = false)
    @Nullable
    String featureKey;

    String displayName = "Find a Quarkus feature flag";

    String description = "Find a Quarkus feature flag.";

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new org.openrewrite.featureflags.search.FindFeatureFlag(
                        "io.quarkiverse.flags.Flags find(String)", featureKey),
                new org.openrewrite.featureflags.search.FindFeatureFlag(
                        "io.quarkiverse.flags.Flags findAndAwait(String)", featureKey),
                new org.openrewrite.featureflags.search.FindFeatureFlag(
                        "io.quarkiverse.flags.Flags isEnabled(String)", featureKey),
                new org.openrewrite.featureflags.search.FindFeatureFlag(
                        "io.quarkiverse.flags.Flags getString(String)", featureKey),
                new org.openrewrite.featureflags.search.FindFeatureFlag(
                        "io.quarkiverse.flags.Flags getInt(String)", featureKey));
    }
}
