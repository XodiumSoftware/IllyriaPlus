package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockCanBuildEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling mushroom placement on logs within the system. */
internal object MushroomMechanic : MechanicInterface {
    private val MATERIALS: Set<Material> = setOf(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM)

    override val faqCategory: FaqCategory = FaqCategory.WORLD

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.RED_MUSHROOM)
                .setName(MM.deserialize("<mango>Mushroom Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Log Placement</yellow> <firewatch>></gradient> " +
                            "<white>Mushrooms can be placed on logs</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: BlockCanBuildEvent) {
        if (event.material !in MATERIALS) return
        if (Tag.LOGS.isTagged(event.block.getRelative(0, -1, 0).type)) event.isBuildable = true
    }
}
