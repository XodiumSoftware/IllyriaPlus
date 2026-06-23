package org.xodium.illyriaplus.pdcs

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance

/** Provides access to [Player]-specific persistent data including nicknames and scoreboard preferences. */
@Suppress("Unused")
internal object PlayerPDC {
    /** The [NamespacedKey] used for storing nickname data. */
    private val NICKNAME_KEY = NamespacedKey(instance, "nickname")

    /** The [NamespacedKey] used for storing scoreboard visibility preferences. */
    private val SCOREBOARD_VISIBILITY_KEY = NamespacedKey(instance, "scoreboard_visibility")

    /** The [NamespacedKey] used for storing intoxication level. */
    private val INTOXICATION_KEY = NamespacedKey(instance, "intoxication")

    /** The [NamespacedKey] used for storing the last drink timestamp. */
    private val LAST_DRINK_TIME_KEY = NamespacedKey(instance, "last_drink_time")

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
     * Gets or sets a [Player]'s scoreboard visibility preference in their persistent data container.
     *
     * @return `true` if the scoreboard is visible, `false` otherwise.
     */
    var Player.scoreboardVisibility: Boolean
        get() = persistentDataContainer.getOrDefault(SCOREBOARD_VISIBILITY_KEY, PersistentDataType.BOOLEAN, false)
        set(value) = persistentDataContainer.set(SCOREBOARD_VISIBILITY_KEY, PersistentDataType.BOOLEAN, value)

    /**
     * Gets or sets a [Player]'s intoxication level in their persistent data container.
     *
     * @return The intoxication level, or 0 if no value is set.
     */
    var Player.intoxication: Int
        get() = persistentDataContainer.getOrDefault(INTOXICATION_KEY, PersistentDataType.INTEGER, 0)
        set(value) = persistentDataContainer.set(INTOXICATION_KEY, PersistentDataType.INTEGER, value.coerceAtLeast(0))

    /**
     * Gets or sets the timestamp of a [Player]'s last alcoholic drink in their persistent data container.
     *
     * @return The epoch millis of the last drink, or 0 if no drink has been recorded.
     */
    var Player.lastDrinkTime: Long
        get() = persistentDataContainer.getOrDefault(LAST_DRINK_TIME_KEY, PersistentDataType.LONG, 0L)
        set(value) = persistentDataContainer.set(LAST_DRINK_TIME_KEY, PersistentDataType.LONG, value)
}
