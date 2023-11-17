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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveBoolVariationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveBoolVariation("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6"));
    }

    @Test
    @DocumentExample
    void enablePermanently() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      // Unused local variables are not yet cleaned up
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                      else {
                        // The code to run if the feature is off
                          System.out.println("Feature is off");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  void bar() {
                      // Unused local variables are not yet cleaned up
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }

    @Test
    void enablePermanentlyNegated() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      if (!client.boolVariation("flag-key-123abc", context, false)) {
                        // The code to run if the feature is off
                          System.out.println("Feature is off");
                      }
                      else {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }

    @Test
    void disablePermanently() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBoolVariation("flag-key-123abc", false)),
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                      else {
                        // The code to run if the feature is off
                          System.out.println("Feature is off");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      // The code to run if the feature is off
                      System.out.println("Feature is off");
                  }
              }
              """
          )
        );
    }

    @Test
    void enablePermanentlyWithParameters() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  // Unused parameters are not yet cleaned up
                  void bar(LDClient client, LDContext context) {
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  // Unused parameters are not yet cleaned up
                  void bar(LDClient client, LDContext context) {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyOnlyAffectsSourceFileWithFeatureFlag() {
        // language=java
        rewriteRun(
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  void bar(LDClient client, LDContext context) {
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  void bar(LDClient client, LDContext context) {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          ),
          java("""
            class Bar {
                void bar() {
                    if (true) {
                        // conditional retained; simplify only applies to the file with the feature flag check
                    }
                }
            }
            """)
        );
    }
}
