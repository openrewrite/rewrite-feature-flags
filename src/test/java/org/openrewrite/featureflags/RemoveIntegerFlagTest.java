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

class RemoveIntegerFlagTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveIntegerFlag("com.acme.bank.InHouseFF getIntegerFeatureFlagValue(String, Integer)", "flag-key-123abc", 42))
          // language=java
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.acme.bank;
              public class InHouseFF {
                  public Integer getIntegerFeatureFlagValue(String key, Integer fallback) {
                      return fallback;
                  }
              }
              """
          ));
    }

    @DocumentExample
    @Test
    void removeIntegerFeatureFlag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveIntegerFlag("com.acme.bank.InHouseFF getIntegerFeatureFlagValue(String, Integer)", "flag-key-123abc", 42)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      Integer maxRetries = inHouseFF.getIntegerFeatureFlagValue("flag-key-123abc", 3);
                      System.out.println("Max retries: " + maxRetries);
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Max retries: " + 42);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeVariableDeclarationWithIntegerFlag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveIntegerFlag("com.acme.bank.InHouseFF getIntegerFeatureFlagValue(String, Integer)", "flag-key-123abc", 100)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      int threshold = inHouseFF.getIntegerFeatureFlagValue("flag-key-123abc", 50);
                      System.out.println("Threshold: " + threshold);
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Threshold: " + 100);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeInlineIntegerUsage() {
        rewriteRun(
          spec -> spec.recipe(new RemoveIntegerFlag("com.acme.bank.InHouseFF getIntegerFeatureFlagValue(String, Integer)", "flag-key-123abc", 5)),
          // language=java
          java(
            """
              import com.acme.bank.InHouseFF;
              class Foo {
                  private InHouseFF inHouseFF = new InHouseFF();
                  void bar() {
                      System.out.println("Max retries: " + inHouseFF.getIntegerFeatureFlagValue("flag-key-123abc", 10));
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      System.out.println("Max retries: " + 5);
                  }
              }
              """
          )
        );
    }
}
