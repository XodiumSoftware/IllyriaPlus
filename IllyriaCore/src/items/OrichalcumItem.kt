package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** A legendary reddish-gold alloy said to channel ancient power. */
internal object OrichalcumItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.GOLD_INGOT).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<red>$name</red>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A legendary reddish-gold alloy.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
        }
}
