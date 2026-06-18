package org.xodium.illyriaplus.items

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance

/** Represents a contract for reusable item builders within the system. */
internal interface ItemInterface {
    companion object {
        /** The [NamespacedKey] used to store alcoholic strength on item stacks. */
        val ALCOHOL_STRENGTH_KEY = NamespacedKey(instance, "alcohol_strength")
    }

    /** The alcoholic strength of this item. Zero for non-alcoholic items. */
    val alcoholStrength: Int get() = 0

    /** Builds and returns the configured [ItemStack]. */
    operator fun invoke(): ItemStack
}
