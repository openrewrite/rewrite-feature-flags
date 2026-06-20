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
package org.openrewrite.featureflags.launchdarkly;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MarkIncompatibleEvaluationDetailAccessorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MarkIncompatibleEvaluationDetailAccessors())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @DocumentExample
    @Test
    void flagsUnmappableAccessors() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.EvaluationDetail;

              class A {
                  void inspect(EvaluationDetail<Boolean> detail) {
                      int index = detail.getVariationIndex();
                      boolean isDefault = detail.isDefaultValue();
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.EvaluationDetail;

              class A {
                  void inspect(EvaluationDetail<Boolean> detail) {
                      int index = /* TODO OpenFeature migration: `getVariationIndex()` has no OpenFeature equivalent; `FlagEvaluationDetails` exposes `getVariant()` (a String) instead */ detail.getVariationIndex();
                      boolean isDefault = /* TODO OpenFeature migration: `isDefaultValue()` has no OpenFeature equivalent; inspect `getReason()` / `getErrorCode()` instead */ detail.isDefaultValue();
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesGetValueUntouched() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.EvaluationDetail;

              class A {
                  boolean value(EvaluationDetail<Boolean> detail) {
                      return detail.getValue();
                  }
              }
              """
          )
        );
    }
}
