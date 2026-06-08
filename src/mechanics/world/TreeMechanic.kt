package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Material
import org.bukkit.TreeType
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.event.EventHandler
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.structure.Structure
import org.bukkit.util.Vector
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.mechanics.world.TreeMechanic.mirror
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystems
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk
import kotlin.time.measureTime

/** Wrapper holding a loaded structure and its pre-computed trunk centre offset. */
private data class TreeStructure(
    val structure: Structure,
    val trunkOffset: Vector,
)

/** Represents a mechanic handling trees within the system. */
internal object TreeMechanic : MechanicInterface {
    private val trees = AtomicReference<Map<TreeType, List<TreeStructure>>>(emptyMap())

    override val faqTab: FaqTab = FaqTab.WORLD_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.OAK_SAPLING)
                .setName(MM.deserialize("<mango>Tree Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Sapling Grows</yellow> <firewatch>></gradient> " +
                            "<white>Custom tree structures</white>",
                    ),
                ),
        )

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.scheduler.runTaskAsynchronously(instance) { _ ->
                    trees.set(loadAllStructures())
                }
            }.inWholeMilliseconds

    @EventHandler
    fun on(event: StructureGrowEvent) = handleStructureGrowth(event)

    /**
     * Handles structure growth events by cancelling vanilla growth and placing a custom structure.
     *
     * @param event The [StructureGrowEvent] to handle.
     */
    private fun handleStructureGrowth(event: StructureGrowEvent) {
        val treeStruct =
            trees
                .get()[event.species]
                ?.takeIf { it.isNotEmpty() }
                ?.randomOrNull()
                ?: return

        event.isCancelled = true
        event.location.block.type = Material.AIR

        val rotation = StructureRotation.entries.random()
        val mirror = Mirror.entries.random()
        val transformedOffset =
            treeStruct.trunkOffset
                .clone()
                .mirror(mirror)
                .rotate(rotation)
        val placeLoc = event.location.clone().subtract(transformedOffset)

        treeStruct.structure.place(placeLoc, false, rotation, mirror, 0, 1.0f, Random())
    }

    /**
     * Maps a Bukkit [TreeType] to the folder name used in `resources/structures/trees/`.
     *
     * Returns `null` if there is no custom structure pack for that type.
     */
    private fun TreeType.folderName(): String? =
        when (this) {
            TreeType.TREE, TreeType.BIG_TREE -> "oak"

            TreeType.REDWOOD, TreeType.TALL_REDWOOD -> "spruce"

            TreeType.BIRCH -> "birch"

            TreeType.JUNGLE, TreeType.SMALL_JUNGLE -> "jungle"

            TreeType.ACACIA -> "acacia"

            TreeType.DARK_OAK -> "dark_oak"

            TreeType.CHERRY -> "cherry"

            TreeType.MANGROVE -> "mangrove"

            TreeType.AZALEA -> "azalea"

            TreeType.CRIMSON_FUNGUS -> "crimson"

            TreeType.WARPED_FUNGUS -> "warped"

            TreeType.RED_MUSHROOM -> "red_mushroom"

            TreeType.BROWN_MUSHROOM -> "brown_mushroom"

            // TODO: Re-enable Pale Oak once the upstream Paper `StructureGrowEvent` bug is fixed.
            //        TreeType.PALE_OAK -> "pale_oak"
            else -> null
        }

    /**
     * Loads every `.nbt` [Structure] found in the mapped folder inside the plugin jar.
     *
     * The raw NBT is parsed to determine the trunk centre offset so no temporary world placement is required.
     *
     * @param type The [TreeType] to load structures for.
     * @param fs The jar [FileSystem] containing the plugin resources.
     * @return A list of [TreeStructure]s; empty if the folder does not exist or is unmapped.
     */
    private fun loadStructures(
        type: TreeType,
        fs: FileSystem,
    ): List<TreeStructure> {
        val folderName = type.folderName() ?: return emptyList()
        val plugin = instance
        val structureManager = plugin.server.structureManager
        val rootPath = fs.getPath("structures/trees/$folderName")

        if (!rootPath.exists()) return emptyList()

        return rootPath
            .walk()
            .filter { it.isRegularFile() }
            .filter { it.toString().endsWith(".nbt") }
            .mapNotNull { path ->
                val resourcePath = path.toString().removePrefix("/")
                plugin.getResource(resourcePath)?.use { input ->
                    val bytes = input.readBytes()
                    val structure =
                        ByteArrayInputStream(bytes).use {
                            structureManager.loadStructure(it)
                        }
                    val offset =
                        ByteArrayInputStream(bytes).use {
                            val root = NbtReader.readRoot(it)
                            computeOffsetFromNbt(root)
                        }
                    TreeStructure(structure, offset)
                }
            }.toList()
    }

    /**
     * Loads all tree structures from the plugin jar.
     *
     * @return A map containing every [TreeType] and its associated [TreeStructure]s.
     */
    private fun loadAllStructures(): Map<TreeType, List<TreeStructure>> =
        runCatching {
            val jarFileUri =
                IllyriaPlus::class.java.protectionDomain.codeSource.location
                    .toURI()
            val jarUri = URI.create("jar:$jarFileUri")
            val (fs, shouldClose) =
                try {
                    FileSystems.newFileSystem(jarUri, emptyMap<String, Any>()) to true
                } catch (_: FileSystemAlreadyExistsException) {
                    FileSystems.getFileSystem(jarUri) to false
                }

            try {
                TreeType.entries.associateWith { loadStructures(it, fs) }
            } finally {
                if (shouldClose) runCatching { fs.close() }
            }
        }.getOrDefault(emptyMap())

    // ------------------------------------------------------------------
    // NBT parsing
    // ------------------------------------------------------------------

    /**
     * Computes the average X/Z centre of all log/wood blocks in the structure's
     * bottom layer (relative Y == 0) by inspecting the raw NBT data.
     */
    private fun computeOffsetFromNbt(root: NbtCompound): Vector {
        val sizeArray = root.getIntArray("size")
        val sizeX = sizeArray.getOrNull(0) ?: 1
        val sizeZ = sizeArray.getOrNull(2) ?: 1

        val palette =
            root
                .getList("palette")
                .map { (it as NbtCompound).getString("Name") }

        val logPositions = mutableListOf<Pair<Int, Int>>()
        root.getList("blocks").forEach { blockTag ->
            val compound = blockTag as? NbtCompound ?: return@forEach
            val state = compound["state"]?.asInt() ?: return@forEach
            val pos = compound.getList("pos").mapNotNull { (it as? NbtInt)?.value }
            if (pos.size < 3) return@forEach

            val x = pos[0]
            val y = pos[1]
            val z = pos[2]
            if (y == 0 && isTrunkBlockId(palette.getOrNull(state))) {
                logPositions.add(x to z)
            }
        }

        return if (logPositions.isEmpty()) {
            // Fallback: centre of the structure footprint.
            Vector(sizeX / 2.0, 0.0, sizeZ / 2.0)
        } else {
            val avgX = logPositions.map { it.first }.average()
            val avgZ = logPositions.map { it.second }.average()
            Vector(avgX, 0.0, avgZ)
        }
    }

    /** Checks whether a block ID (e.g. `minecraft:oak_log`) represents a trunk block. */
    private fun isTrunkBlockId(id: String?): Boolean {
        if (id == null) return false
        val name = id.removePrefix("minecraft:")
        return name.endsWith("_log") ||
            name.endsWith("_wood") ||
            name.endsWith("_stem") ||
            name.endsWith("_hyphae") ||
            name == "mushroom_stem"
    }

    // ------------------------------------------------------------------
    // Minimal NBT reader
    // ------------------------------------------------------------------

    /** Reads the root compound from a (possibly GZIP-compressed) NBT stream. */
    private object NbtReader {
        fun readRoot(input: InputStream): NbtCompound {
            val buffered = input.buffered()
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

        private fun readNamedTag(dis: DataInputStream): Pair<String, NbtTag> {
            val typeId = dis.readUnsignedByte()
            if (typeId == 0) return "" to NbtEnd
            val nameLen = dis.readUnsignedShort()
            val name =
                if (nameLen > 0) ByteArray(nameLen).also { dis.readFully(it) }.toString(Charsets.UTF_8) else ""
            return name to readPayload(typeId, dis)
        }

        private fun readPayload(
            typeId: Int,
            dis: DataInputStream,
        ): NbtTag =
            when (typeId) {
                1 -> {
                    NbtByte(dis.readByte())
                }

                2 -> {
                    NbtShort(dis.readShort())
                }

                3 -> {
                    NbtInt(dis.readInt())
                }

                4 -> {
                    NbtLong(dis.readLong())
                }

                5 -> {
                    NbtFloat(dis.readFloat())
                }

                6 -> {
                    NbtDouble(dis.readDouble())
                }

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

                else -> {
                    error("Unknown NBT tag type: $typeId")
                }
            }
    }

    /** Sealed hierarchy for NBT tags parsed by [NbtReader]. */
    private sealed class NbtTag

    private object NbtEnd : NbtTag()

    private data class NbtByte(
        val value: Byte,
    ) : NbtTag()

    private data class NbtShort(
        val value: Short,
    ) : NbtTag()

    private data class NbtInt(
        val value: Int,
    ) : NbtTag()

    private data class NbtLong(
        val value: Long,
    ) : NbtTag()

    private data class NbtFloat(
        val value: Float,
    ) : NbtTag()

    private data class NbtDouble(
        val value: Double,
    ) : NbtTag()

    private data class NbtByteArray(
        val value: ByteArray,
    ) : NbtTag()

    private data class NbtString(
        val value: String,
    ) : NbtTag()

    private data class NbtList(
        val value: List<NbtTag>,
    ) : NbtTag()

    private data class NbtCompound(
        val value: Map<String, NbtTag>,
    ) : NbtTag() {
        operator fun get(key: String): NbtTag? = value[key]

        fun getIntArray(key: String): IntArray = (value[key] as? NbtIntArray)?.value ?: intArrayOf()

        fun getList(key: String): List<NbtTag> = (value[key] as? NbtList)?.value ?: emptyList()

        fun getString(key: String): String = (value[key] as? NbtString)?.value ?: ""
    }

    private data class NbtIntArray(
        val value: IntArray,
    ) : NbtTag()

    private data class NbtLongArray(
        val value: LongArray,
    ) : NbtTag()

    /** Converts any numeric NBT tag to an [Int]. */
    private fun NbtTag.asInt(): Int =
        when (this) {
            is NbtInt -> value
            is NbtShort -> value.toInt()
            is NbtByte -> value.toInt()
            is NbtLong -> value.toInt()
            else -> 0
        }

    // ------------------------------------------------------------------
    // Vector helpers for rotation / mirroring
    // ------------------------------------------------------------------

    /** Rotates this vector around the Y-axis by the given [rotation]. */
    private fun Vector.rotate(rotation: StructureRotation): Vector =
        when (rotation) {
            StructureRotation.NONE -> Vector(x, y, z)
            StructureRotation.CLOCKWISE_90 -> Vector(-z, y, x)
            StructureRotation.CLOCKWISE_180 -> Vector(-x, y, -z)
            StructureRotation.COUNTERCLOCKWISE_90 -> Vector(z, y, -x)
        }

    /** Mirrors this vector across the given [mirror] plane. */
    private fun Vector.mirror(mirror: Mirror): Vector =
        when (mirror) {
            Mirror.NONE -> Vector(x, y, z)
            Mirror.LEFT_RIGHT -> Vector(-x, y, z)
            Mirror.FRONT_BACK -> Vector(x, y, -z)
        }
}
