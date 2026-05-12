package org.xodium.illyriaplus.tables

import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.tables.AnchorTable.insert
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Exposed table mapping teleport anchors to their display name and world location.
 *
 * Columns:
 * - **uuid**: Primary key (randomly generated per anchor).
 * - **name**: Human-readable anchor label. Defaults to the next free
 *   "Anchor N" string when inserted via [insert].
 * - **x**, **y**, **z**: Overworld coordinates of the anchor.
 */
@OptIn(ExperimentalUuidApi::class)
internal object AnchorTable : Table() {
    val uuid = uuid("uuid")

    val name = varchar("name", 255)

    val x = double("x")

    val y = double("y")

    val z = double("z")

    override val primaryKey = PrimaryKey(uuid)

    /**
     * Returns the next available default anchor name (e.g., "Anchor 1", "Anchor 2").
     *
     * Scans existing rows to find the lowest unused integer suffix.
     */
    private fun nextName(): String =
        transaction {
            "Anchor ${
                (1..Int.MAX_VALUE).first { int ->
                    int !in
                        selectAll()
                            .mapNotNull { it[name].removePrefix("Anchor ").toIntOrNull() }
                            .toSet()
                }
            }"
        }

    /**
     * Lightweight domain object representing a single teleport anchor row.
     */
    data class Anchor(
        val uuid: Uuid,
        val name: String,
        val location: Location,
    ) {
        /** The world this anchor resides in. */
        val world: World get() = location.world

        /**
         * Checks whether this anchor occupies the same block as [other].
         *
         * Compares world and block coordinates (X, Y, Z).
         */
        fun matches(other: Location): Boolean =
            world == other.world &&
                location.blockX == other.blockX &&
                location.blockY == other.blockY &&
                location.blockZ == other.blockZ
    }

    /** Converts a [ResultRow] from this table into an [Anchor]. */
    private fun ResultRow.toAnchor(): Anchor =
        Anchor(
            uuid = this[uuid],
            name = this[name],
            location =
                Location(
                    instance.server.getWorld("world"),
                    this[x],
                    this[y],
                    this[z],
                ),
        )

    /** Returns every anchor stored in the table. */
    fun all(): List<Anchor> = transaction { selectAll().map { it.toAnchor() } }

    /**
     * Finds the anchor whose stored coordinates exactly match [location].
     *
     * Block placement coordinates are exact doubles, so direct equality is safe.
     */
    fun findByLocation(location: Location): Anchor? =
        transaction {
            selectAll()
                .firstOrNull { it[x] == location.x && it[y] == location.y && it[z] == location.z }
                ?.toAnchor()
        }

    /**
     * Renames the anchor with the given [uuid].
     *
     * @param uuid The primary key of the row to update.
     * @param newName The new display name.
     */
    fun updateName(
        uuid: Uuid,
        newName: String,
    ) = transaction { update({ AnchorTable.uuid eq uuid }) { it[name] = newName } }

    /**
     * Deletes the anchor at the given [location].
     *
     * @return `true` if a row was removed, `false` if no anchor matched.
     */
    fun deleteByLocation(location: Location): Boolean =
        transaction {
            val anchor = findByLocation(location) ?: return@transaction false

            deleteWhere { uuid eq anchor.uuid }
            true
        }

    /**
     * Inserts a new anchor with the given [location].
     *
     * @param location The Bukkit [Location] of the anchor.
     * @param anchorName Optional display name. Falls back to the next
     *   auto-generated "Anchor N" name if omitted.
     * @return The randomly generated UUID of the inserted row.
     */
    fun insert(
        location: Location,
        anchorName: String? = null,
    ): Uuid =
        transaction {
            val newUuid = Uuid.random()

            AnchorTable.insert {
                it[uuid] = newUuid
                it[name] = anchorName ?: nextName()
                it[x] = location.x
                it[y] = location.y
                it[z] = location.z
            }
            newUuid
        }
}
