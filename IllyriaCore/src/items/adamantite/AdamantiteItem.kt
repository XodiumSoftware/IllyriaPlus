package org.xodium.illyriacore.items.adamantite

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** An extremely durable dark-green metal prized for armor and tools. */
internal object AdamantiteItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_INGOT).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#006400:#000000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>An extremely durable metal.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
