plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "LaunchDarkly Migration"

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:$rewriteVersion"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.meta:rewrite-analysis:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:$rewriteVersion")

    testImplementation("org.openrewrite:rewrite-java-17")
    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-gradle")
    testImplementation("org.openrewrite.gradle.tooling:model:$rewriteVersion")
    testImplementation("org.openrewrite:rewrite-maven")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testRuntimeOnly("org.gradle:gradle-tooling-api:latest.release")
}

recipeDependencies {
    parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:5.10.+")
    parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:6.+")
    //parserClasspath("com.launchdarkly:launchdarkly-java-server-sdk:7.+")
}