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
package org.openrewrite.featureflags.unleash;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveIsEnabledTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveIsEnabled("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion().classpath("unleash-client-java"));
    }

    @Test
    @DocumentExample
    void removeIsEnabled() {
        rewriteRun(
          //language=java
          java(
            """
              import io.getunleash.Unleash;

              class Test {
                  void bar(Unleash unleash) {
                      if (unleash.isEnabled("flag-key-123abc")) {
                          System.out.println("Feature enabled");
                      } else {
                          System.out.println("Feature disabled");
                      }
                  }
              }
              """,
            """
              import io.getunleash.Unleash;

              class Test {
                  void bar(Unleash unleash) {
                      System.out.println("Feature enabled");
                  }
              }
              """
          )
        );
    }
}
