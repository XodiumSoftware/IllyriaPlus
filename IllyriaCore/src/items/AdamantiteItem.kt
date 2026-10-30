package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** An extremely durable dark-green metal prized for armor and tools. */
internal object AdamantiteItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.IRON_INGOT).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<green>$name</green>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>An extremely durable metal.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
        }
}
