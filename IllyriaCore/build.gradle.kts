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

group = "org.xodium.illyriacore"
version = "$mcVersion+build.${buildNumber.get()}"
description = "Minecraft plugin that enhances the base gameplay"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.wyck.dev/releases")
    maven("https://repo.wyck.dev/snapshots")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")
    compileOnly("com.google.code.gson:gson:2.14.0")

    implementation(project(":IllyriaLib"))
    implementation(kotlin("stdlib"))
    implementation("com.github.retrooper:packetevents-spigot:2.14.0")
    implementation("dev.wyck:wyck-vanilla:4.0.0-837d6ec")
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
        resources.srcDirs("resources")
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
        dependsOn(processResources)
        archiveBaseName.set("IllyriaCore")
        archiveClassifier.set("")
        relocate("dev.wyck", "${project.group}.libs.wyck")
        relocate("com.github.retrooper", "${project.group}.libs.packetevents")
        relocate("io.github.retrooper", "${project.group}.libs.packetevents")
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
    main.set("org.xodium.illyriacore.IllyriaCore")
    name.set("IllyriaCore")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
    bootstrapper.set("org.xodium.illyriacore.IllyriaCoreBootstrap")
}
