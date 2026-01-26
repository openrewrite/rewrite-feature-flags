plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    id("org.openrewrite.build.moderne-source-available-license") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Feature flag migration"

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:$rewriteVersion"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.meta:rewrite-analysis:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:$rewriteVersion")

    implementation("org.assertj:assertj-core:3.27.4")
    testImplementation("org.openrewrite:rewrite-java-21")
    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-gradle")
    testImplementation("org.openrewrite.gradle.tooling:model:$rewriteVersion")
    testImplementation("org.openrewrite:rewrite-maven")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.14.2")

    testImplementation("dev.openfeature:sdk:latest.release")
    testImplementation("io.getunleash:unleash-client-java:latest.release")
    testImplementation("io.quarkiverse.flags:quarkus-flags:latest.release")
    testImplementation("org.ff4j:ff4j-core:2.0.0") // 2.1.x requires Java 21

    testRuntimeOnly("com.launchdarkly:launchdarkly-java-server-sdk:5.+")

    testRuntimeOnly(gradleApi())
}

recipeDependencies {
    parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:6.+")
    //parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:7.+")
}
