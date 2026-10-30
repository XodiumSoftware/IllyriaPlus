package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** A rare silvery metal ingot used in custom recipes and trades. */
internal object MithrilItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.IRON_INGOT).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<aqua>$name</aqua>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A rare silvery metal.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
        }
}
