package org.xodium.illyriaplus.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM

/** Represents a Greatsword. */
@Suppress("UnstableApiUsage")
internal object GreatswordItem : ItemInterface {
    override val key: NamespacedKey = NamespacedKey(instance, "greatsword")

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            if (!hasData(DataComponentTypes.CUSTOM_NAME) && !hasData(DataComponentTypes.ITEM_NAME)) {
                setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("Netherite Greatsword"))
            }
            setData(DataComponentTypes.ITEM_MODEL, key)
            editPersistentDataContainer { it.set(key, PersistentDataType.INTEGER, 1) }
        }
}
