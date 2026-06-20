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

class MigrateLDValueToValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLDValueToValue())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void scalarOfToNewValue() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDValue;

              class A {
                  LDValue flag() {
                      return LDValue.of("on");
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Value;

              class A {
                  Value flag() {
                      return new Value("on");
                  }
              }
              """
          )
        );
    }

    @Test
    void ofNullToNewValue() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDValue;

              class A {
                  LDValue flag() {
                      return LDValue.ofNull();
                  }
              }
              """,
            """
              import dev.openfeature.sdk.Value;

              class A {
                  Value flag() {
                      return new Value();
                  }
              }
              """
          )
        );
    }

    @Test
    void skipsUnsupportedStructuredBuilders() {
        // buildObject() has no safe scalar mapping, so the file is left untouched for manual migration.
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDValue;

              class A {
                  LDValue flag() {
                      return LDValue.buildObject().put("k", LDValue.of("v")).build();
                  }
              }
              """
          )
        );
    }
}
