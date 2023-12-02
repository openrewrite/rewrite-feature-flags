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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeVariationDefaultTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeVariationDefault("flag-key-123abc", "true"))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6"));
    }

    @Nested
    class BooleanVariation {
        @Test
        @DocumentExample
        void changeDefaultValueToTrue() {
            rewriteRun(
              // language=java
              java(
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.boolVariation("flag-key-123abc", context, false)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """,
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.boolVariation("flag-key-123abc", context, true)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void changeDefaultValueToTrueEvenIfVariable() {
            rewriteRun(
              // language=java
              java(
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          boolean defaultValue = false; // Not cleaned up
                          if (client.boolVariation("flag-key-123abc", context, defaultValue)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """,
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          boolean defaultValue = false; // Not cleaned up
                          if (client.boolVariation("flag-key-123abc", context, true)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class StringVariation {
        @Test
        @DocumentExample
        void changeDefaultValueToTrue() {
            rewriteRun(
              // language=java
              java(
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.stringVariation("flag-key-123abc", context, "foo")) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """,
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.stringVariation("flag-key-123abc", context, "true")) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class DoubleVariation {
        @Test
        @DocumentExample
        void changeDefaultValueToTrue() {
            rewriteRun(
              spec -> spec.recipe(new ChangeVariationDefault("flag-key-123abc", "4.56")),
              // language=java
              java(
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.doubleVariation("flag-key-123abc", context, 1.23)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """,
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.doubleVariation("flag-key-123abc", context, 4.56)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class IntVariation {
        @Test
        @DocumentExample
        void changeDefaultValueToTrue() {
            rewriteRun(
              spec -> spec.recipe(new ChangeVariationDefault("flag-key-123abc", "456")),
              // language=java
              java(
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.intVariation("flag-key-123abc", context, 123)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """,
                """
                  import com.launchdarkly.sdk.LDContext;
                  import com.launchdarkly.sdk.server.LDClient;
                  class Foo {
                      private LDClient client = new LDClient("sdk-key-123abc");
                      void bar(LDContext context) {
                          if (client.intVariation("flag-key-123abc", context, 456)) {
                              System.out.println("Feature is on");
                          }
                      }
                  }
                  """
              )
            );
        }
    }
}
