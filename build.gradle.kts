// Root project is an aggregator — the plugin modules live in `IllyriaCore/` and `IllyriaBridge/`.

plugins {
    kotlin("jvm") version "2.4.20" apply false

    id("xyz.jpenilla.run-paper") version "3.1.0" apply false
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.4.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0" apply false
}
