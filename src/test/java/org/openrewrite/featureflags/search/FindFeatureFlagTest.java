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
package org.openrewrite.featureflags.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindFeatureFlagTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        //language=java
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.acme;
              public class FeatureFlag {
                  public boolean isEnabled(String key) {
                      return false;
                  }
              }
              """
          )
        );
    }

    @Test
    @DocumentExample
    void findFeatureFlagWithAnyKey() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag("com.acme.FeatureFlag isEnabled(String)", null)),
          //language=java
          java(
            """
              import com.acme.FeatureFlag;

              class Test {
                  public void a() {
                      FeatureFlag client = new FeatureFlag();
                      boolean flagValue = client.isEnabled("flag-key-123abc");
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.acme.FeatureFlag;

              class Test {
                  public void a() {
                      FeatureFlag client = new FeatureFlag();
                      boolean flagValue = /*~~>*/client.isEnabled("flag-key-123abc");
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
    void findFlagWithSpecificKeyThroughFlow() {
        rewriteRun(
          spec -> spec.recipe(new FindFeatureFlag("com.acme.FeatureFlag isEnabled(String)", "flag-key-123abc")),
          //language=java
          java(
            """
              import com.acme.FeatureFlag;

              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a() {
                      FeatureFlag client = new FeatureFlag();
                      boolean flagValue = client.isEnabled(FEATURE_FLAG);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.isEnabled(FEATURE2_FLAG);
                      if (flagValue2) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                  }
              }
              """,
            """
              import com.acme.FeatureFlag;

              class Test {
                  private static final String FEATURE_FLAG = "flag-key-123abc";
                  private static final String FEATURE2_FLAG = "flag-key-789def";
                  public void a() {
                      FeatureFlag client = new FeatureFlag();
                      boolean flagValue = /*~~>*/client.isEnabled(FEATURE_FLAG);
                      if (flagValue) {
                          // Application code to show the feature
                      } else {
                          // The code to run if the feature is off
                      }
                      boolean flagValue2 = client.isEnabled(FEATURE2_FLAG);
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
