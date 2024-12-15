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
package org.openrewrite.featureflags.ff4j;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveCheckTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveCheck("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion().classpath("ff4j-core"));
    }

    @Test
    @DocumentExample
    void removeCheck() {
        rewriteRun(
          //language=java
          java(
            """
              import org.ff4j.FF4j;

              class Test {
                  void bar(FF4j ff4j) {
                      if (ff4j.check("flag-key-123abc")) {
                          System.out.println("Feature enabled");
                      } else {
                          System.out.println("Feature disabled");
                      }
                  }
              }
              """,
            """
              import org.ff4j.FF4j;

              class Test {
                  void bar(FF4j ff4j) {
                      System.out.println("Feature enabled");
                  }
              }
              """
          )
        );
    }
}
