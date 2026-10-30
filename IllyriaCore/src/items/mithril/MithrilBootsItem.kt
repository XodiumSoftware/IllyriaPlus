package org.xodium.illyriacore.items.mithril

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** Silent mithril boots that barely whisper against stone. */
internal object MithrilBootsItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_BOOTS).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#C0C0C0:#E0FFFF>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(MM.deserialize("<gray>Boots that carry their wearer like a winter breeze.</gray>")),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
