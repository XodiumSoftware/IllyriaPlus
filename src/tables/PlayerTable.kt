package org.xodium.illyriaplus.tables

import org.bukkit.entity.Player
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.xodium.illyriaplus.data.PlayerData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/**
 * Database table storing persistent player data.
 *
 * Columns:
 * - **uuid**: Primary key representing the player's UUID.
 * - **nickname**: Optional custom display name.
 * - **scoreboardVisibility**: Whether the player has their scoreboard enabled.
 */
@OptIn(ExperimentalUuidApi::class)
internal object PlayerTable : Table() {
    val uuid = uuid("uuid")

    val nickname = varchar("nickname", 255).nullable()

    val scoreboardVisibility = bool("scoreboard_visibility").default(false)

    override val primaryKey = PrimaryKey(uuid)

    /** Converts a [ResultRow] from this table into a [PlayerData]. */
    private fun ResultRow.toPlayer(): PlayerData =
        PlayerData(
            uuid = this[uuid],
            nickname = this[nickname],
            scoreboardVisibility = this[scoreboardVisibility],
        )

    /** Finds the [PlayerData] for the given [player], or `null` if no row exists. */
    fun findByPlayer(player: Player): PlayerData? =
        transaction {
            selectAll()
                .where { uuid eq player.uniqueId.toKotlinUuid() }
                .singleOrNull()
                ?.toPlayer()
        }

    /**
     * Updates the nickname for the given [player].
     *
     * Does nothing if the player has no database row.
     */
    fun updateNickname(
        player: Player,
        nickname: String?,
    ) = transaction {
        update({ uuid eq player.uniqueId.toKotlinUuid() }) {
            it[PlayerTable.nickname] = nickname
        }
    }

    /**
     * Updates the scoreboard visibility for the given [player].
     *
     * Does nothing if the player has no database row.
     */
    fun updateScoreboardVisibility(
        player: Player,
        visible: Boolean,
    ) = transaction {
        update({ uuid eq player.uniqueId.toKotlinUuid() }) {
            it[PlayerTable.scoreboardVisibility] = visible
        }
    }

    /**
     * Inserts a new row for the given [player].
     *
     * @param player The player to persist.
     * @param nickname Optional custom nickname.
     * @param scoreboardVisibility Initial scoreboard visibility. Defaults to `false`.
     * @return The player's UUID.
     */
    fun insert(
        player: Player,
        nickname: String? = null,
        scoreboardVisibility: Boolean = false,
    ): Uuid =
        transaction {
            val newUuid = player.uniqueId.toKotlinUuid()

            PlayerTable.insert {
                it[uuid] = newUuid
                it[PlayerTable.nickname] = nickname
                it[PlayerTable.scoreboardVisibility] = scoreboardVisibility
            }

            newUuid
        }

    /**
     * Ensures the given [player] has a database row.
     *
     * Returns existing [PlayerData] if found, otherwise inserts defaults
     * and returns the newly created row.
     */
    fun ensurePlayer(player: Player): PlayerData =
        findByPlayer(player) ?: run {
            insert(player)
            PlayerData(
                uuid = player.uniqueId.toKotlinUuid(),
                nickname = null,
                scoreboardVisibility = false,
            )
        }
}
