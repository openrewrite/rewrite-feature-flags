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
package org.openrewrite.launchdarkly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeLaunchDarkly6Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.launchdarkly")
            .build()
            .activateRecipes("org.openrewrite.launchdarkly.UpgradeLaunchDarkly6"))
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-5"));
    }

    @Nested
    class Dependencies {
        @Test
        @DocumentExample("Maven")
        void mavenDependency() {
            rewriteRun(
              //language=xml
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                      <dependency>
                        <groupId>com.launchdarkly</groupId>
                        <artifactId>launchdarkly-java-server-sdk</artifactId>
                        <version>5.10.9</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                      Matcher matcher = Pattern.compile("<version>(6\\.\\d\\.\\d+)</version>").matcher(actual);
                      assertTrue(matcher.find(), actual);
                      return """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>demo</artifactId>
                          <version>0.0.1-SNAPSHOT</version>
                          <dependencies>
                            <dependency>
                              <groupId>com.launchdarkly</groupId>
                              <artifactId>launchdarkly-java-server-sdk</artifactId>
                              <version>%s</version>
                            </dependency>
                          </dependencies>
                        </project>
                        """.formatted(matcher.group(1));
                  }
                )
              )
            );
        }

        @Test
        @DocumentExample("Gradle")
        void gradleDependency() {
            rewriteRun(
              spec -> spec.beforeRecipe(withToolingApi()),
              buildGradle(
                """
                  plugins {
                    id "java"
                  }
                  
                  repositories {
                    mavenCentral()
                  }
                  
                  dependencies {
                    implementation "com.launchdarkly:launchdarkly-java-server-sdk:5.10.9"
                  }
                  """,
                spec -> spec.after(actual -> {
                    Matcher matcher = Pattern.compile("com\\.launchdarkly:launchdarkly-java-server-sdk:(6\\.\\d+\\.\\d+)").matcher(actual);
                    assertTrue(matcher.find(), actual);
                    return """
                        plugins {
                          id "java"
                        }
                        
                        repositories {
                          mavenCentral()
                        }
                        
                        dependencies {
                          implementation "com.launchdarkly:launchdarkly-java-server-sdk:%s"
                        }
                        """.formatted(matcher.group(1));
                  }
                )
              )
            );
        }
    }


    @Nested
    class CodeChanges {
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
}