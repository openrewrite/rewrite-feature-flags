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

class MigrateLDClientToOpenFeatureTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLDClientToOpenFeature())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void localVariableGeneratesProviderBootstrap() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.server.LDClient;

              class A {
                  void init() {
                      LDClient client = new LDClient("sdk-key-123abc");
                  }
              }
              """,
            """
              import com.launchdarkly.openfeature.serverprovider.Provider;
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.OpenFeatureAPI;

              class A {
                  void init() {
                      OpenFeatureAPI.getInstance().setProviderAndWait(new Provider("sdk-key-123abc"));
                      Client client = OpenFeatureAPI.getInstance().getClient();
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldKeepsConfigurationInComment() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.server.LDClient;

              class A {
                  LDClient client = new LDClient("sdk-key-123abc");
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              import dev.openfeature.sdk.OpenFeatureAPI;

              class A {
                  Client client = /* TODO Configure the OpenFeature provider, e.g. OpenFeatureAPI.getInstance().setProviderAndWait(new Provider("sdk-key-123abc")) */ OpenFeatureAPI.getInstance().getClient();
              }
              """
          )
        );
    }
}
