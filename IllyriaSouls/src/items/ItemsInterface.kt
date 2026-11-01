package org.xodium.illyriasouls.items

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.snakeToProperCase
import org.xodium.illyriasouls.IllyriaSouls

/** Represents a contract for custom items within IllyriaSouls. */
internal interface ItemsInterface {
    /** The unique [NamespacedKey] identifying this custom item. */
    val key: NamespacedKey
        get() = NamespacedKey(IllyriaSouls.instance, id)

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

    /** The model folder for this item (e.g., `flask`, `soul`). */
    val modelPath: String

    /** The full [Key] used for the `ITEM_MODEL` data component. */
    val itemModelKey: Key
        get() = Key.key(IllyriaSouls.ID, "$modelPath/$id")

    /**
     * Builds the custom [ItemStack].
     *
     * @return A fresh custom [ItemStack].
     */
    operator fun invoke(): ItemStack
}
