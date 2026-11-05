package org.xodium.illyriamannequins

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.MannequinPDC.arrows
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

    /** GUI slots in display order, each bound to a backing inventory index. */
    private val SLOTS =
        listOf(
            armorSlot(EquipmentSlot.HEAD),
            armorSlot(EquipmentSlot.CHEST),
            armorSlot(EquipmentSlot.LEGS),
            armorSlot(EquipmentSlot.FEET),
            Slot(
                getter = { it.equipment.itemInMainHand },
                setter = { m, item -> m.equipment.setItem(EquipmentSlot.HAND, item) },
                validator = { true },
            ),
            Slot(
                getter = { it.equipment.itemInOffHand },
                setter = { m, item -> m.equipment.setItem(EquipmentSlot.OFF_HAND, item) },
                validator = { true },
            ),
            Slot(
                getter = { it.arrows },
                setter = { m, item -> m.arrows = item },
                validator = { Tag.ITEMS_ARROWS.isTagged(it.type) },
            ),
        )

    /**
     * Opens an equipment GUI for the given mannequin.
     *
     * The upper window shows seven slots (helmet, chestplate, leggings, boots, main hand,
     * off hand, arrows) backed by a [VirtualInventory] that live-syncs changes to [mannequin].
     * Armor slots reject non-armor; the arrow slot only accepts [Tag.ITEMS_ARROWS] items.
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
                    "H C L B . M O . A",
                ) {
                    'H' by SlotElement.InventoryLink(backing, 0)
                    'C' by SlotElement.InventoryLink(backing, 1)
                    'L' by SlotElement.InventoryLink(backing, 2)
                    'B' by SlotElement.InventoryLink(backing, 3)
                    'M' by SlotElement.InventoryLink(backing, 4)
                    'O' by SlotElement.InventoryLink(backing, 5)
                    'A' by SlotElement.InventoryLink(backing, 6)
                    '.' by ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
        }.open()
    }

    /**
     * Builds a [VirtualInventory] pre-populated with the mannequin's current equipment,
     * wired to validate each slot and live-sync changes back to the mannequin.
     *
     * @param mannequin The mannequin whose equipment is mirrored.
     * @return A [VirtualInventory] bound to the mannequin's equipment.
     */
    private fun backingInventory(mannequin: Mannequin): VirtualInventory {
        val initial = SLOTS.map { it.getter(mannequin) }.toTypedArray()
        return VirtualInventory(initial).apply {
            addPreUpdateHandler { event ->
                val newItem: ItemStack? = event.newItem
                if (newItem == null || newItem.isEmpty) return@addPreUpdateHandler
                val slot = SLOTS.getOrNull(event.slot) ?: return@addPreUpdateHandler
                if (!slot.validator(newItem)) {
                    event.isCancelled = true
                }
            }
            addPostUpdateHandler { event ->
                val slot = SLOTS.getOrNull(event.slot) ?: return@addPostUpdateHandler
                val item = event.newItem?.takeUnless { it.isEmpty }
                slot.setter(mannequin, item)
            }
        }
    }

    /**
     * Builds a [Slot] backed by an armor [EquipmentSlot], rejecting items not valid for that slot.
     *
     * @param slot The armor equipment slot to bind to.
     * @return A [Slot] reading and writing that [EquipmentSlot].
     */
    private fun armorSlot(slot: EquipmentSlot) =
        Slot(
            getter = { it.equipment.getItem(slot) },
            setter = { m, item -> m.equipment.setItem(slot, item) },
            validator = { item -> isValidArmor(item.type, slot) },
        )

    /**
     * Checks if a material is a valid armor type for an armor equipment slot.
     *
     * @param material The material to check.
     * @param slot The equipment slot to check against.
     * @return `true` if the material is armor valid for the slot.
     */
    private fun isValidArmor(
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

    /**
     * A GUI slot: how to read it from a mannequin, write it back, and validate inserts.
     *
     * @property getter Reads the current item from a mannequin, or `null` if empty.
     * @property setter Writes a new item to the mannequin, `null` clears the slot.
     * @property validator Returns `true` if the given item may enter the slot.
     */
    private data class Slot(
        val getter: (Mannequin) -> ItemStack?,
        val setter: (Mannequin, ItemStack?) -> Unit,
        val validator: (ItemStack) -> Boolean,
    )
}
