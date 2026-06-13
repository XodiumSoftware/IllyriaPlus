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
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystems
import java.util.*
import java.util.concurrent.atomic.AtomicReference
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
                    val loaded = loadAllStructures()
                    trees.set(loaded)
                    val total = loaded.values.sumOf { it.size }
                    instance.logger.info("[TreeMechanic] Loaded $total custom tree structures.")
                    loaded.forEach { (type, list) ->
                        if (list.isNotEmpty()) {
                            instance.logger.info(
                                "[TreeMechanic]   ${type.name}: ${list.size} structure(s)",
                            )
                        }
                    }
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
        instance.logger.info(
            "[TreeMechanic] StructureGrowEvent fired for ${event.species} " +
                "at ${event.location.blockX}, ${event.location.blockY}, ${event.location.blockZ}",
        )
        val treeStruct =
            trees
                .get()[event.species]
                ?.takeIf { it.isNotEmpty() }
                ?.randomOrNull()
                ?: run {
                    instance.logger.info(
                        "[TreeMechanic] No custom structures found for ${event.species}; " +
                            "falling back to vanilla.",
                    )
                    return
                }

        event.isCancelled = true
        event.location.block.type = Material.AIR
        instance.logger.info("[TreeMechanic] Placing custom structure for ${event.species}")

        val placeLoc = event.location.clone().subtract(treeStruct.trunkOffset)
        instance.logger.info(
            "[TreeMechanic] Sapling loc: ${event.location.toVector()}, " +
                "trunkOffset: ${treeStruct.trunkOffset}, " +
                "placeLoc (structure origin): ${placeLoc.toVector()}",
        )

        treeStruct.structure.place(placeLoc, false, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, Random())
        instance.logger.info(
            "[TreeMechanic] Block at sapling location after placement: ${event.location.block.type}",
        )
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

        if (!rootPath.exists()) {
            instance.logger.info("[TreeMechanic] Folder not found for $type ($folderName)")
            return emptyList()
        }

        val files =
            rootPath
                .walk()
                .filter { it.isRegularFile() }
                .filter { it.toString().endsWith(".nbt") }
                .toList()
        instance.logger.info("[TreeMechanic] Found ${files.size} .nbt file(s) for $type ($folderName)")

        return files.mapNotNull { path ->
            val resourcePath = path.toString().removePrefix("/")
            val jsonResourcePath = resourcePath.replace(".nbt", ".json")
            val json = plugin.getResource(jsonResourcePath)?.bufferedReader()?.use { it.readText() }
            val offset = json?.let { parseManualOffset(it) }
            if (offset == null) {
                instance.logger.warning("[TreeMechanic] Skipping $resourcePath: missing or invalid $jsonResourcePath")
                return@mapNotNull null
            }
            instance.logger.info("[TreeMechanic] $resourcePath offset: $offset")
            plugin.getResource(resourcePath)?.use { input ->
                val structure = structureManager.loadStructure(ByteArrayInputStream(input.readBytes()))
                TreeStructure(structure, offset)
            }
        }.toList()
    }

    /**
     * Parses a simple JSON object of the form `{"x":1,"y":0,"z":2}` into a [Vector].
     *
     * Returns `null` if the input is not in the expected format.
     */
    private fun parseManualOffset(json: String): Vector? {
        val regex = """"([xyz])"\s*:\s*(-?\d+)""".toRegex()
        val matches = regex.findAll(json)
        val coords = mutableMapOf<String, Int>()
        matches.forEach { match ->
            val (key, value) = match.destructured
            coords[key] = value.toInt()
        }
        val x = coords["x"] ?: return null
        val y = coords["y"] ?: return null
        val z = coords["z"] ?: return null
        return Vector(x.toDouble(), y.toDouble(), z.toDouble())
    }

    /**
     * Loads all tree structures from the plugin jar.
     *
     * @return A map containing every [TreeType] and its associated [TreeStructure]s.
     */
    private fun loadAllStructures(): Map<TreeType, List<TreeStructure>> {
        instance.logger.info("[TreeMechanic] Loading tree structures from plugin jar...")
        return runCatching {
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
    }
}
