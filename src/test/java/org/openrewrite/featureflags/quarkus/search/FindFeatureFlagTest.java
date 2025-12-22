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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindFeatureFlagTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("quarkus-flags"));
    }

    @DocumentExample
    @Test
    void findFeatureFlag() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(null)),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean flagValue = flags.isEnabled("flag-key-123abc");
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean flagValue = /*~~>*/flags.isEnabled("flag-key-123abc");
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void findFeatureFlagByName() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag("flag-key-123abc")),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean flagValue = flags.isEnabled("flag-key-123abc");
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = flags.isEnabled("flag-key-789def");
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean flagValue = /*~~>*/flags.isEnabled("flag-key-123abc");
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = flags.isEnabled("flag-key-789def");
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void findFlagByNameUsingVariable() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag("flag-key-123abc")),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a(Flags flags) {
                      boolean flagValue = flags.isEnabled(FEATURE_FLAG);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = flags.isEnabled(FEATURE2_FLAG);
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a(Flags flags) {
                      boolean flagValue = /*~~>*/flags.isEnabled(FEATURE_FLAG);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = flags.isEnabled(FEATURE2_FLAG);
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void findGetStringFlag() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(null)),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      String environment = flags.getString("env-flag");
                      System.out.println(environment);
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      String environment = /*~~>*/flags.getString("env-flag");
                      System.out.println(environment);
                  }
              }
              """
          )
        );
    }

    @Test
    void findGetIntFlag() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(null)),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      int maxRetries = flags.getInt("max-retries");
                      System.out.println(maxRetries);
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      int maxRetries = /*~~>*/flags.getInt("max-retries");
                      System.out.println(maxRetries);
                  }
              }
              """
          )
        );
    }

    @Test
    void findAllFlagTypes() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(null)),
          //language=java
          java(
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean enabled = flags.isEnabled("feature-enabled");
                      String env = flags.getString("environment");
                      int retries = flags.getInt("max-retries");
                  }
              }
              """,
            """
              import io.quarkiverse.flags.Flags;

              class Test {
                  public void a(Flags flags) {
                      boolean enabled = /*~~>*/flags.isEnabled("feature-enabled");
                      String env = /*~~>*/flags.getString("environment");
                      int retries = /*~~>*/flags.getInt("max-retries");
                  }
              }
              """
          )
        );
    }
}
