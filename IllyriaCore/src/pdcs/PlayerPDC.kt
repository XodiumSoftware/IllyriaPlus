package org.xodium.illyriacore.pdcs

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Provides access to [Player]-specific persistent data including nicknames. */
@Suppress("Unused")
internal object PlayerPDC {
    /** The [NamespacedKey] used for storing nickname data. */
    private val NICKNAME_KEY = NamespacedKey(instance, "nickname")

    /** The [NamespacedKey] used for storing bending type data. */
    private val BENDING_TYPE_KEY = NamespacedKey(instance, "bending_type")

    /** The [NamespacedKey] used for storing bending level data. */
    private val BENDING_LEVEL_KEY = NamespacedKey(instance, "bending_level")

    /** The default bending level when none is stored. */
    private const val DEFAULT_BENDING_LEVEL = 1

    /** The minimum allowed bending level. */
    const val MIN_BENDING_LEVEL = 1

    /** The maximum allowed bending level. */
    const val MAX_BENDING_LEVEL = 5

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
     * Gets or sets a [Player]'s bending type in their persistent data container.
     *
     * @return The [Player]'s [BendingTypeEnum], or `null` if none is set.
     */
    var Player.bendingType: BendingTypeEnum?
        get() =
            persistentDataContainer
                .get(BENDING_TYPE_KEY, PersistentDataType.STRING)
                ?.let { runCatching { BendingTypeEnum.valueOf(it) }.getOrNull() }
        set(value) {
            if (value == null) {
                persistentDataContainer.remove(BENDING_TYPE_KEY)
            } else {
                persistentDataContainer.set(BENDING_TYPE_KEY, PersistentDataType.STRING, value.name)
            }
        }

    /**
     * Whether the [Player] has a bending type set in their persistent data container.
     *
     * @return `true` if the [Player] has chosen a [BendingTypeEnum], `false` otherwise.
     */
    val Player.hasBendingType: Boolean get() = bendingType != null

    /**
     * Gets or sets a [Player]'s bending level in their persistent data container. Values are
     * clamped to `[MIN_BENDING_LEVEL, MAX_BENDING_LEVEL]` on write.
     *
     * @return The [Player]'s bending level, or [DEFAULT_BENDING_LEVEL] if none is set.
     */
    var Player.bendingLevel: Int
        get() =
            persistentDataContainer.getOrDefault(
                BENDING_LEVEL_KEY,
                PersistentDataType.INTEGER,
                DEFAULT_BENDING_LEVEL,
            )
        set(value) {
            persistentDataContainer.set(
                BENDING_LEVEL_KEY,
                PersistentDataType.INTEGER,
                value.coerceIn(MIN_BENDING_LEVEL, MAX_BENDING_LEVEL),
            )
        }
}
