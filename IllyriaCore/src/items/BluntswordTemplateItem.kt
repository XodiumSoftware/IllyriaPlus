package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyrialib.Utils.MM

/** Smithing template used to forge a Bluntsword. */
internal object BluntswordTemplateItem : ItemsInterface {
    @EventHandler
    fun on(event: EntityDeathEvent) {
        // Stub: define drop source and chance for the Bluntsword template.
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE).apply {
            setData(
                DataComponentTypes.CUSTOM_NAME,
                MM.deserialize("Bluntsword Upgrade Smithing Template"),
            )
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("Used at a smithing table to upgrade a weapon into a Bluntsword."),
                    ),
                ),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        }
}
