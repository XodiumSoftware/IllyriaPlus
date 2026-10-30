package org.xodium.illyriacore.items.adamantite

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A dense adamantite shovel that sculpts the earth like clay. */
internal object AdamantiteShovelItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SHOVEL).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#006400:#000000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A spade that carves stone as if it were sand.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
