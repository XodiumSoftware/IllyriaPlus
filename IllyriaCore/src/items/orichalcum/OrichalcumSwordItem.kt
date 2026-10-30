package org.xodium.illyriacore.items.orichalcum

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A blazing sword forged from the legendary orichalcum alloy. */
internal object OrichalcumSwordItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.GOLDEN_SWORD).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>An ancient blade that burns with hidden heat.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
