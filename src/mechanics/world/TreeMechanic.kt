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
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.io.File
import java.util.*
import java.util.jar.JarFile

/** Represents a mechanic handling trees within the system. */
internal object TreeMechanic : MechanicInterface {
    private val TREES: Map<TreeType, List<Structure>> = TreeType.entries.associateWith { loadStructures(it) }

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

    @EventHandler
    fun on(event: StructureGrowEvent) = handleStructureGrowth(event)

    /**
     * Handles structure growth events by cancelling vanilla growth and placing a custom structure.
     *
     * @param event The [StructureGrowEvent] to handle.
     */
    private fun handleStructureGrowth(event: StructureGrowEvent) {
        val structures = TREES[event.species] ?: return

        if (structures.isEmpty()) return

        event.isCancelled = true
        event.location.block.type = Material.AIR
        placeStructure(structures.random(), event.location)
    }

    /**
     * Loads every `.nbt` [Structure] found in the type's folder inside the plugin jar.
     *
     * @param type The [TreeType] to load structures for.
     * @return A list of loaded [Structure]s; empty if the folder does not exist.
     */
    private fun loadStructures(type: TreeType): List<Structure> =
        // TODO: we have to create our own mapping since the folder name does not match treetype.
        runCatching {
            val dir = "structures/trees/${type.name.lowercase()}/"
            val jar =
                File(
                    IllyriaPlus::class.java.protectionDomain.codeSource.location
                        .toURI(),
                )

            JarFile(jar).use { jarFile ->
                jarFile
                    .entries()
                    .asSequence()
                    .filter { !it.isDirectory && it.name.startsWith(dir) && it.name.endsWith(".nbt") }
                    .mapNotNull { entry ->
                        IllyriaPlus.instance.getResource(entry.name)?.use {
                            IllyriaPlus.instance.server.structureManager
                                .loadStructure(it)
                        }
                    }.toList()
            }
        }.getOrNull() ?: emptyList()

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
