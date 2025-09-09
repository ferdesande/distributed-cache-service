plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.detekt)
}

group = "com.fsg"
version = "0.0.1-SNAPSHOT"
description = "Cache Service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
    toolVersion = "1.23.8"
    config.setFrom(("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
    parallel = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAll") {
    description = "Runs detekt on the whole project at once."
    parallel = true
    setSource(files(projectDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/build/**")
    exclude("**/resources/**")
    config.setFrom(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}
