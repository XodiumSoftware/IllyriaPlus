package org.xodium.illyriaplus.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance

/** Represents a Greatsword. */
@Suppress("UnstableApiUsage")
internal object GreatswordItem : ItemInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            setData(DataComponentTypes.ITEM_MODEL, NamespacedKey(instance, "greatsword"))
            editPersistentDataContainer {
                it.set(NamespacedKey(instance, "greatsword"), PersistentDataType.INTEGER, 1)
            }
        }
}
