package org.xodium.illyriacore.pdcs

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import kotlin.math.roundToInt

/** Provides access to [Player]-specific persistent data including nicknames and souls. */
@Suppress("Unused")
internal object PlayerPDC {
    /** Default starting flask charges for new players. */
    private const val FLASK_DEFAULT_CHARGES: Int = 5

    /** The [NamespacedKey] used for storing nickname data. */
    private val NICKNAME_KEY = NamespacedKey(instance, "nickname")

    /** The [NamespacedKey] used for storing the amount of souls held in the active death stain. */
    private val STAINED_SOULS_KEY = NamespacedKey(instance, "stained_souls")

    /** The [NamespacedKey] used for storing the serialized death stain location. */
    private val STAIN_LOCATION_KEY = NamespacedKey(instance, "stain_location")

    /** The [NamespacedKey] used for storing the world UID of the death stain. */
    private val STAIN_WORLD_KEY = NamespacedKey(instance, "stain_world")

    /** The [NamespacedKey] used for storing the number of remaining flask charges. */
    private val FLASK_CHARGES_KEY = NamespacedKey(instance, "flask_charges")

    /** The [NamespacedKey] used for storing the flask's maximum charges (upgraded over time). */
    private val FLASK_MAX_CHARGES_KEY = NamespacedKey(instance, "flask_max_charges")

    /** The [NamespacedKey] used for storing the flask's heal potency tier (upgraded over time). */
    private val FLASK_POTENCY_KEY = NamespacedKey(instance, "flask_potency")

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
     * Gets or sets the amount of souls currently held in this [Player]'s active death stain.
     *
     * @return The stained soul count, or `0` if no stain is active.
     */
    var Player.stainedSouls: Int
        get() = persistentDataContainer.getOrDefault(STAINED_SOULS_KEY, PersistentDataType.INTEGER, 0)
        set(value) {
            if (value <= 0) {
                persistentDataContainer.remove(STAINED_SOULS_KEY)
            } else {
                persistentDataContainer.set(STAINED_SOULS_KEY, PersistentDataType.INTEGER, value)
            }
        }

    /**
     * Gets or sets the death stain [Location] for this [Player].
     *
     * Stored as `x;y;z` coordinates and a world UID; returns `null` if any piece is missing.
     *
     * @return The stain location, or `null` if no stain is active.
     */
    var Player.stainLocation: Location?
        get() {
            val worldUid = persistentDataContainer.get(STAIN_WORLD_KEY, PersistentDataType.STRING) ?: return null
            val coords = persistentDataContainer.get(STAIN_LOCATION_KEY, PersistentDataType.STRING) ?: return null
            val (x, y, z) = coords.split(";").mapNotNull { it.toDoubleOrNull() }
            val world = instance.server.getWorld(worldUid) ?: return null

            return Location(world, x, y, z)
        }
        set(value) {
            if (value == null) {
                persistentDataContainer.remove(STAIN_LOCATION_KEY)
                persistentDataContainer.remove(STAIN_WORLD_KEY)
            } else {
                persistentDataContainer.set(
                    STAIN_LOCATION_KEY,
                    PersistentDataType.STRING,
                    "${(value.x * 100).roundToInt() / 100.0};" +
                        "${(value.y * 100).roundToInt() / 100.0};" +
                        "${(value.z * 100).roundToInt() / 100.0}",
                )
                persistentDataContainer.set(STAIN_WORLD_KEY, PersistentDataType.STRING, value.world.uid.toString())
            }
        }

    /** The current number of remaining flask charges for this [Player]. */
    var Player.flaskCharges: Int
        get() =
            persistentDataContainer.getOrDefault(
                FLASK_CHARGES_KEY,
                PersistentDataType.INTEGER,
                FLASK_DEFAULT_CHARGES,
            )
        set(value) {
            persistentDataContainer.set(
                FLASK_CHARGES_KEY,
                PersistentDataType.INTEGER,
                value.coerceIn(0, flaskMaxCharges),
            )
        }

    /** The maximum flask charges for this [Player] (upgradeable). */
    var Player.flaskMaxCharges: Int
        get() =
            persistentDataContainer.getOrDefault(
                FLASK_MAX_CHARGES_KEY,
                PersistentDataType.INTEGER,
                FLASK_DEFAULT_CHARGES,
            )
        set(value) {
            persistentDataContainer.set(FLASK_MAX_CHARGES_KEY, PersistentDataType.INTEGER, value.coerceAtLeast(1))
            flaskCharges = flaskCharges.coerceAtMost(value)
        }

    /** The flask heal potency tier for this [Player] (upgradeable). */
    var Player.flaskPotency: Int
        get() = persistentDataContainer.getOrDefault(FLASK_POTENCY_KEY, PersistentDataType.INTEGER, 0)
        set(value) {
            persistentDataContainer.set(FLASK_POTENCY_KEY, PersistentDataType.INTEGER, value.coerceAtLeast(0))
        }

    /** Refills the player's flask to its maximum charges. */
    fun Player.refillFlask() {
        flaskCharges = flaskMaxCharges
    }

    /** Clears all death-stain data for this [Player]. */
    fun Player.clearStain() {
        stainLocation = null
        stainedSouls = 0
    }
}
