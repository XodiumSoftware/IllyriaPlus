package org.xodium.illyriaplus.mechanics.world

import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling cauldron interactions within the system. */
internal object CauldronMechanic : MechanicInterface {
    // private const val / private val settings here

    @EventHandler
    fun on(event: PlayerInteractEvent) {
        // implementation
    }
}
