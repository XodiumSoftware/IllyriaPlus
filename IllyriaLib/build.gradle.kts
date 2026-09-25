plugins {
    id("java")

    kotlin("jvm")

    id("org.jlleitschuh.gradle.ktlint")
}

group = "org.xodium.illyrialib"
description = "Shared library for the IllyriaPlus plugin modules"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    implementation(kotlin("stdlib"))
    compileOnly("com.google.code.gson:gson:2.14.0")
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
    withType<JavaCompile> { options.encoding = "UTF-8" }
}
