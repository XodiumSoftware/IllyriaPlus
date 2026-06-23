import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")

    kotlin("jvm") version "2.4.0"

    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

val mcVersion = "26.1.2"
val buildNumber =
    providers
        .exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText
        .map { it.trim() }

group = "org.xodium.illyriaplus"
version = "$mcVersion+build.${buildNumber.get()}"
description = "Minecraft plugin that enhances the base gameplay"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")

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
        resources.srcDirs("resources")
    }
}

dokka {
    moduleName.set("IllyriaPlus")
    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs"))
    }
}

tasks {
    shadowJar {
        dependsOn(processResources)
        archiveClassifier.set("")
        destinationDirectory.set(layout.projectDirectory.dir("build/libs"))
        minimize()
    }
    jar { enabled = false }
    runServer { minecraftVersion(mcVersion) }
    withType<JavaCompile> { options.encoding = "UTF-8" }
    withType(AbstractRun::class) { jvmArgs("-XX:+AllowEnhancedClassRedefinition") }
}

paperPluginYaml {
    main.set("org.xodium.illyriaplus.IllyriaPlus")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
    bootstrapper.set("org.xodium.illyriaplus.IllyriaPlusBootstrap")
}

fun yapettoExtractCasesSection(text: String): Triple<String, String, String>? {
    val casesKey = "\"cases\":"
    val firstModelStart = text.indexOf("\"model\":")
    if (firstModelStart == -1) return null
    val modelsStart = text.indexOf("\"models\":", firstModelStart)
    if (modelsStart == -1) return null
    val arrayStart = text.indexOf('[', modelsStart)
    if (arrayStart == -1) return null
    val firstObjectStart = text.indexOf('{', arrayStart)
    if (firstObjectStart == -1) return null
    val casesIndex = text.indexOf(casesKey, firstObjectStart)
    if (casesIndex == -1) return null
    val casesArrayStart = text.indexOf('[', casesIndex)
    if (casesArrayStart == -1) return null

    var depth = 1
    var inString = false
    var escape = false
    for (i in (casesArrayStart + 1) until text.length) {
        val c = text[i]
        when {
            escape -> escape = false
            c == '\\' && inString -> escape = true
            c == '"' -> inString = !inString
            inString -> { }
            c == '[' -> depth++
            c == ']' -> {
                depth--
                if (depth == 0) {
                    return Triple(
                        text.substring(0, casesArrayStart + 1),
                        text.substring(casesArrayStart + 1, i),
                        text.substring(i),
                    )
                }
            }
        }
    }
    return null
}

fun yapettoSplitCases(arrayContent: String): List<String> {
    val cases = mutableListOf<String>()
    var start = -1
    var lineStart = 0
    var depth = 0
    var inString = false
    var escape = false
    arrayContent.forEachIndexed { index, c ->
        when {
            escape -> escape = false
            c == '\\' && inString -> escape = true
            c == '"' -> inString = !inString
            inString -> { }
            c == '\n' -> lineStart = index + 1
            c == '{' -> {
                if (depth == 0) start = lineStart
                depth++
            }
            c == '}' -> {
                depth--
                if (depth == 0 && start != -1) {
                    cases.add(arrayContent.substring(start, index + 1))
                    start = -1
                }
            }
        }
    }
    return cases
}

fun yapettoCaseWhen(caseText: String): String? {
    val key = "\"when\":"
    val idx = caseText.indexOf(key)
    if (idx == -1) return null
    var i = idx + key.length
    while (i < caseText.length && (caseText[i].isWhitespace() || caseText[i] == ':')) i++
    if (i >= caseText.length || caseText[i] != '"') return null
    i++
    val sb = StringBuilder()
    while (i < caseText.length) {
        val c = caseText[i]
        if (c == '\\' && i + 1 < caseText.length) {
            sb.append(caseText[i + 1])
            i += 2
        } else if (c == '"') {
            break
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

fun yapettoMakeCase(variant: String): String =
    """          {
            "when": "illyriaplus:$variant",
            "model": {
              "type": "minecraft:model",
              "model": "illyriaplus:item/painting/$variant"
            }
          }"""

fun yapettoCollectVariants(modelsDir: java.io.File): List<String> {
    if (!modelsDir.isDirectory) return emptyList()
    return modelsDir
        .listFiles { f -> f.isFile && f.extension == "json" }
        ?.map { it.nameWithoutExtension }
        ?.sorted()
        ?: emptyList()
}

tasks.register("mergeYapettoPaintings") {
    group = "resourcepack"
    description = "Merge IllyriaPlus painting item cases into the vanilla painting.json."
    notCompatibleWithConfigurationCache("uses project layout at execution time")
    doLast {
        val vanillaFile =
            layout.projectDirectory.file("resourcepack/assets/minecraft/items/painting.json").asFile
        val modelsDir =
            layout.projectDirectory.file("resourcepack/assets/illyriaplus/models/item/painting").asFile
        val text = vanillaFile.readText()
        val section =
            yapettoExtractCasesSection(text)
                ?: error("Could not find cases array in ${vanillaFile.relativeTo(project.rootDir)}")
        val caseObjects = yapettoSplitCases(section.second)
        val vanillaCases =
            caseObjects.filter { case ->
                val whenValue = yapettoCaseWhen(case) ?: ""
                !whenValue.startsWith("illyriaplus:") && !whenValue.startsWith("yapetto:")
            }
        val expectedVariants = yapettoCollectVariants(modelsDir)
        val illyriaplusCases = expectedVariants.map { yapettoMakeCase(it) }
        val allCases = vanillaCases + illyriaplusCases
        val innerIndentation =
            if (allCases.isEmpty()) {
                ""
            } else {
                "\n${allCases.joinToString(",\n")}\n        "
            }
        vanillaFile.writeText(section.first + innerIndentation + section.third)
        println("Updated ${vanillaFile.relativeTo(project.rootDir)}")
        println("  Vanilla cases: ${vanillaCases.size}")
        println("  Removed old modded cases: ${caseObjects.size - vanillaCases.size}")
        println("  Added illyriaplus cases: ${illyriaplusCases.size}")
        println("  Total cases: ${allCases.size}")
    }
}

tasks.register("checkYapettoPaintings") {
    group = "resourcepack"
    description = "Verify IllyriaPlus painting item cases are merged into painting.json."
    notCompatibleWithConfigurationCache("uses project layout at execution time")
    doLast {
        val vanillaFile =
            layout.projectDirectory.file("resourcepack/assets/minecraft/items/painting.json").asFile
        val modelsDir =
            layout.projectDirectory.file("resourcepack/assets/illyriaplus/models/item/painting").asFile
        val text = vanillaFile.readText()
        val section =
            yapettoExtractCasesSection(text)
                ?: error("Could not find cases array in ${vanillaFile.relativeTo(project.rootDir)}")
        val caseObjects = yapettoSplitCases(section.second)
        val current =
            caseObjects
                .mapNotNull { yapettoCaseWhen(it) }
                .filter { it.startsWith("illyriaplus:") }
                .map { it.removePrefix("illyriaplus:") }
                .toSet()
        val expected = yapettoCollectVariants(modelsDir).toSet()
        val missing = (expected - current).toSortedSet()
        val extra = (current - expected).toSortedSet()
        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            if (missing.isNotEmpty()) {
                logger.error("Error: painting.json is missing ${missing.size} IllyriaPlus case(s):")
                missing.forEach { logger.error("  - illyriaplus:$it") }
            }
            if (extra.isNotEmpty()) {
                logger.error("Error: painting.json has ${extra.size} unexpected IllyriaPlus case(s):")
                extra.forEach { logger.error("  - illyriaplus:$it") }
            }
            error("Run './gradlew mergeYapettoPaintings' to fix.")
        }
        println("Check passed: ${expected.size} IllyriaPlus cases present in painting.json")
    }
}
