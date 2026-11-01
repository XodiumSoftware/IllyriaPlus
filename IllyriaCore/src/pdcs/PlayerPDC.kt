package org.xodium.illyriacore.pdcs

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/** Provides access to [Player]-specific persistent data including nicknames. */
@Suppress("Unused")
internal object PlayerPDC {
    /** The [NamespacedKey] used for storing nickname data. */
    private val NICKNAME_KEY = NamespacedKey(instance, "nickname")

    /**
     * Gets or sets a [Player]'s nickname in their persistent data container.
     *
     * @return The [Player]'s nickname, or their actual name if no nickname is set.
     */
    var Player.nickname: String
        get() = persistentDataContainer.getOrDefault(NICKNAME_KEY, PersistentDataType.STRING, name)
        set(value) {
            if (value.isBlank()) {
                persistentDataContainer.remove(NICKNAME_KEY)
            } else {
                persistentDataContainer.set(NICKNAME_KEY, PersistentDataType.STRING, value)
            }
        }
}
