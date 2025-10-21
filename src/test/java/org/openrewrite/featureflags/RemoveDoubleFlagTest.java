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
package org.openrewrite.featureflags;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveDoubleFlagTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDoubleFlag("com.acme.bank.InHouseFF getDoubleFeatureFlagValue(String, Double)", "flag-key-123abc", 3.14))
          // language=java
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.acme.bank;
              public class InHouseFF {
                  public Double getDoubleFeatureFlagValue(String key, Double fallback) {
                      return fallback;
                  }
              }
              """
          ));
    }

    @DocumentExample
    @Test
    void removeDoubleFeatureFlag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDoubleFlag("com.acme.bank.InHouseFF getDoubleFeatureFlagValue(String, Double)", "flag-key-123abc", 3.14)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      Double multiplier = inHouseFF.getDoubleFeatureFlagValue("flag-key-123abc", 1.5);
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
    void removeVariableDeclarationWithDoubleFlag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDoubleFlag("com.acme.bank.InHouseFF getDoubleFeatureFlagValue(String, Double)", "flag-key-123abc", 2.5)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      double threshold = inHouseFF.getDoubleFeatureFlagValue("flag-key-123abc", 1.0);
                      System.out.println("Threshold: " + threshold);
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Threshold: " + 2.5);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeInlineDoubleUsage() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDoubleFlag("com.acme.bank.InHouseFF getDoubleFeatureFlagValue(String, Double)", "flag-key-123abc", 0.75)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      System.out.println("Rate: " + inHouseFF.getDoubleFeatureFlagValue("flag-key-123abc", 0.5));
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Rate: " + 0.75);
                  }
              }
              """
          )
        );
    }
}
