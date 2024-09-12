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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class RemoveStringFlagTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveStringFlag("com.acme.bank.InHouseFF getStringFeatureFlagValue(String, String)", "flag-key-123abc", "topic-456"))
          // language=java
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.acme.bank;
              public class InHouseFF {
                  public String getStringFeatureFlagValue(String key, String fallback) {
                      return fallback;
                  }
              }
              """
          ));
    }

    @DocumentExample
    @Test
    void removeStringFeatureFlag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveStringFlag("com.acme.bank.InHouseFF getStringFeatureFlagValue(String, String)", "flag-key-123abc", "topic-456")),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      String topic = inHouseFF.getStringFeatureFlagValue("flag-key-123abc", "topic-123");
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

    @Disabled("This test is disabled because it is not yet implemented.")
    @Test
    void removeEqualsComparison() {
        rewriteRun(
          spec -> spec.recipe(new RemoveStringFlag("com.acme.bank.InHouseFF getStringFeatureFlagValue(String, String)", "flag-key-123abc", "topic-456")),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      if ("topic-456".equals(inHouseFF.getStringFeatureFlagValue("flag-key-123abc", "topic-123"))) {
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
