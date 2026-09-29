package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM

/** Represents the Nullscape Key item. */
internal object NullscapeKeyItem : ItemInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.TRIAL_KEY).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#4B0082:#8A2BE2:#DA70D6>Nullscape Key"))
            setData(DataComponentTypes.ITEM_MODEL, key)
        }
}
