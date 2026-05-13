package org.xodium.illyriaplus.pdcs

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance

/** Provides access to item-specific persistent data for spell storage on wands. */
@Suppress("Unused")
internal object ItemStackPDC {
    /** The key used for storing the currently selected spell ID on a wand. */
    private val SELECTED_SPELL_KEY = NamespacedKey(instance, "selected_spell")

    /**
     * Gets or sets the selected spell ID on an item.
     *
     * @return The spell ID string, or empty string if none selected.
     */
    var ItemStack.selectedSpell: String
        get() = persistentDataContainer.get(SELECTED_SPELL_KEY, PersistentDataType.STRING) ?: ""
        set(value) {
            editPersistentDataContainer { it.set(SELECTED_SPELL_KEY, PersistentDataType.STRING, value) }
        }
}
