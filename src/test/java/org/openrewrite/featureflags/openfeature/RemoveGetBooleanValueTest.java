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

class RemoveGetBooleanValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveGetBooleanValue("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion().classpath("sdk"));
    }

    @DocumentExample
    @Test
    void removeGetBooleanValue() {
        rewriteRun(
          //language=java
          java(
            """
              import dev.openfeature.sdk.Client;
              
              class Foo {
                  void bar(Client client) {
                      if (client.getBooleanValue("flag-key-123abc", false)) {
                          System.out.println("Feature enabled");
                      } else {
                          System.out.println("Feature disabled");
                      }
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Client;
              
              class Foo {
                  void bar(Client client) {
                      System.out.println("Feature enabled");
                  }
              }
              """
          )
        );
    }
}
