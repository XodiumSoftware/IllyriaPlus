package org.xodium.illyriaplus.items

import org.bukkit.NamespacedKey
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import kotlin.time.measureTime

/** Represents a contract for reusable item builders within the system. */
internal interface ItemInterface : Listener {
    /** The [NamespacedKey] identifying this item type. Used for item models and PDC lookups. */
    val key: NamespacedKey

    /** Builds and returns the configured [ItemStack]. */
    operator fun invoke(): ItemStack

    /**
     * Registers this enchantment's event listeners with the plugin manager.
     * Should be called during plugin enable.
     *
     * @return Time taken to register in milliseconds.
     */
    fun register(): Long =
        measureTime { instance.server.pluginManager.registerEvents(this, instance) }.inWholeMilliseconds
}
