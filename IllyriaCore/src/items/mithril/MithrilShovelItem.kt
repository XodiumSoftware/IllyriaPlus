package org.xodium.illyriacore.items.mithril

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A light mithril shovel that turns earth like water. */
internal object MithrilShovelItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SHOVEL).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#C0C0C0:#E0FFFF>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A light spade that scoops soil without effort.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
