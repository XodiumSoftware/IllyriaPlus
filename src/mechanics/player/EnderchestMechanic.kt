package org.xodium.illyriaplus.mechanics.player

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling ender chest access within the system. */
internal object EnderchestMechanic : MechanicInterface {
    @EventHandler(ignoreCancelled = true)
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
