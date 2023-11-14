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
package org.openrewrite.launchdarkly.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindFeatureFlagTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("launchdarkly-java-server-sdk"));
    }

    @Test
    @DocumentExample
    void findFeatureFlag() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(null, null)),
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = /*~~>*/client.boolVariation("flag-key-123abc", user, false);
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
    void findFeatureFlagByType() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(FindFeatureFlag.FeatureFlagType.Bool, null)),
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      String flagValue2 = client.stringVariation("flag-key-789def", user, "on");
                      if ("on".equals(flagValue2)) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = /*~~>*/client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      String flagValue2 = client.stringVariation("flag-key-789def", user, "on");
                      if ("on".equals(flagValue2)) {
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
          spec -> spec.recipe(new FindFeatureFlag(null, "flag-key-123abc")),
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.boolVariation("flag-key-789def", user, false);
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = /*~~>*/client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.boolVariation("flag-key-789def", user, false);
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
    void findFeatureFlagByTypeAndName() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag(FindFeatureFlag.FeatureFlagType.Bool, "flag-key-123abc")),
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      String flagValue2 = client.stringVariation("flag-key-123abc", user, "on");
                      if ("on".equals(flagValue2)) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = /*~~>*/client.boolVariation("flag-key-123abc", user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      String flagValue2 = client.stringVariation("flag-key-123abc", user, "on");
                      if ("on".equals(flagValue2)) {
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
          spec -> spec.recipe(new FindFeatureFlag(null, "flag-key-123abc")),
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = client.boolVariation(FEATURE_FLAG, user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.boolVariation(FEATURE2_FLAG, user, false);
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.server.LDClient;
              
              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a() {
                      LDClient client = new LDClient("sdk-key");
                      LDUser user = new LDUser.Builder("user-key")
                              .name("user")
                              .build();
                      boolean flagValue = /*~~>*/client.boolVariation(FEATURE_FLAG, user, false);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.boolVariation(FEATURE2_FLAG, user, false);
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
}
