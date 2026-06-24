package org.xodium.illyriaplus.items.aura

import io.papermc.paper.event.player.PlayerArmSwingEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.xodium.illyriaplus.items.ItemInterface

/** Represents a contract for reusable aura item builders within the system. */
internal interface AuraItemInterface : ItemInterface {
    /**
     * Applies the aura effect to the given item.
     *
     * @param player The player to apply the aura to.
     */
    fun aura(player: Player)

    @EventHandler
    fun on(event: PlayerArmSwingEvent) {
        if (!isItem(event.player.inventory.itemInMainHand)) return

        aura(event.player)
    }
}
