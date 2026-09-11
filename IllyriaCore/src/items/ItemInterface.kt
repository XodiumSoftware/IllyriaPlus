package org.xodium.illyriaplus.items

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

/** Represents a contract for reusable item builders within the system. */
internal interface ItemInterface {
    /** The [NamespacedKey] identifying this item type. Used for item models and PDC lookups. */
    val key: NamespacedKey

    /** Builds and returns the configured [ItemStack]. */
    operator fun invoke(): ItemStack
}
