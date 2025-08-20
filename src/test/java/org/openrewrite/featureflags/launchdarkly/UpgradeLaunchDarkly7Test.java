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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeLaunchDarkly7Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.featureflags")
            .build()
            .activateRecipes("org.openrewrite.featureflags.launchdarkly.UpgradeLaunchDarkly7"))
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "launchdarkly-java-server-sdk-6.+"));
    }

    @Nested
    class Dependencies {
        @DocumentExample("Maven")
        @Test
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
                        <version>6.3.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                      Matcher matcher = Pattern.compile("<version>(7[.][0-9]+[.][0-9])</version>").matcher(actual);
                    assertThat(matcher.find()).as(actual).isTrue();
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
                    implementation "com.launchdarkly:launchdarkly-java-server-sdk:6.3.0"
                  }
                  """,
                spec -> spec.after(actual -> {
                    Matcher matcher = Pattern.compile("com\\.launchdarkly:launchdarkly-java-server-sdk:(7[.][0-9]+[.][0-9])").matcher(actual);
                    assertThat(matcher.find()).as(actual).isTrue();
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
}
