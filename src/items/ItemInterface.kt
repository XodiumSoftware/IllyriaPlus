package org.xodium.illyriaplus.items

import org.bukkit.inventory.ItemStack

/** Represents a contract for reusable item builders within the system. */
internal interface ItemInterface {
    /** Builds and returns the configured [ItemStack]. */
    operator fun invoke(): ItemStack
}
