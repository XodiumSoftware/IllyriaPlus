package org.xodium.illyriacore.items.orichalcum

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A blessed orichalcum hoe that turns sand into garden soil. */
internal object OrichalcumHoeItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_HOE).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A blessed hoe that makes barren earth bloom.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
