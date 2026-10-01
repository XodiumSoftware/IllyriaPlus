package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

/** Drops a mannequin's equipment when it dies. */
@Suppress("UnstableApiUsage")
internal object MannequinDeathDrops : Listener {
    @EventHandler
    fun on(event: EntityDeathEvent) {
        val mannequin = event.entity as? Mannequin ?: return
        val equipment = mannequin.equipment
        event.drops.addAll(
            listOf(
                equipment.helmet,
                equipment.chestplate,
                equipment.leggings,
                equipment.boots,
                equipment.itemInMainHand,
                equipment.itemInOffHand,
            ).filterNot { it.isEmpty },
        )
    }
}
