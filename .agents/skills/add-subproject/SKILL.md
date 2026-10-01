---
name: add-subproject
description: Scaffolds a new Gradle subproject (plugin module) in the IllyriaPlus monorepo with build config, main class, and settings registration.
---

# Add a Subproject

Use this skill when the user wants to create a new plugin module in the IllyriaPlus monorepo.

## Before Writing Code

1. Ask the user:
    - What is the module name? (e.g., `IllyriaQuests`, `IllyriaEconomy`)
    - What is the description/purpose of the module?
    - Does it need NMS access (paperweight userdev)? If yes, follow the IllyriaBridge pattern.
    - Does it need PacketEvents or other extra dependencies?
    - Does it need a bootstrap class (for registry access)?
    - Does it need a `resources/` directory (e.g., for structures)?

2. Convert the module name to camelCase for the package name (e.g., `IllyriaQuests` → `org.xodium.illyriaquests`).

## Creating the Module

1. Create the module directory: `IllyriaPlus/<ModuleName>/`
2. Create subdirectories:
    - `src/` — Kotlin source directory
    - `resources/` — if the module needs bundled resources
3. Create `build.gradle.kts` (see template below).
4. Create `src/<ModuleName>.kt` — main plugin class (see template below).
5. If a bootstrap class is needed, create `src/<ModuleName>Bootstrap.kt`.
6. Update `settings.gradle.kts` to include the new module.
7. Update `.github/workflows/ci.yml` to include the new module (see CI section below).

## Build Configuration Template

Use this as the base. Adjust based on the answers above:

```kotlin
import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")

    kotlin("jvm")

    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("xyz.jpenilla.resource-factory-paper-convention")
    id("org.jlleitschuh.gradle.ktlint")
    // Add only if NMS access needed:
    // id("io.papermc.paperweight.userdev")
}

val mcVersion = "26.3"
val buildNumber =
    providers
        .exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput
        .asText
        .map { it.trim() }

group = "org.xodium.<modulename>"
version = "$mcVersion+build.${buildNumber.get()}"
description = "<description>"

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

    // If NMS needed, replace the paper-api compileOnly with:
    // paperweight.paperDevBundle("$mcVersion.build.+")

    // Add extra dependencies here (e.g., PacketEvents)
    // implementation("com.github.retrooper:packetevents-spigot:2.14.0")
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
        // Only include if module has resources:
        // resources.srcDirs("resources")
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
        archiveBaseName.set("<ModuleName>")
        archiveClassifier.set("")
        relocate("org.xodium.illyrialib", "${project.group}.libs.illyrialib")
        // If using PacketEvents, add:
        // relocate("com.github.retrooper", "${project.group}.libs.packetevents")
        // relocate("io.github.retrooper", "${project.group}.libs.packetevents")
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
    main.set("org.xodium.<modulename>.<ModuleName>")
    name.set("<ModuleName>")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
    // If bootstrap class exists:
    // bootstrapper.set("org.xodium.<modulename>.<ModuleName>Bootstrap")
}
```

## Main Class Template

```kotlin
package org.xodium.<modulename>

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateServerVersion

/** Main class of the plugin. */
internal class <ModuleName> : JavaPlugin() {
    companion object {
        lateinit var instance: <ModuleName>
            private set
    }

    override fun onEnable() {
        if (!validateServerVersion()) return

        instance = this

        UpdateChecker(this).check()
    }
}
```

## Settings Registration

Append the module name (quoted, camelCase-preserved) to the `include()` call in `settings.gradle.kts`:

```kotlin
include("IllyriaLib", "IllyriaCore", "IllyriaBridge", ..., "<ModuleName>")
```

## CI Workflow Changes

In `.github/workflows/ci.yml`:

### 1. Changes Filter

Add a `<filtername>` output to the `changes` job and its path filter:

```yaml
outputs:
    <filtername>: ${{ steps.filter.outputs.<filtername> }}

# In the filters block:
<filtername>:
    - "<ModuleName>/src/**"
    - "<ModuleName>/build.gradle.kts"
    - "IllyriaLib/**"
```

### 2. Lint Job

Add `needs.changes.outputs.<filtername> == 'true'` to the lint job's `if` condition and all Kotlin-related step conditions.

### 3. Build & Release Jobs

Add `build_<filtername>` and `release_<filtername>` jobs. Copy the pattern from `build_bridge` / `release_bridge`, replacing the module name and artifact path. Both jobs must have `needs: [changes, lint]` (build) or `needs: [changes, build_<filtername>]` (release).

## Validation

Run `./gradlew :<ModuleName>:shadowJar` and confirm `BUILD SUCCESSFUL`.

The output JAR should appear at `<ModuleName>/build/libs/<ModuleName>-<version>.jar`.

After finishing, summarize the files changed and ask the user if they want to commit.
