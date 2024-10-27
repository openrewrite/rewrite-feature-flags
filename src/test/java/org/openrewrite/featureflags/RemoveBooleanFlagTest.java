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
package org.openrewrite.featureflags;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class RemoveBooleanFlagTest implements RewriteTest {

    @Test
    @DocumentExample
    void customMethodPatternForWrapper() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBooleanFlag("com.acme.bank.CustomLaunchDarklyWrapper featureFlagEnabled(String, boolean)", "flag-key-123abc", true)),
          // language=java
          java(
            """
              package com.acme.bank;
              
              public class CustomLaunchDarklyWrapper {
                  public boolean featureFlagEnabled(String key, boolean fallback) {
                      return fallback;
                  }
              }
              """,
            SourceSpec::skip
          ),
          // language=java
          java(
            """
              import com.acme.bank.CustomLaunchDarklyWrapper;
              class Foo {
                  private CustomLaunchDarklyWrapper wrapper = new CustomLaunchDarklyWrapper();
                  void bar() {
                      if (wrapper.featureFlagEnabled("flag-key-123abc", false)) {
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
    @Issue("https://github.com/openrewrite/rewrite-feature-flags/issues/23")
    void customMethodPatternNoConstants() {
        // language=java
        rewriteRun(
          spec -> spec.recipe(new RemoveBooleanFlag("com.osd.util.ToggleChecker isToggleEnabled(String, boolean)", "FEATURE_TOGGLE1", true)),
          java(
            """
              package com.osd.util;
              import java.util.Map;
              import java.util.HashMap;
              
              public class ToggleChecker {
                  public boolean isToggleEnabled(String toggleName, boolean fallback) {
                      Map<String,Boolean> toggleMap = new HashMap<>();
                      toggleMap.put("FEATURE_TOGGLE1", true);
                      toggleMap.put("FEATURE_TOGGLE2", true);
                      toggleMap.put("FEATURE_TOGGLE3", false);
                      return toggleMap.containsKey(toggleName);
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.osd.util.ToggleChecker;
              class Foo {
                  private ToggleChecker checker = new ToggleChecker();
                  void bar() {
                      if (checker.isToggleEnabled("FEATURE_TOGGLE1", false)) {
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
    void removeWhenFeatureFlagIsAConstant() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBooleanFlag("com.acme.bank.CustomLaunchDarklyWrapper featureFlagEnabled(String, boolean)", "flag-key-123abc", true)),
          // language=java
          java(
            """
              package com.acme.bank;
              
              public class CustomLaunchDarklyWrapper {
                  public boolean featureFlagEnabled(String key, boolean fallback) {
                      return fallback;
                  }
              }
              """,
            SourceSpec::skip
          ),
          // language=java
          java(
            """
              import com.acme.bank.CustomLaunchDarklyWrapper;
              class Foo {
                  private static final String FEATURE_TOGGLE = "flag-key-123abc";
              
                  private CustomLaunchDarklyWrapper wrapper = new CustomLaunchDarklyWrapper();
                  void bar() {
                      if (wrapper.featureFlagEnabled(FEATURE_TOGGLE, false)) {
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
    void removeUnnecessaryTernary() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBooleanFlag("com.acme.bank.CustomLaunchDarklyWrapper featureFlagEnabled(String, boolean)", "flag-key-123abc", true)),
          // language=java
          java(
            """
              package com.acme.bank;
              
              public class CustomLaunchDarklyWrapper {
                  public boolean featureFlagEnabled(String key, boolean fallback) {
                      return fallback;
                  }
              }
              """,
            SourceSpec::skip
          ),
          // language=java
          java(
            """
              import com.acme.bank.CustomLaunchDarklyWrapper;
              class Foo {
                  private CustomLaunchDarklyWrapper wrapper = new CustomLaunchDarklyWrapper();
                  String bar() {
                      return wrapper.featureFlagEnabled("flag-key-123abc", false) ? "Feature is on" : "Feature is off";
                  }
              }
              """,
            """
              class Foo {
                  String bar() {
                      return "Feature is on";
                  }
              }
              """
          )
        );
    }
}
