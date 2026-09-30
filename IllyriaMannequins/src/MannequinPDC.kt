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
