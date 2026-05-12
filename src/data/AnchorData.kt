package org.xodium.illyriaplus.data

import org.jetbrains.exposed.v1.core.Table.Dual.uuid
import org.jetbrains.exposed.v1.core.Table.Dual.varchar
import org.xodium.illyriaplus.data.AnchorData.name
import kotlin.uuid.ExperimentalUuidApi

/**
 * Represents the data structure for a teleport destination.
 *
 * @property name The display name of this teleport anchor.
 * @property x The X coordinate of the teleport location.
 * @property y The Y coordinate of the teleport location.
 * @property z The Z coordinate of the teleport location.
 */
@OptIn(ExperimentalUuidApi::class)
internal object AnchorData {
    val uuid = uuid("uuid")

    val name = varchar("name", 255).nullable()

    val location

    /**
     * Returns a copy of this anchor with the given [name].
     *
     * @param name The new display name.
     * @return A new [AnchorData] with the updated name.
     */
    fun name(name: String): AnchorData = copy(name = name)

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
