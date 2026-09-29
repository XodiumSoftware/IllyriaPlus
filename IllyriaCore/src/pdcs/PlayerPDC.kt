package org.xodium.illyriacore.pdcs

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/** Provides access to [Player]-specific persistent data including nicknames. */
@Suppress("Unused")
internal object PlayerPDC {
    /** The [NamespacedKey] used for storing nickname data. */
    private val NICKNAME_KEY = NamespacedKey(instance, "nickname")

    /** The [NamespacedKey] used for storing the player's last Nether-side portal block location. */
    private val LAST_NETHER_PORTAL_KEY = NamespacedKey(instance, "last_nether_portal")

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

    /**
     * Gets or sets a [Player]'s last used Nether-side portal block location in their persistent
     * data container.
     *
     * The location is stored as a semicolon-separated string: `world;x;y;z` using block
     * coordinates. Setting the value to `null` removes the stored location.
     *
     * @return The [Player]'s last Nether portal block location, or `null` if none is stored or the
     * stored world no longer exists.
     */
    var Player.lastNetherPortal: Location?
        get() {
            val raw =
                persistentDataContainer.get(LAST_NETHER_PORTAL_KEY, PersistentDataType.STRING)
                    ?: return null
            val parts = raw.split(";")
            if (parts.size != 4) return null
            val world = Bukkit.getWorld(parts[0]) ?: return null
            return Location(
                world,
                (parts[1].toIntOrNull() ?: return null).toDouble(),
                (parts[2].toIntOrNull() ?: return null).toDouble(),
                (parts[3].toIntOrNull() ?: return null).toDouble(),
            )
        }
        set(value) {
            if (value == null) {
                persistentDataContainer.remove(LAST_NETHER_PORTAL_KEY)
            } else {
                persistentDataContainer.set(
                    LAST_NETHER_PORTAL_KEY,
                    PersistentDataType.STRING,
                    "${value.world.name};${value.blockX};${value.blockY};${value.blockZ}",
                )
            }
        }
}
