package org.xodium.illyriacore.items

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.snakeToProperCase

/** Represents a contract for custom items within the system. */
internal interface ItemsInterface {
    /** The unique [NamespacedKey] identifying this custom item. */
    val key: NamespacedKey
        get() = NamespacedKey(IllyriaCore.instance, id)

    /** The registry key fragment (snake_case) identifying this custom item. */
    val id: String
        get() =
            javaClass
                .simpleName
                .removeSuffix("Item")
                .split(Regex("(?=[A-Z])"))
                .filter { it.isNotEmpty() }
                .joinToString("_") { it.lowercase() }

    /** The display name derived from [id], converted to proper case. */
    val name: String
        get() = id.snakeToProperCase()

    /**
     * Builds the custom [ItemStack].
     *
     * @param amount The stack size. Defaults to 1.
     * @return A fresh custom [ItemStack].
     */
    fun item(amount: Int = 1): ItemStack
}
