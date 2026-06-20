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

class MigrateLDClientLifecycleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLDClientLifecycle())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void close() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.server.LDClient;

              class A {
                  void shutdown(LDClient client) {
                      client.close();
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.server.LDClient;
              import dev.openfeature.sdk.OpenFeatureAPI;

              class A {
                  void shutdown(LDClient client) {
                      OpenFeatureAPI.getInstance().shutdown();
                  }
              }
              """
          )
        );
    }

    @Test
    void isInitialized() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.server.LDClient;

              class A {
                  boolean ready(LDClient client) {
                      return client.isInitialized();
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.server.LDClient;
              import dev.openfeature.sdk.OpenFeatureAPI;
              import dev.openfeature.sdk.ProviderState;

              class A {
                  boolean ready(LDClient client) {
                      return OpenFeatureAPI.getInstance().getClient().getProviderState() == ProviderState.READY;
                  }
              }
              """
          )
        );
    }
}
