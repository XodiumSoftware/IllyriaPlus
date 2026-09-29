package org.xodium.illyriacore.items

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.toRegistryKeyFragment

/** Represents a contract for an item within the system. */
internal interface ItemInterface {
    /** The unique key identifying this item. */
    val key: NamespacedKey
        get() = NamespacedKey(instance, javaClass.toRegistryKeyFragment<ItemInterface>())

    /**
     * Creates an [ItemStack] for this item.
     *
     * @return The item stack.
     */
    operator fun invoke(): ItemStack
}
