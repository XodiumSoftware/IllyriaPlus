import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")

    kotlin("jvm")

    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("xyz.jpenilla.resource-factory-paper-convention")
    id("org.jlleitschuh.gradle.ktlint")
}

val mcVersion = "26.3"
val buildNumber =
    providers
        .exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput
        .asText
        .map { it.trim() }

group = "org.xodium.illyriamannequins"
version = "$mcVersion+build.${buildNumber.get()}"
description = "Mannequin NPCs for the IllyriaPlus ecosystem"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")
    compileOnly("com.google.code.gson:gson:2.14.0")

    implementation(project(":IllyriaLib"))
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
}

sourceSets {
    main {
        kotlin.srcDirs("src")
    }
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    filter {
        exclude("**/build/**")
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("IllyriaMannequins")
        archiveClassifier.set("")
        relocate("org.xodium.illyrialib", "${project.group}.libs.illyrialib")
        minimize()
    }
    jar { enabled = false }
    runServer {
        minecraftVersion(mcVersion)
        runDirectory = rootProject.layout.projectDirectory.dir(".server")
    }
    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType(AbstractRun::class) { jvmArgs("-XX:+AllowEnhancedClassRedefinition") }
}

paperPluginYaml {
    main.set("org.xodium.illyriamannequins.IllyriaMannequins")
    name.set("IllyriaMannequins")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
}
