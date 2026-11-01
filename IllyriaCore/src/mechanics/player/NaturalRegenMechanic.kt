package org.xodium.illyriacore.mechanics.player

import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.xodium.illyriacore.mechanics.MechanicInterface

/**
 * Disables vanilla hunger-based health regeneration so that healing must come from
 * flasks, bonfires, or other soulslike systems.
 */
internal object NaturalRegenMechanic : MechanicInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityRegainHealthEvent) {
        if (event.regainReason == EntityRegainHealthEvent.RegainReason.SATIATED) event.isCancelled = true
    }
}
