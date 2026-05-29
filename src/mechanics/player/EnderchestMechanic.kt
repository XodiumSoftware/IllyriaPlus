package org.xodium.illyriaplus.mechanics.player

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling ender chest access within the system. */
internal object EnderchestMechanic : MechanicInterface {
    override val faqCategory = FaqCategory.PLAYER

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ENDER_CHEST)
                .setName(MM.deserialize("<mango>Enderchest Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Portable Access</yellow> <firewatch>></gradient> " +
                            "<white>Right-click air with ender chest</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: PlayerInteractEvent) = openEnderchest(event)

    /**
     * Opens the player's ender chest when right-clicking air with an ender chest item.
     *
     * @param event The PlayerInteractEvent triggered by the player.
     */
    private fun openEnderchest(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR) return
        if (event.item?.type != Material.ENDER_CHEST) return
        if (event.player.gameMode != GameMode.SURVIVAL) return

        event.isCancelled = true
        instance.server.scheduler.runTask(
            instance,
            Runnable { event.player.openInventory(event.player.enderChest) },
        )
    }
}
