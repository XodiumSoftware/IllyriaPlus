package org.xodium.illyriamannequins

import org.bukkit.NamespacedKey
import org.bukkit.entity.Mannequin
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import java.util.UUID

/** Provides access to [Mannequin]-specific persistent data. */
@Suppress("Unused")
internal object MannequinPDC {
    /** The [NamespacedKey] used for storing the mannequin owner's UUID. */
    private val OWNER_KEY = NamespacedKey(instance, "owner")

    /** The [NamespacedKey] used for storing whether the mannequin follows players. */
    private val FOLLOWING_KEY = NamespacedKey(instance, "following")

    /**
     * Gets or sets whether the [Mannequin] follows nearby players.
     *
     * @return `true` if following, `false` otherwise.
     */
    var Mannequin.following: Boolean
        get() =
            persistentDataContainer
                .get(FOLLOWING_KEY, PersistentDataType.BOOLEAN)
                ?: false
        set(value) = persistentDataContainer.set(FOLLOWING_KEY, PersistentDataType.BOOLEAN, value)

    /**
     * Gets or sets the [Mannequin]'s owner UUID.
     *
     * @return The owner [UUID], or `null` if not set.
     */
    var Mannequin.owner: UUID?
        get() =
            persistentDataContainer
                .get(OWNER_KEY, PersistentDataType.STRING)
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        set(value) {
            if (value == null) {
                persistentDataContainer.remove(OWNER_KEY)
            } else {
                persistentDataContainer.set(OWNER_KEY, PersistentDataType.STRING, value.toString())
            }
        }
}
