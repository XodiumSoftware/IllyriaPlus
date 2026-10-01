package org.xodium.illyriamannequins

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.item.ItemBuilder

/** Inventory GUI for editing a mannequin's equipment. */
@OptIn(ExperimentalDslApi::class)
@Suppress("UnstableApiUsage")
internal object MannequinEquipmentUI {
    private const val TITLE = "<firewatch>Equipment</gradient>"

    /** GUI slot indices mapped to [EquipmentSlot]s in display order. */
    private val SLOT_ORDER =
        listOf(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.HAND,
            EquipmentSlot.OFF_HAND,
        )

    /**
     * Opens an equipment GUI for the given mannequin.
     *
     * The upper window shows six slots (helmet, chestplate, leggings, boots, main hand, off hand)
     * backed by a [VirtualInventory] that live-syncs changes to [mannequin]'s equipment.
     * Armor slots reject items not valid for that slot.
     *
     * @param player The player viewing the GUI, must be the mannequin's owner.
     * @param mannequin The mannequin whose equipment is being edited.
     */
    fun open(
        player: Player,
        mannequin: Mannequin,
    ) {
        val backing = backingInventory(mannequin)
        window(player) {
            title by MM.deserialize(TITLE)
            upperGui by
                gui(
                    "H C L B . M O . .",
                ) {
                    'H' by SlotElement.InventoryLink(backing, 0)
                    'C' by SlotElement.InventoryLink(backing, 1)
                    'L' by SlotElement.InventoryLink(backing, 2)
                    'B' by SlotElement.InventoryLink(backing, 3)
                    'M' by SlotElement.InventoryLink(backing, 4)
                    'O' by SlotElement.InventoryLink(backing, 5)
                    '.' by ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
        }.open()
    }

    /**
     * Builds a [VirtualInventory] pre-populated with the mannequin's current equipment,
     * wired to validate armor slots and live-sync changes back to the mannequin.
     *
     * @param mannequin The mannequin whose equipment is mirrored.
     * @return A [VirtualInventory] bound to the mannequin's equipment.
     */
    private fun backingInventory(mannequin: Mannequin): VirtualInventory {
        val initial =
            SLOT_ORDER
                .map { mannequin.equipment.getItem(it) }
                .toTypedArray()
        return VirtualInventory(initial).apply {
            addPreUpdateHandler { event ->
                val newItem: ItemStack? = event.newItem
                if (newItem != null && !newItem.isEmpty) {
                    val slot = SLOT_ORDER.getOrNull(event.slot)
                    if (slot != null && slot.isArmor && !isValidForSlot(newItem.type, slot)) {
                        event.isCancelled = true
                    }
                }
            }
            addPostUpdateHandler { event ->
                val slot = SLOT_ORDER.getOrNull(event.slot) ?: return@addPostUpdateHandler
                val item = event.newItem?.takeUnless { it.isEmpty }
                mannequin.equipment.setItem(slot, item)
            }
        }
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
