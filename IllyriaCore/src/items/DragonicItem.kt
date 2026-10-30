package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** A crystalline scale fragment pulsing with draconic energy. */
internal object DragonicItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.ECHO_SHARD).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<dark_purple>$name</dark_purple>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A crystalline fragment of draconic power.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
        }
}
