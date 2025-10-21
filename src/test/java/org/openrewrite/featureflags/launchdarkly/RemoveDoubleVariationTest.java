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

class RemoveDoubleVariationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDoubleVariation("flag-key-123abc", 3.14))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void replaceDoubleVariation() {
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
                      double multiplier = client.doubleVariation("flag-key-123abc", context, 1.5);
                      System.out.println("Multiplier: " + multiplier);
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Multiplier: " + 3.14);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceDoubleVariationInline() {
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
                      System.out.println("Rate: " + client.doubleVariation("flag-key-123abc", context, 0.5));
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Rate: " + 3.14);
                  }
              }
              """
          )
        );
    }
}
