package org.xodium.illyriacore.items.draconic

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A draconic helm crowned with a crystallized scale crest. */
internal object DraconicHelmetItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_HELMET).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#4B0082:#8B0000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A helm shaped from the crown of an elder wyrm.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
