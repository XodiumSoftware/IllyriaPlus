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

group = "org.xodium.illyriaseasons"
version = "$mcVersion+build.${buildNumber.get()}"
description = "Seasonal biome visual effects driven by Wyck"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.wyck.dev/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")

    implementation("dev.wyck:wyck:3.4.0")
    implementation(kotlin("stdlib"))
    implementation(project(":IllyriaLib"))
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
        archiveBaseName.set("IllyriaSeasons")
        archiveClassifier.set("")
        relocate("dev.wyck", "${project.group}.libs.wyck")
        relocate("org.xodium.illyrialib", "${project.group}.libs.illyrialib")
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
    main.set("org.xodium.illyriaseasons.IllyriaSeasons")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
}
