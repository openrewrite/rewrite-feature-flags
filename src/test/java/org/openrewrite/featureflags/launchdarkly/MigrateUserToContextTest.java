/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.featureflags.launchdarkly;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.featureflags.launchdarkly.MigrateUserToContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateUserToContextTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateUserToContext())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-5"));
    }

    @Test
    @DocumentExample
    void builderUserToContext() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.LDValue;
                                
              class A {
                  void foo() {
                      LDUser user = new LDUser.Builder("user-key-123abc")
                              .name("Sandy")
                              .email("sandy@example.com")
                              .custom("groups", LDValue.buildArray().add("Google").add("Microsoft").build())
                              .build();
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.LDValue;
                                
              class A {
                  void foo() {
                      LDContext user = LDContext.builder("user-key-123abc")
                              .name("Sandy")
                              .set("email", "sandy@example.com")
                              .set("groups", LDValue.buildArray().add("Google").add("Microsoft").build())
                              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void userToContext() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDUser;
                                
              class A {
                void foo() {
                  LDUser user = new LDUser("user-key-123abc");
                }
              }
              """,
            """
              import com.launchdarkly.sdk.LDContext;
                                
              class A {
                void foo() {
                  LDContext user = LDContext.create("user-key-123abc");
                }
              }
              """
          )
        );
    }

    @Test
    void privateAttribute() {
        rewriteRun(
          //language=java
          java(
            """
              import com.launchdarkly.sdk.LDUser;
              import com.launchdarkly.sdk.LDValue;
                                
              class A {
                  void foo() {
                      LDUser user = new LDUser.Builder("user-key-123abc")
                              .name("Sandy")
                              .privateEmail("sandy@example.com")
                              .privateCustom("groups", LDValue.buildArray().add("Google").add("Microsoft").build())
                              .build();
                  }
              }
              """,
            """
              import com.launchdarkly.sdk.LDContext;
              import com.launchdarkly.sdk.LDValue;
                                
              class A {
                  void foo() {
                      LDContext user = LDContext.builder("user-key-123abc")
                              .name("Sandy")
                              .set("email", "sandy@example.com")
                              .set("groups", LDValue.buildArray().add("Google").add("Microsoft").build())
                              .privateAttributes("email", "groups")
                              .build();
                  }
              }
              """
          )
        );
    }
}
