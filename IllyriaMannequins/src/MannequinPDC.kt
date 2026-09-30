package org.xodium.illyriamannequins

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Mannequin
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import java.util.UUID

/** Provides access to [Mannequin]-specific persistent data. */
@Suppress("Unused")
internal object MannequinPDC {
    /** The [NamespacedKey] used for storing the mannequin's combat mode. */
    private val COMBAT_MODE_KEY = NamespacedKey(instance, "combat_mode")

    /** The [NamespacedKey] used for storing whether the mannequin follows players. */
    private val FOLLOWING_KEY = NamespacedKey(instance, "following")

    /** The [NamespacedKey] used for storing the mannequin owner's UUID. */
    private val OWNER_KEY = NamespacedKey(instance, "owner")

    /**
     * Gets or sets the [Mannequin]'s combat mode.
     *
     * @return The [CombatMode], or [CombatMode.DEFENSIVE] if not set.
     */
    var Mannequin.combatMode: CombatMode
        get() =
            persistentDataContainer
                .get(COMBAT_MODE_KEY, PersistentDataType.STRING)
                ?.let { runCatching { CombatMode.valueOf(it) }.getOrNull() }
                ?: CombatMode.DEFENSIVE
        set(value) = persistentDataContainer.set(COMBAT_MODE_KEY, PersistentDataType.STRING, value.name)

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

/** The combat behavior mode of a mannequin. */
internal enum class CombatMode(
    color: String,
) {
    /** Attacks nearby monsters preemptively. */
    AGGRESSIVE("red"),

    /** Raises the shield and only fights back after being attacked. */
    DEFENSIVE("yellow"),

    /** Ignores monsters and keeps following the owner. */
    FLEEING("green"),
    ;

    /** The colored display name of the mode. */
    val display: Component = MM.deserialize("<$color>${name.lowercase().replaceFirstChar { it.uppercase() }}")
}
