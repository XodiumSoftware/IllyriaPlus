import xyz.jpenilla.runtask.task.AbstractRun
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

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
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcVersion.build.+")

    implementation(kotlin("stdlib"))

    implementation("xyz.xenondevs.invui:invui:2.1.1")
    implementation("xyz.xenondevs.invui:invui-kotlin:2.1.1")
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

    register("generateTreeOffsets") {
        group = "IllyriaPlus"
        description = "Generates JSON offset files for tree structures based on their trunk centres."
        notCompatibleWithConfigurationCache("Uses helper functions defined in build script")
        doLast {
            val treesDir = file("resources/structures/trees")
            if (!treesDir.exists()) {
                println("Tree structures directory not found: $treesDir")
                return@doLast
            }

            val nbtFiles = treesDir.walkTopDown().filter { it.isFile && it.extension == "nbt" }.toList()
            println("Found ${nbtFiles.size} .nbt file(s)")

            nbtFiles.forEach { nbtFile ->
                val jsonFile = nbtFile.parentFile.resolve(nbtFile.nameWithoutExtension + ".json")
                val bytes = nbtFile.readBytes()
                val offset = computeTrunkOffset(bytes)
                jsonFile.writeText(
                    "{\"x\": ${offset.first}, \"y\": ${offset.second}, \"z\": ${offset.third}}",
                )
                println(
                    "Generated ${jsonFile.relativeTo(treesDir)} -> " +
                        "(${offset.first}, ${offset.second}, ${offset.third})",
                )
            }
        }
    }
}

/**
 * Computes the trunk centre offset (x, y, z) for a raw NBT structure file.
 *
 * Mirrors the algorithm used by TreeMechanic: find the lowest layer containing log/wood/stem/hyphae
 * blocks, then pick the position with the tallest vertical column, breaking ties by proximity to
 * the structure footprint centre.
 */
fun computeTrunkOffset(bytes: ByteArray): Triple<Int, Int, Int> {
    val root = readNbtRoot(bytes)

    val sizeArray = root.getIntArray("size")
    val sizeX = sizeArray.getOrNull(0) ?: 1
    val sizeZ = sizeArray.getOrNull(2) ?: 1

    val palette =
        root
            .getList("palette")
            .map { (it as NbtCompound).getString("Name") }

    data class LogBlock(val x: Int, val y: Int, val z: Int)

    val logBlocks = mutableListOf<LogBlock>()
    root.getList("blocks").forEach { blockTag ->
        val compound = blockTag as? NbtCompound ?: return@forEach
        val state = compound["state"]?.asInt() ?: return@forEach
        val pos = compound.getList("pos").mapNotNull { (it as? NbtInt)?.value }
        if (pos.size < 3) return@forEach

        val x = pos[0]
        val y = pos[1]
        val z = pos[2]
        if (isTrunkBlockId(palette.getOrNull(state))) {
            logBlocks.add(LogBlock(x, y, z))
        }
    }

    if (logBlocks.isEmpty()) {
        return Triple(sizeX / 2, 0, sizeZ / 2)
    }

    val minY = logBlocks.minOf { it.y }
    val baseLayer = logBlocks.filter { it.y == minY }.map { it.x to it.z }
    val logByPosition = logBlocks.map { Triple(it.x, it.y, it.z) }.toSet()

    fun columnHeight(
        x: Int,
        z: Int,
    ): Int {
        var height = 0
        var y = minY
        while (logByPosition.contains(Triple(x, y, z))) {
            height++
            y++
        }
        return height
    }

    val centerX = sizeX / 2.0
    val centerZ = sizeZ / 2.0
    val trunkBase =
        baseLayer
            .map { (x, z) -> Triple(x, z, columnHeight(x, z)) }
            .sortedWith(
                compareByDescending<Triple<Int, Int, Int>> { it.third }
                    .thenBy {
                        val dx = it.first - centerX
                        val dz = it.second - centerZ
                        dx * dx + dz * dz
                    },
            ).first()

    return Triple(trunkBase.first, minY, trunkBase.second)
}

fun isTrunkBlockId(id: String?): Boolean {
    if (id == null) return false
    val name = id.removePrefix("minecraft:")
    return name.endsWith("_log") ||
        name.endsWith("_wood") ||
        name.endsWith("_stem") ||
        name.endsWith("_hyphae") ||
        name == "mushroom_stem"
}

// ------------------------------------------------------------------
// Minimal NBT reader (matches TreeMechanic.NbtReader)
// ------------------------------------------------------------------

fun readNbtRoot(input: ByteArray): NbtCompound {
    val buffered = ByteArrayInputStream(input).buffered()
    buffered.mark(4)
    val header = buffered.readNBytes(2)
    buffered.reset()

    val isGzip =
        header.size == 2 &&
            header[0] == 0x1f.toByte() &&
            header[1] == 0x8b.toByte()

    val stream = if (isGzip) GZIPInputStream(buffered) else buffered
    DataInputStream(stream).use { dis ->
        val (_, tag) = readNamedTag(dis)
        return tag as NbtCompound
    }
}

fun readNamedTag(dis: DataInputStream): Pair<String, NbtTag> {
    val typeId = dis.readUnsignedByte()
    if (typeId == 0) return "" to NbtEnd
    val nameLen = dis.readUnsignedShort()
    val name =
        if (nameLen > 0) {
            ByteArray(nameLen).also { dis.readFully(it) }.toString(Charsets.UTF_8)
        } else {
            ""
        }
    return name to readPayload(typeId, dis)
}

fun readPayload(
    typeId: Int,
    dis: DataInputStream,
): NbtTag =
    when (typeId) {
        1 -> NbtByte(dis.readByte())
        2 -> NbtShort(dis.readShort())
        3 -> NbtInt(dis.readInt())
        4 -> NbtLong(dis.readLong())
        5 -> NbtFloat(dis.readFloat())
        6 -> NbtDouble(dis.readDouble())
        7 -> {
            val len = dis.readInt()
            NbtByteArray(ByteArray(len).also { dis.readFully(it) })
        }
        8 -> {
            val len = dis.readUnsignedShort()
            NbtString(ByteArray(len).also { dis.readFully(it) }.toString(Charsets.UTF_8))
        }
        9 -> {
            val elemType = dis.readUnsignedByte()
            val len = dis.readInt()
            NbtList(List(len) { readPayload(elemType, dis) })
        }
        10 -> {
            val map = mutableMapOf<String, NbtTag>()
            while (true) {
                val (name, tag) = readNamedTag(dis)
                if (tag is NbtEnd) break
                map[name] = tag
            }
            NbtCompound(map)
        }
        11 -> {
            val len = dis.readInt()
            NbtIntArray(IntArray(len) { dis.readInt() })
        }
        12 -> {
            val len = dis.readInt()
            NbtLongArray(LongArray(len) { dis.readLong() })
        }
        else -> error("Unknown NBT tag type: $typeId")
    }

sealed class NbtTag

object NbtEnd : NbtTag()

data class NbtByte(val value: Byte) : NbtTag()

data class NbtShort(val value: Short) : NbtTag()

data class NbtInt(val value: Int) : NbtTag()

data class NbtLong(val value: Long) : NbtTag()

data class NbtFloat(val value: Float) : NbtTag()

data class NbtDouble(val value: Double) : NbtTag()

data class NbtByteArray(val value: ByteArray) : NbtTag()

data class NbtString(val value: String) : NbtTag()

data class NbtList(val value: List<NbtTag>) : NbtTag()

data class NbtCompound(val value: Map<String, NbtTag>) : NbtTag() {
    operator fun get(key: String): NbtTag? = value[key]

    fun getIntArray(key: String): IntArray = (value[key] as? NbtIntArray)?.value ?: intArrayOf()

    fun getList(key: String): List<NbtTag> = (value[key] as? NbtList)?.value ?: emptyList()

    fun getString(key: String): String = (value[key] as? NbtString)?.value ?: ""
}

data class NbtIntArray(val value: IntArray) : NbtTag()

data class NbtLongArray(val value: LongArray) : NbtTag()

fun NbtTag.asInt(): Int =
    when (this) {
        is NbtInt -> value
        is NbtShort -> value.toInt()
        is NbtByte -> value.toInt()
        is NbtLong -> value.toInt()
        else -> 0
    }

paperPluginYaml {
    main.set("org.xodium.illyriaplus.IllyriaPlus")
    website.set("https://github.com/XodiumSoftware/IllyriaPlus")
    authors.add("Xodium")
    apiVersion.set(mcVersion)
    bootstrapper.set("org.xodium.illyriaplus.IllyriaPlusBootstrap")
}
