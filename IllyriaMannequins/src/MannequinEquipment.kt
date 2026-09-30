package org.xodium.illyriamannequins

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Mannequin
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector

/** Handles swapping items between a player and a mannequin's equipment slots. */
@Suppress("UnstableApiUsage")
internal object MannequinEquipment {
    /** The height in blocks of the boots slot on a mannequin. */
    private const val BOOTS_HEIGHT = 0.5

    /** The height in blocks of the leggings slot on a mannequin. */
    private const val LEGGINGS_HEIGHT = 1.0

    /** The height in blocks of the chestplate slot on a mannequin. */
    private const val CHESTPLATE_HEIGHT = 1.45

    /** The lateral offset beyond which a click targets an arm instead of the body. */
    private const val ARM_OFFSET = 0.25

    /** The minimum height in blocks at which a click can target an arm. */
    private const val ARM_MIN_HEIGHT = 0.4

    /** The maximum height in blocks at which a click can target an arm. */
    private const val ARM_MAX_HEIGHT = 1.45

    /**
     * Swaps the player's held item with the item in the mannequin slot matching the clicked body part.
     *
     * @param event The interact event.
     * @param mannequin The mannequin to swap with.
     * @return `true` if an item was equipped onto or taken from the mannequin.
     */
    fun swapItem(
        event: PlayerInteractAtEntityEvent,
        mannequin: Mannequin,
    ): Boolean {
        val item = event.player.inventory.getItem(event.hand)
        val slot = slotForClick(event.clickedPosition)
        val equipment = mannequin.equipment
        val previous = equipment.getItem(slot)
        if (item.isEmpty) {
            if (previous.isEmpty) return false
            equipment.setItem(slot, null)
            event.player.inventory.setItem(event.hand, previous)
            return true
        }
        if (slot.isArmor && !isValidForSlot(item.type, slot)) return false
        equipment.setItem(slot, item.asOne())
        if (previous.isEmpty) {
            item.amount--
        } else {
            event.player.inventory.setItem(event.hand, previous)
        }
        return true
    }

    /**
     * Maps a clicked position on the mannequin to an equipment slot.
     *
     * @param clicked The relative click position on the mannequin.
     * @return The matching [EquipmentSlot].
     */
    private fun slotForClick(clicked: Vector): EquipmentSlot {
        if (clicked.y in ARM_MIN_HEIGHT..ARM_MAX_HEIGHT) {
            if (clicked.x < -ARM_OFFSET) return EquipmentSlot.OFF_HAND
            if (clicked.x > ARM_OFFSET) return EquipmentSlot.HAND
        }
        return slotForHeight(clicked.y)
    }

    /**
     * Maps a clicked body height to an armor equipment slot.
     *
     * @param height The relative click height on the mannequin.
     * @return The matching [EquipmentSlot].
     */
    private fun slotForHeight(height: Double): EquipmentSlot =
        when {
            height < BOOTS_HEIGHT -> EquipmentSlot.FEET
            height < LEGGINGS_HEIGHT -> EquipmentSlot.LEGS
            height < CHESTPLATE_HEIGHT -> EquipmentSlot.CHEST
            else -> EquipmentSlot.HEAD
        }

    /**
     * Checks if a material is a valid armor type for an equipment slot.
     *
     * @param material The material to check.
     * @param slot The equipment slot to check against.
     * @return `true` if the material is armor valid for the slot.
     */
    private fun isValidForSlot(
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
