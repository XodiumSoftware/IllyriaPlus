package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM

/** Represents the Incendium Key item. */
internal object IncendiumKeyItem : ItemInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.TRIAL_KEY).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#8B0000:#FF4500:#FF6347>Incendium Key"))
            setData(DataComponentTypes.ITEM_MODEL, key)
        }
}
