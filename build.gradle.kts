plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Feature flag migration"

val rewriteVersion = "latest.release"
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:8.41.1"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.meta:rewrite-analysis:2.13.1")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:1.24.1")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:1.21.1")

    testImplementation("org.openrewrite:rewrite-java-17")
    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-gradle")
    testImplementation("org.openrewrite.gradle.tooling:model:latest.release")
    testImplementation("org.openrewrite:rewrite-maven")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("dev.openfeature:sdk:latest.release")
    testImplementation("io.getunleash:unleash-client-java:latest.release")
    testImplementation("org.ff4j:ff4j-core:2.0.0") // 2.1.x requires Java 21

    testRuntimeOnly("org.gradle:gradle-tooling-api:latest.release")
}

recipeDependencies {
    parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:5.10.+")
    parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:6.+")
    //parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:7.+")
}
