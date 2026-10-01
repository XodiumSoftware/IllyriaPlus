package org.xodium.illyriamannequins

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Mannequin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.EquipmentSlot

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

    /**
     * Checks if a material is a valid armor type for an equipment slot.
     *
     * @param material The material to check.
     * @param slot The equipment slot to check against.
     * @return `true` if the material is armor valid for the slot.
     */
    internal fun isValidForSlot(
        material: Material,
        slot: EquipmentSlot,
    ): Boolean =
        when (slot) {
            EquipmentSlot.FEET -> Tag.ITEMS_FOOT_ARMOR.isTagged(material)
            EquipmentSlot.LEGS -> Tag.ITEMS_LEG_ARMOR.isTagged(material)
            EquipmentSlot.CHEST -> Tag.ITEMS_CHEST_ARMOR.isTagged(material)
            EquipmentSlot.HEAD -> Tag.ITEMS_HEAD_ARMOR.isTagged(material) || material.isBlock
            else -> false
        }
}
