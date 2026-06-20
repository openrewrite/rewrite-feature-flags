/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.featureflags.launchdarkly;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateLDContextToEvaluationContextTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLDContextToEvaluationContext())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void builderWithAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;

              class A {
                  LDContext context() {
                      return LDContext.builder("user-key-123abc")
                              .name("Sandy")
                              .set("email", "sandy@example.com")
                              .build();
                  }
              }
              """,
            """
              import dev.openfeature.sdk.EvaluationContext;
              import dev.openfeature.sdk.MutableContext;

              class A {
                  EvaluationContext context() {
                      return new MutableContext("user-key-123abc")
                              .add("name", "Sandy")
                              .add("email", "sandy@example.com");
                  }
              }
              """
          )
        );
    }

    @Test
    void createSingleKey() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDContext;

              class A {
                  LDContext context() {
                      return LDContext.create("user-key-123abc");
                  }
              }
              """,
            """
              import dev.openfeature.sdk.EvaluationContext;
              import dev.openfeature.sdk.MutableContext;

              class A {
                  EvaluationContext context() {
                      return new MutableContext("user-key-123abc");
                  }
              }
              """
          )
        );
    }
}
