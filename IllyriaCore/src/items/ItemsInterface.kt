package org.xodium.illyriacore.items

import net.kyori.adventure.key.Key
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

    /** The model folder derived from this item's source package (e.g., `mithril`). */
    val modelPath: String
        get() = javaClass.packageName.substringAfterLast(".")

    /** The full [Key] used for the `ITEM_MODEL` data component. */
    val itemModelKey: Key
        get() = Key.key(IllyriaCore.ID, "$modelPath/$id")

    /**
     * Builds the custom [ItemStack].
     *
     * @return A fresh custom [ItemStack].
     */
    operator fun invoke(): ItemStack
}
