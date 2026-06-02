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
import java.util.*

/** Represents a mechanic handling trees within the system. */
internal object TreeMechanic : MechanicInterface {
    private val TREES: Map<TreeType, Structure?> = TreeType.entries.associateWith { loadStructure(it) }

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
        val structure = TREES[event.species] ?: return

        event.isCancelled = true
        event.location.block.type = Material.AIR
        placeStructure(structure, event.location)
    }

    /**
     * Loads a [Structure] from the plugin's jar resources.
     *
     * @param type The [TreeType] to load a structure for.
     * @return The loaded [Structure], or null if the resource does not exist.
     */
    private fun loadStructure(type: TreeType): Structure? =
        runCatching {
            IllyriaPlus.instance.getResource("structures/${type.name.lowercase()}.nbt")?.use {
                IllyriaPlus.instance.server.structureManager
                    .loadStructure(it)
            }
        }.getOrNull()

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
