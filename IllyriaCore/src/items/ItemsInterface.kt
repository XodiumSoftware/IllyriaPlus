package org.xodium.illyriacore.items

import org.bukkit.NamespacedKey
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.recipes.RecipeInterface
import org.xodium.illyrialib.Utils.snakeToProperCase
import kotlin.time.measureTime

/** Represents a contract for custom items within the plugin. */
internal interface ItemsInterface :
    Listener,
    RecipeInterface {
    /** The unique [NamespacedKey] identifying this custom item. */
    val key: NamespacedKey
        get() = NamespacedKey(instance, id)

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
     * Checks whether the given [item] was created by this custom item.
     *
     * @param item The item stack to check.
     * @return `true` if the item carries this custom item's PDC marker.
     */
    fun isItem(item: ItemStack): Boolean =
        item.persistentDataContainer.getOrDefault(key, PersistentDataType.BOOLEAN, false)

    /**
     * Builds the custom [ItemStack].
     *
     * @return A fresh custom [ItemStack].
     */
    operator fun invoke(): ItemStack

    /**
     * Registers this feature with the server, including any event listeners and recipes.
     *
     * @return The time taken to register the feature in milliseconds.
     */
    override fun register(): Long =
        measureTime {
            instance.server.pluginManager.registerEvents(this, instance)
            recipes.forEach { instance.server.addRecipe(it) }
        }.inWholeMilliseconds
}
