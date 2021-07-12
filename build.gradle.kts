// === PLUGINS =====================================================================================

plugins {
    java
    idea
    // Using the lastest 5.3, I run into: https://issuetracker.google.com/issues/166468915
    id("io.freefair.javadoc-links") version "5.1.1"
}

// === MAIN BUILD DETAILS ==========================================================================

group = "com.norswap"
version = "1.0.0-SNAPSHOT"
description = "Ethereum EVM model"
java.sourceCompatibility = JavaVersion.VERSION_15
java.targetCompatibility = JavaVersion.VERSION_15

val website = "https://github.com/norswap/${project.name}"
val vcs = "https://github.com/norswap/${project.name}.git"

sourceSets.main.get().java.srcDirs("src")
sourceSets.test.get().java.srcDirs("test")

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test.get().useTestNG()

tasks.javadoc.get().options {
    // https://github.com/gradle/gradle/issues/7038
    this as StandardJavadocDocletOptions
    addStringOption("Xdoclint:none", "-quiet")
    if (JavaVersion.current().isJava9Compatible)
        addBooleanOption("html5", true) // nice future proofing

    // Normally we would use `links = listOf(...)` here, but it doesn't work with javadoc.io.
    // Instead, we use the io.freefair.javadoc-links plugin.
}

// === IDE =========================================================================================

idea.module {
    // Download javadoc & sources for dependencies.
    isDownloadJavadoc = true
    isDownloadSources = true
}

// === DEPENDENCIES ================================================================================

repositories {
    mavenCentral()
    maven {
        url = uri("https://norswap.jfrog.io/artifactory/maven")
    }
}

dependencies {
    implementation("com.norswap:utils:2.1.8")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    testImplementation("org.testng:testng:7.4.0")
}

// =================================================================================================