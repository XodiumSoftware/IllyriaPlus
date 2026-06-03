package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.TreeType
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.event.EventHandler
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.structure.Structure
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
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

/** Represents a mechanic handling trees within the system. */
internal object TreeMechanic : MechanicInterface {
    private val trees = AtomicReference<Map<TreeType, List<Structure>>>(emptyMap())

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
                instance.server.scheduler.runTaskAsynchronously(instance) { _ -> trees.set(loadAllStructures()) }
            }.inWholeMilliseconds

    @EventHandler
    fun on(event: StructureGrowEvent) = handleStructureGrowth(event)

    /**
     * Handles structure growth events by cancelling vanilla growth and placing a custom structure.
     *
     * @param event The [StructureGrowEvent] to handle.
     */
    private fun handleStructureGrowth(event: StructureGrowEvent) {
        val structure =
            trees
                .get()[event.species]
                ?.takeIf { it.isNotEmpty() }
                ?.randomOrNull()
                ?: return

        event.isCancelled = true
        placeStructure(structure, event.location.clone().apply { block.type = Material.AIR })
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
     * @param type The [TreeType] to load structures for.
     * @param fs The jar [FileSystem] containing the plugin resources.
     * @return A list of loaded [Structure]s; empty if the folder does not exist or is unmapped.
     */
    private fun loadStructures(
        type: TreeType,
        fs: FileSystem,
    ): List<Structure> {
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
                plugin.getResource(path.toString().removePrefix("/"))?.use { structureManager.loadStructure(it) }
            }.toList()
    }

    /**
     * Loads all tree structures from the plugin jar.
     *
     * @return A map containing every [TreeType] and its associated structures.
     */
    private fun loadAllStructures(): Map<TreeType, List<Structure>> =
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

    /**
     * Places a [Structure] at the given [Location].
     *
     * @param structure The [Structure] to place.
     * @param location The [Location] to place the structure at.
     */
    private fun placeStructure(
        structure: Structure,
        location: Location,
    ) {
        structure.place(
            location,
            false,
            StructureRotation.entries.random(),
            Mirror.entries.random(),
            0,
            1.0f,
            Random(),
        )
    }
}
