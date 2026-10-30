package org.xodium.illyriacore.items.adamantite

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** Plated adamantite boots that root the wearer like stone. */
internal object AdamantiteBootsItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_BOOTS).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#006400:#000000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>Boots that refuse to yield an inch of ground.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
