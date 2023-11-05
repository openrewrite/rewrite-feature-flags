/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.launchdarkly;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveBoolVariationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveBoolVariation("flag-key-123abc", true))
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6"));
    }

    @Test
    void enablePermanently() {
        rewriteRun(
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                      else {
                        // The code to run if the feature is off
                          System.out.println("Feature is off");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      // Application code to show the feature
                      System.out.println("Feature is on");
                  }
              }   
              """
          )
        );
    }

    @Test
    void disablePermanently() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBoolVariation("flag-key-123abc", false)),
          // language=java
          java(
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      if (client.boolVariation("flag-key-123abc", context, false)) {
                          // Application code to show the feature
                          System.out.println("Feature is on");
                      }
                      else {
                        // The code to run if the feature is off
                          System.out.println("Feature is off");
                      }
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.*;
              import com.launchdarkly.sdk.server.*;
              class Foo {
                  LDClient client = new LDClient("sdk-key-123abc");
                  void bar() {
                      LDContext context = LDContext.builder("context-key-123abc")
                        .name("Sandy")
                        .build();
                      // The code to run if the feature is off
                      System.out.println("Feature is off");
                  }
              }
              """
          )
        );
    }

    // TODO Add additional tests for other variations of the feature flag check
    // - removal of unused LDContext
    // - removal of unused LDClient
    // - negated `client.boolVariation` check
    // - permanently disable flag
    // - checks other than `client.boolVariation`
}
