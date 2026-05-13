package org.xodium.illyriaplus.data

import org.bukkit.Location
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a teleport anchor in the world.
 *
 * @property uuid The unique identifier of the anchor.
 * @property name The display name of the anchor.
 * @property location The [Location] of the anchor in the world.
 */
@OptIn(ExperimentalUuidApi::class)
internal data class AnchorData(
    val uuid: Uuid,
    val name: String,
    val location: Location,
) {
    /**
     * Checks whether this anchor occupies the same block as [other].
     *
     * Compares world and block coordinates (X, Y, Z).
     *
     * @param other The [Location] to compare against.
     * @return `true` if the world and block coordinates match.
     */
    fun matches(other: Location): Boolean =
        location.world == other.world &&
            location.blockX == other.blockX &&
            location.blockY == other.blockY &&
            location.blockZ == other.blockZ
}
