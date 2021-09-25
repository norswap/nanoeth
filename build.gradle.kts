// === PLUGINS =====================================================================================

plugins {
    java
    idea
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    // Using the lastest 5.3, I run into: https://issuetracker.google.com/issues/166468915
    id("io.freefair.javadoc-links") version "5.1.1"
}

// === MAIN BUILD DETAILS ==========================================================================

group = "com.norswap"
version = "0.0.1"
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
    options.compilerArgs.add("-Xlint:unchecked")
}

// There must be an "ethereumTests" property (e.g. in gradle.properties) pointing to a local
// clone of github.com/ethereum/tests.
val ethereumTests: String by project

tasks.test {
    useTestNG()
    // pass the path to the tests
    systemProperty("ethereumTests", ethereumTests)
}

tasks.javadoc.get().options {
    // https://github.com/gradle/gradle/issues/7038
    this as StandardJavadocDocletOptions
    addStringOption("Xdoclint:none", "-quiet")
    if (JavaVersion.current().isJava9Compatible)
        addBooleanOption("html5", true) // nice future proofing

    // Normally we would use `links = listOf(...)` here, but it doesn't work with javadoc.io.
    // Instead, we use the io.freefair.javadoc-links plugin.
}

tasks.javadoc {
    // Copy the javadoc into /docs, where it will be picked up by github-pages.
    doLast {
        copy {
            from("$buildDir/docs/javadoc")
            into("docs")
        }
    }
}

// === IDE =========================================================================================

idea.module {
    // Download javadoc & sources for dependencies.
    isDownloadJavadoc = true
    isDownloadSources = true
}

// === PUBLISHING ==================================================================================

// Publication definition
publishing.publications.create<MavenPublication>(project.name) {
    from(components["java"])
    pom.withXml {
        val root = asNode()
        root.appendNode("name", project.name)
        root.appendNode("description", project.description)
        root.appendNode("url", website)
        root.appendNode("scm").apply {
            appendNode("url", website)
            val connection = "scm:git:git@github.com:norswap/${project.name}.git"
            appendNode("connection", connection)
            appendNode("developerConnection", connection)
        }
        root.appendNode("licenses").appendNode("license").apply {
            appendNode("name", "The BSD 3-Clause License")
            appendNode("url", "$website/blob/master/LICENSE")
        }
        root.appendNode("developers").appendNode("developer").apply {
            appendNode("id", "norswap")
            appendNode("name", "Nicolas Laurent")
        }
    }
}

signing {
    // Create a 'gradle.properties' file at the root of the project, containing the next two lines,
    // replacing the values as needed:
    // signing.gnupg.keyName=<KEY_ID>
    // signing.gnupg.passphrase=<PASSWORD_WITHOUT_QUOTES>

    // Note the key ID is a suffix of the public key displayed by `gpg --list-keys`.

    // You'll need to have GnuPG installed, and it should be aliased to "gpg2"
    // (homebrew on mac links it to only "gpg" by default).

    // You are forced to use the agent, because otherwise Gradle wants a private keyring, which
    // gnupg doesn't create by default since version 2.x.y. An alternative is to export the
    // private keys to a keyring file.

    // You will have to publish your key with:
    // $ gpg --send-key <KEY_ID>

    // On my mac this wouldn't work until I followed this steps:
    // https://bastide.org/2021/06/30/gpg-complains-about-no-keyserver-available/
    // (generate a ~/.gnupg/my.pem file then reference it in a new ~/.gnupg/dirmngr.conf file)

    useGpgCmd()
    sign(publishing.publications[project.name])
}

// The below takes care of publish to Sonatype Nexus (Maven Central).
// Dashboard at https://oss.sonatype.org/
// It's apparently normal for it to take tens of minutes for the released project to show up
// (including on the dashboard's repository search), meaning that you can monitor the status anywhere).
// This whole affair is a massive pile of crap, and a UX nightmare.
// Tip: In the Gradle execution, any SKIPPED step should be a red flag.

// NOTE: Can't release "-SNAPSHOT" releases to Maven with this config.

// Use `gradle publishToSonatype closeAndReleaseSonatypeStagingDirectory` to deploy to Maven Central.
// Or use the the deploy task below (`grade deploy`).
nexusPublishing {
    repositories {
        sonatype {
            // Create a 'gradle.properties' file at the root of the project, containing the next two
            // lines, replacing the values as needed:
            // mavenCentralUsername=<USERNAME>
            // mavenCentralPassword=<PASSWORD>
            username.set(property("mavenCentralUsername") as String)
            password.set(property("mavenCentralPassword") as String)
        }
    }
}

// Deploy to Maven Central
tasks.register("deploy") {
    // NOTE: will deployed all defined publications
    val publishToSonatype = tasks["publishToSonatype"]
    val closeAndReleaseSonatype = tasks["closeAndReleaseSonatypeStagingRepository"]
    dependsOn(publishToSonatype)
    dependsOn(closeAndReleaseSonatype)
    closeAndReleaseSonatype.mustRunAfter(publishToSonatype)
}

// === DEPENDENCIES ================================================================================

repositories {
    mavenCentral()
    maven {
        url = uri("https://norswap.jfrog.io/artifactory/maven")
    }
}

dependencies {
    implementation("com.norswap:utils:2.1.11")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    testImplementation("org.testng:testng:7.4.0")
    testImplementation("org.json:json:20210307")
}

// =================================================================================================