package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.StonecutterInventory
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.mechanics.MechanicInterface

/**
 * Represents a mechanic that applies armor skins via the stonecutter while preserving
 * all original item data such as durability, enchantments, and trims.
 */
@Suppress("UnstableApiUsage")
internal object ArmorSkinMechanic : MechanicInterface {
    /** Armor slot tags from Paper mapped to their [EquipmentSlot]. */
    private val ARMOR_SLOTS =
        mapOf(
            Tag.ITEMS_HEAD_ARMOR to EquipmentSlot.HEAD,
            Tag.ITEMS_CHEST_ARMOR to EquipmentSlot.CHEST,
            Tag.ITEMS_LEG_ARMOR to EquipmentSlot.LEGS,
            Tag.ITEMS_FOOT_ARMOR to EquipmentSlot.FEET,
        )

    /** Available cosmetic skin sets. */
    private val SKINS =
        setOf(
            "knight",
        )

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun on(event: InventoryClickEvent) {
        if (event.inventory !is StonecutterInventory) return
        if (event.slotType != InventoryType.SlotType.RESULT) return
        if (event.currentItem == null) return

        val player = event.whoClicked as? Player ?: return
        val stonecutter = event.inventory as StonecutterInventory
        val input = stonecutter.inputItem ?: return
        val output = event.currentItem ?: return

        val slot = ARMOR_SLOTS.entries.find { it.key.isTagged(input.type) }?.value ?: return
        val skin = extractSkinKey(output) ?: return

        event.isCancelled = true

        val result =
            input.clone().apply {
                amount = output.amount
                setData(
                    DataComponentTypes.ITEM_MODEL,
                    NamespacedKey(instance, skin),
                )
                setData(
                    DataComponentTypes.CUSTOM_MODEL_DATA,
                    CustomModelData.customModelData().addString(slot.name.lowercase()).build(),
                )
            }

        val newInput = input.clone().apply { amount -= output.amount }
        stonecutter.setInputItem(newInput.takeIf { it.amount > 0 } ?: ItemStack.of(Material.AIR))

        player.setItemOnCursor(result)
        player.updateInventory()
    }

    /** Extracts the skin identifier from a recipe output by reading its [DataComponentTypes.ITEM_MODEL]. */
    private fun extractSkinKey(item: ItemStack): String? {
        val model = item.getData(DataComponentTypes.ITEM_MODEL) ?: return null
        return model.value().takeIf { it in SKINS }
    }
}
