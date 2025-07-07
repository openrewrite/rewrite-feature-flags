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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveBoolVariationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveBoolVariation("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void enablePermanently() {
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
              class Foo {
                  void bar() {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }

    @Test
    void keyInConstant() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;

              class Foo {
                  private static final String FEATURE_FLAG_123ABC = "flag-key-123abc";

                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = null;
                      if (client.boolVariation(FEATURE_FLAG_123ABC, context, false)) {
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
              class Foo {
                  void bar() {
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
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = null;
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
              class Foo {
                  void bar() {
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
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = null;
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
              class Foo {
                  void bar() {
                      // The code to run if the feature is off
                      System.out.println("Feature is off");
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnusedLDContextWithBuilder() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
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
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnusedLDContextWithBuilderContext() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext ldContext = LDContext.create("newValue");
                      LDContext context = LDContext.builderFromContext(ldContext)
                                .anonymous(false)
                                .name("name")
                                .set("email", "email@gmail.com")
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
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      // Application code to show the feature
                      System.out.println("Feature is on");
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
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
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
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
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
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void foo() {
                      LDContext context = null;
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                  }
              }
              """,
            """
              class Foo {
                  void foo() {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          ),
          java(
            """
              class Bar {
                  void bar() {
                      if (true) {
                          // conditional retained; simplify only applies to the file with the feature flag check
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-feature-flags/issues/40")
    @Test
    void localVariablesNotInlined() {
        // language=java
        rewriteRun(
          java(
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.server.LDClient;
              class Foo {
                  private LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = null;
                      // Local variables not yet inlined
                      boolean flagEnabled = client.boolVariation("flag-key-123abc", context, false);
                      if (flagEnabled) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }
              """
          )
        );
    }
}
