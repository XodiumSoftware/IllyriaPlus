package org.xodium.illyriaplus.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM

/** Represents a contract for custom items within the system. */
@Suppress("UnstableApiUsage")
internal interface ItemInterface {
    /** The unique identifier key for this item in persistent data. */
    val key: NamespacedKey get() = NamespacedKey(instance, this::class.java.simpleName.lowercase())

    /** The base material used for this item. */
    val material: Material get() = Material.STICK

    /** The display name of this item in MiniMessage format. */
    val title: String get() = this::class.java.simpleName.toString()

    /** The lore lines of this item in MiniMessage format. */
    val lore: List<String> get() = emptyList()

    /** Creates a new item stack representing this custom item. */
    val item: ItemStack
        get() =
            ItemStack.of(material).apply {
                editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
                setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize(title))
                setData(DataComponentTypes.LORE, ItemLore.lore(this@ItemInterface.lore.map { MM.deserialize(it) }))
            }

    /**
     * Checks if the given item matches this custom item.
     *
     * @param item The item to check.
     * @return True if the item is this custom item, false otherwise.
     */
    fun isItem(item: ItemStack): Boolean = item.persistentDataContainer.get(key, PersistentDataType.BOOLEAN) == true
}
