package org.xodium.illyriaplus.mechanics.player

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling XP conversion within the system. */
internal object XpMechanic : MechanicInterface {
    private const val XP_COST_TO_BOTTLE: Int = 11

    override val faqTab = FaqTab.PLAYER

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(MM.deserialize("<mango>XP Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Bottle XP</yellow> <firewatch>></gradient> <white>Sneak + " +
                            "right-click enchanting table with bottle</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: PlayerInteractEvent) = xpToBottle(event)

    /**
     * Handles the interaction event where a player can convert their experience points into an experience bottle
     * if specific conditions are met.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with the world or an object.
     */
    private fun xpToBottle(event: PlayerInteractEvent) {
        if (event.clickedBlock?.type != Material.ENCHANTING_TABLE ||
            event.item?.type != Material.GLASS_BOTTLE ||
            !event.player.isSneaking
        ) {
            return
        }

        val player = event.player

        if (player.calculateTotalExperiencePoints() < XP_COST_TO_BOTTLE) return

        player.giveExp(-XP_COST_TO_BOTTLE)
        event.item?.subtract(1)
        player.inventory
            .addItem(ItemStack.of(Material.EXPERIENCE_BOTTLE, 1))
            .values
            .forEach { player.world.dropItemNaturally(player.location, it) }
    }
}
