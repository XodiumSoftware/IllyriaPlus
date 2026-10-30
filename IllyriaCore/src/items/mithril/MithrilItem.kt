package org.xodium.illyriacore.items.mithril

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A rare silvery metal ingot used in custom recipes and trades. */
internal object MithrilItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_INGOT).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#C0C0C0:#E0FFFF>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A rare silvery metal.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
