import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")

    kotlin("jvm") version "2.4.20"

    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper")
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

val mcVersion = "26.2"
val buildNumber =
    providers
        .exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput
        .asText
        .map { it.trim() }

group = "org.xodium.illyriakingdoms"
version = "$mcVersion+build.${buildNumber.get()}"
description = "Minecraft plugin that adds a kingdom system"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")

    implementation(kotlin("stdlib"))
    compileOnly("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("xyz.xenondevs.invui:invui:2.3.2")
    implementation("xyz.xenondevs.invui:invui-kotlin:2.3.2")
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
        archiveClassifier.set("")
        relocate("xyz.xenondevs.commons", "${project.group}.libs.commons")
        relocate("xyz.xenondevs.invui", "${project.group}.libs.invui")
        minimize()
    }
    jar { enabled = false }
    runServer { minecraftVersion(mcVersion) }
    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType(AbstractRun::class) { jvmArgs("-XX:+AllowEnhancedClassRedefinition") }
}

paperPluginYaml {
    main.set("org.xodium.illyriakingdoms.IllyriaKingdoms")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
}
