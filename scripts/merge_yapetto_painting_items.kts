#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

import kotlinx.serialization.json.*
import java.io.File

const val VANILLA_FILE = "assets/minecraft/items/painting.json"
const val ILLYRIAPLUS_MODELS_DIR = "assets/illyriaplus/models/item/painting"
const val ILLYRIAPLUS_NAMESPACE = "illyriaplus"

val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun findPackRoot(): File? {
    val cwd = File("").absoluteFile
    val candidates = listOf(cwd, File(cwd, "resourcepack"))
    candidates.forEach { candidate ->
        if (File(candidate, "pack.mcmeta").isFile && File(candidate, VANILLA_FILE).isFile) {
            return candidate
        }
    }

    var path = cwd
    repeat(5) {
        if (File(path, "pack.mcmeta").isFile && File(path, VANILLA_FILE).isFile) {
            return path
        }
        val parent = path.parentFile ?: return null
        if (parent == path) return null
        path = parent
    }
    return null
}

fun loadJson(file: File): JsonObject = json.parseToJsonElement(file.readText()).jsonObject

fun saveJson(file: File, data: JsonObject) {
    file.writeText(json.encodeToString(JsonObject.serializer(), data) + "\n")
}

fun collectIllyriaplusVariants(modelsDir: File): List<String> {
    if (!modelsDir.isDirectory) return emptyList()
    return modelsDir.listFiles { f -> f.isFile && f.extension == "json" }
        ?.map { it.nameWithoutExtension }
        ?.sorted()
        ?: emptyList()
}

fun makeCase(variant: String): JsonObject = buildJsonObject {
    put("when", "${ILLYRIAPLUS_NAMESPACE}:${variant}")
    putJsonObject("model") {
        put("type", "minecraft:model")
        put("model", "${ILLYRIAPLUS_NAMESPACE}:item/painting/${variant}")
    }
}

fun getCurrentIllyriaplusCases(cases: JsonArray): Set<String> =
    cases.mapNotNull { it.jsonObject["when"]?.jsonPrimitive?.content }
        .filter { it.startsWith("${ILLYRIAPLUS_NAMESPACE}:") }
        .map { it.removePrefix("${ILLYRIAPLUS_NAMESPACE}:") }
        .toSet()

fun checkMerge(data: JsonObject, expectedVariants: List<String>): Triple<Boolean, Set<String>, Set<String>> {
    val cases = try {
        data["model"]?.jsonObject?.["models"]?.jsonArray?.get(0)?.jsonObject?.["cases"]?.jsonArray
            ?: return Triple(false, expectedVariants.toSortedSet(), emptySet())
    } catch (e: Exception) {
        return Triple(false, expectedVariants.toSortedSet(), emptySet())
    }

    val current = getCurrentIllyriaplusCases(cases)
    val expected = expectedVariants.toSet()
    val missing = (expected - current).toSortedSet()
    val extra = (current - expected).toSortedSet()
    return Triple(missing.isEmpty() && extra.isEmpty(), missing, extra)
}

fun merge(
    data: JsonObject,
    expectedVariants: List<String>
): Quadruple<JsonObject, Int, Int, Int> {
    val selectModel = data["model"]?.jsonObject?.["models"]?.jsonArray?.get(0)?.jsonObject
        ?: error("Invalid painting.json structure: missing model/models/0")
    val cases = selectModel["cases"]?.jsonArray
        ?: error("Invalid painting.json structure: missing cases")

    val vanillaCases = cases.filter { case ->
        val whenValue = case.jsonObject["when"]?.jsonPrimitive?.content ?: ""
        !whenValue.startsWith("${ILLYRIAPLUS_NAMESPACE}:") && !whenValue.startsWith("yapetto:")
    }
    val removed = cases.size - vanillaCases.size

    val illyriaplusCases = expectedVariants.map { makeCase(it) }

    val newSelectModel = JsonObject(
        selectModel.toMutableMap().apply {
            put("cases", JsonArray(vanillaCases + illyriaplusCases))
        }
    )
    val models = data["model"]?.jsonObject?.["models"]?.jsonArray
        ?: error("Invalid painting.json structure")
    val newModels = JsonArray(models.mapIndexed { index, element ->
        if (index == 0) newSelectModel else element
    })

    val newData = JsonObject(
        data.toMutableMap().apply {
            val model = data["model"]?.jsonObject ?: error("Invalid painting.json structure")
            put("model", JsonObject(model.toMutableMap().apply { put("models", newModels) }))
        }
    )

    return Quadruple(newData, vanillaCases.size, illyriaplusCases.size, removed)
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun main(args: Array<String>): Int {
    val checkMode = args.contains("--check")

    val root = findPackRoot()
    if (root == null) {
        System.err.println("Error: could not find resource pack root containing pack.mcmeta and ${VANILLA_FILE}.")
        return 1
    }

    if (!checkMode) {
        println("Working in resource pack root: ${root}")
    }

    val vanillaFile = File(root, VANILLA_FILE)
    val modelsDir = File(root, ILLYRIAPLUS_MODELS_DIR)

    val data = loadJson(vanillaFile)
    val illyriaplusVariants = collectIllyriaplusVariants(modelsDir)

    if (checkMode) {
        val (ok, missing, extra) = checkMerge(data, illyriaplusVariants)
        if (!ok) {
            if (missing.isNotEmpty()) {
                System.err.println("Error: ${VANILLA_FILE} is missing ${missing.size} IllyriaPlus case(s):")
                missing.forEach { System.err.println("  - ${ILLYRIAPLUS_NAMESPACE}:${it}") }
            }
            if (extra.isNotEmpty()) {
                System.err.println("Error: ${VANILLA_FILE} has ${extra.size} unexpected IllyriaPlus case(s):")
                extra.forEach { System.err.println("  - ${ILLYRIAPLUS_NAMESPACE}:${it}") }
            }
            System.err.println("Run 'kotlin scripts/merge_yapetto_painting_items.main.kts' to fix.")
            return 1
        }
        println("Check passed: ${illyriaplusVariants.size} IllyriaPlus cases present in ${VANILLA_FILE}")
        return 0
    }

    val (newData, vanillaCount, illyriaplusCount, removed) = merge(data, illyriaplusVariants)
    saveJson(vanillaFile, newData)

    println("Updated ${VANILLA_FILE}")
    println("  Vanilla cases: ${vanillaCount}")
    println("  Removed old illyriaplus cases: ${removed}")
    println("  Added illyriaplus cases: ${illyriaplusCount}")
    println("  Total cases: ${vanillaCount + illyriaplusCount}")
    return 0
}

System.exit(main(args))
