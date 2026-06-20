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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateLaunchDarklyToOpenFeatureTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.featureflags.launchdarkly.MigrateLaunchDarklyToOpenFeature")
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void migrateBooleanEvaluationWithContext() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;

              class FeatureFlags {
                  boolean newCheckout(LDClient client, String userKey) {
                      LDContext context = LDContext.builder(userKey)
                              .name("Sandy")
                              .set("email", "sandy@example.com")
                              .build();
                      return client.boolVariation("new-checkout", context, false);
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.EvaluationContext;
              import dev.openfeature.sdk.MutableContext;

              class FeatureFlags {
                  boolean newCheckout(Client client, String userKey) {
                      EvaluationContext context = new MutableContext(userKey)
                              .add("name", "Sandy")
                              .add("email", "sandy@example.com");
                      return client.getBooleanValue("new-checkout", false, context);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateBooleanDetailEvaluation() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.EvaluationDetail;
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;

              class FeatureFlags {
                  boolean detail(LDClient client, LDContext context) {
                      EvaluationDetail<Boolean> detail = client.boolVariationDetail("new-checkout", context, false);
                      return detail.getValue();
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.EvaluationContext;
              import dev.openfeature.sdk.FlagEvaluationDetails;

              class FeatureFlags {
                  boolean detail(Client client, EvaluationContext context) {
                      FlagEvaluationDetails<Boolean> detail = client.getBooleanDetails("new-checkout", false, context);
                      return detail.getValue();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateJsonValueEvaluation() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.LDValue;
              import com.launchdarkly.sdk.server.LDClient;

              class FeatureFlags {
                  LDValue config(LDClient client, LDContext context) {
                      return client.jsonValueVariation("config", context, LDValue.of("{}"));
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.EvaluationContext;
              import dev.openfeature.sdk.Value;

              class FeatureFlags {
                  Value config(Client client, EvaluationContext context) {
                      return client.getObjectValue("config", new Value("{}"), context);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateStringIntDoubleEvaluations() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;

              class FeatureFlags {
                  void evaluate(LDClient client, LDContext context) {
                      String s = client.stringVariation("s-flag", context, "default");
                      int i = client.intVariation("i-flag", context, 1);
                      double d = client.doubleVariation("d-flag", context, 2.0);
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.EvaluationContext;

              class FeatureFlags {
                  void evaluate(Client client, EvaluationContext context) {
                      String s = client.getStringValue("s-flag", "default", context);
                      int i = client.getIntegerValue("i-flag", 1, context);
                      double d = client.getDoubleValue("d-flag", 2.0, context);
                  }
              }
              """
          )
        );
    }
}
