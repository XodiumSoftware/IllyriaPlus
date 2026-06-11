package org.xodium.illyriaplus.mechanics.player

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.mechanics.server.TabListMechanic.tablist
import org.xodium.illyriaplus.pdcs.PlayerPDC.nickname

/** Represents a mechanic handling player nicknames within the system. */
internal object NicknameMechanic : MechanicInterface {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = handleJoin(event)

    /**
     * Applies the player's stored nickname on join.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun handleJoin(event: PlayerJoinEvent) {
        event.player.nickname()
    }

    /** Applies the player's stored nickname to their display name. */
    private fun Player.nickname() = displayName(MM.deserialize(nickname))

    /**
     * Sets the player's nickname to the given name, applies it, and sends a confirmation.
     *
     * @param name The new nickname. Blank or empty clears the nickname.
     */
    fun Player.nickname(name: String) {
        nickname = name
        nickname()
        playerListName(displayName())
        tablist(this)
    }
}
