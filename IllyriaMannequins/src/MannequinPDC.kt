package org.xodium.illyriamannequins

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Mannequin
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import java.util.UUID

/** Provides access to [Mannequin]-specific persistent data. */
@Suppress("Unused")
internal object MannequinPDC {
    /** The [NamespacedKey] used for storing the mannequin's anchor location. */
    private val ANCHOR_KEY = NamespacedKey(instance, "anchor")

    /** The [NamespacedKey] used for storing the mannequin's combat mode. */
    private val COMBAT_MODE_KEY = NamespacedKey(instance, "combat_mode")

    /** The [NamespacedKey] used for storing the mannequin's movement mode. */
    private val MOVEMENT_MODE_KEY = NamespacedKey(instance, "movement_mode")

    /** The [NamespacedKey] used for storing the mannequin owner's UUID. */
    private val OWNER_KEY = NamespacedKey(instance, "owner")

    /**
     * Gets or sets the [Mannequin]'s anchor location for [MovementMode.STATIONARY].
     *
     * @return The anchor [Location], or `null` if not set.
     */
    var Mannequin.anchor: Location?
        get() =
            persistentDataContainer
                .get(ANCHOR_KEY, PersistentDataType.STRING)
                ?.let { deserializeLocation(it) }
        set(value) {
            if (value == null) {
                persistentDataContainer.remove(ANCHOR_KEY)
            } else {
                persistentDataContainer.set(ANCHOR_KEY, PersistentDataType.STRING, serializeLocation(value))
            }
        }

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
     * Gets or sets the [Mannequin]'s movement mode.
     *
     * @return The [MovementMode], or [MovementMode.FOLLOWING] if not set.
     */
    var Mannequin.movementMode: MovementMode
        get() =
            persistentDataContainer
                .get(MOVEMENT_MODE_KEY, PersistentDataType.STRING)
                ?.let { runCatching { MovementMode.valueOf(it) }.getOrNull() }
                ?: MovementMode.FOLLOWING
        set(value) = persistentDataContainer.set(MOVEMENT_MODE_KEY, PersistentDataType.STRING, value.name)

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

    /**
     * Serializes a location to a compact string.
     *
     * @param location The location to serialize.
     * @return A string in the format `world;x;y;z`.
     */
    private fun serializeLocation(location: Location): String =
        "${location.world.uid};${location.x};${location.y};${location.z}"

    /**
     * Deserializes a location from a compact string.
     *
     * @param data The serialized location string.
     * @return The [Location], or `null` if the data is malformed or the world is unloaded.
     */
    private fun deserializeLocation(data: String): Location? {
        val parts = data.split(";")
        if (parts.size != 4) return null
        val world =
            instance.server.getWorld(runCatching { UUID.fromString(parts[0]) }.getOrNull() ?: return null)
                ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        return Location(world, x, y, z)
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

/** The movement behavior mode of a mannequin. */
internal enum class MovementMode(
    color: String,
) {
    /** Follows the owner around. */
    FOLLOWING("blue"),

    /** Stays at its anchor location, returning to it after combat. */
    STATIONARY("gray"),
    ;

    /** The colored display name of the mode. */
    val display: Component = MM.deserialize("<$color>${name.lowercase().replaceFirstChar { it.uppercase() }}")
}
