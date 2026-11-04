package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyrialib.Utils.MM

/** Smithing template used to forge a Halberd. */
internal object HalberdTemplateItem : ItemsInterface {
    private const val DROP_CHANCE = 0.015
    private const val LOOTING_BONUS = 0.005

    @EventHandler
    fun on(event: EntityDeathEvent) {
        if (event.entityType != EntityType.PIGLIN_BRUTE) return
        val killer = event.entity.killer ?: return
        val chance =
            DROP_CHANCE + (LOOTING_BONUS * killer.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.LOOTING))
        if (Math.random() >= chance) return
        event.drops.add(invoke())
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE).apply {
            setData(
                DataComponentTypes.CUSTOM_NAME,
                MM.deserialize("Halberd Upgrade Smithing Template"),
            )
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("Used at a smithing table to upgrade a weapon into a Halberd."),
                    ),
                ),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        }
}
