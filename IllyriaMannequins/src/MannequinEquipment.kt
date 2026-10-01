package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

/** Handles equipment-related concerns for mannequins. */
@Suppress("UnstableApiUsage")
internal object MannequinEquipment : Listener {
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
