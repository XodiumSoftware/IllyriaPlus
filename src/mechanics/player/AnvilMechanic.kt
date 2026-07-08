package org.xodium.illyriaplus.mechanics.player

import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling anvil customization within the system. */
internal object AnvilMechanic : MechanicInterface {
    @EventHandler
    fun on(event: PrepareAnvilEvent) {
        // TODO: implement custom anvil behavior
    }
}
