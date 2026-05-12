package org.xodium.illyriaplus.data

import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.World
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance

/**
 * Represents the data structure for a teleport destination.
 *
 * @property name The display name of this teleport anchor.
 * @property x The X coordinate of the teleport location.
 * @property y The Y coordinate of the teleport location.
 * @property z The Z coordinate of the teleport location.
 */
@Serializable
internal data class AnchorData(
    val name: String,
    private val x: Double,
    private val y: Double,
    private val z: Double,
) {
    /** The [World] this anchor resides in. */
    val world: World get() = instance.server.getWorld("world") ?: error("Overworld not found")

    /** The specific [Location] within the world to teleport to. */
    val location: Location get() = Location(world, x, y, z)

    /**
     * Convenience constructor from Bukkit types.
     *
     * @param name The display name of this teleport anchor.
     * @param location The specific [Location] to teleport to.
     */
    constructor(name: String, location: Location) : this(name, location.x, location.y, location.z)

    /**
     * Checks if the given [Location] matches this teleport anchor's position.
     *
     * @param location The [Location] to compare against.
     * @return `true` if the world and block coordinates (X, Y, Z) match; `false` otherwise.
     */
    fun matches(location: Location): Boolean =
        world == location.world &&
            this.location.blockX == location.blockX &&
            this.location.blockY == location.blockY &&
            this.location.blockZ == location.blockZ

    /**
     * Returns a copy of this anchor with the given [name].
     *
     * @param name The new display name.
     * @return A new [AnchorData] with the updated name.
     */
    fun name(name: String): AnchorData = copy(name = name)

    companion object {
        /**
         * Generates the next available default anchor name (e.g., "Anchor 1", "Anchor 2").
         *
         * @param existing The list of existing [AnchorData] entries.
         * @return The next unused "Anchor N" name.
         */
        fun nextName(existing: List<AnchorData>): String =
            "Anchor ${
                (1..Int.MAX_VALUE).first {
                    it !in
                        existing
                            .mapNotNull { anchor -> anchor.name.removePrefix("Anchor ").toIntOrNull() }
                            .toSet()
                }
            }"
    }
}
