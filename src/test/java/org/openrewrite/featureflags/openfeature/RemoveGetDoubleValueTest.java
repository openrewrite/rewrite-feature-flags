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
package org.openrewrite.featureflags.openfeature;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveGetDoubleValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveGetDoubleValue("flag-key-123abc", 3.14))
          .parser(JavaParser.fromJavaVersion().classpath("sdk"));
    }

    @DocumentExample
    @Test
    void removeGetDoubleValue() {
        rewriteRun(
          //language=java
          java(
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      Double multiplier = client.getDoubleValue("flag-key-123abc", 1.5);
                      System.out.println("Multiplier: " + multiplier);
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      System.out.println("Multiplier: " + 3.14);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeInlineDoubleValue() {
        rewriteRun(
          //language=java
          java(
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      System.out.println("Rate: " + client.getDoubleValue("flag-key-123abc", 0.5));
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      System.out.println("Rate: " + 3.14);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeWithPrimitiveType() {
        rewriteRun(
          //language=java
          java(
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      double threshold = client.getDoubleValue("flag-key-123abc", 1.0);
                      System.out.println("Threshold: " + threshold);
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;

              class Foo {
                  void bar(Client client) {
                      System.out.println("Threshold: " + 3.14);
                  }
              }
              """
          )
        );
    }
}
