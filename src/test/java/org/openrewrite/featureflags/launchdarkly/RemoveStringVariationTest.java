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
package org.openrewrite.featureflags.launchdarkly;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveStringVariationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveStringVariation("flag-key-123abc", "topic-456"))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6"));
    }

    @Test
    @DocumentExample
    void replaceStringVariation() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = null;
                      String topic = client.stringVariation("flag-key-123abc", context, "topic-123");
                      System.out.println("Publishing to topic: " + topic);
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      String topic = "topic-456";
                      System.out.println("Publishing to topic: " + topic);
                  }
              }
              """
          )
        );
    }
}
