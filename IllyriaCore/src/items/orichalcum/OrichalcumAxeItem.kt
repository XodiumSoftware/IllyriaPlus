package org.xodium.illyriacore.items.orichalcum

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** An ornate orichalcum axe etched with Atlantean glyphs. */
internal object OrichalcumAxeItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_AXE).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>An ornate axe warm with sunken sunlight.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
