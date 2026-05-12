package org.xodium.illyriaplus.tables

import org.bukkit.entity.Player
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Database table storing persistent player data.
 *
 * This table is used to persist per-player settings such as nickname
 * and scoreboard visibility.
 *
 * Columns:
 * - uuid: Primary key representing the player's UUID.
 * - nickname: Optional custom display name.
 * - scoreboardVisibility: Whether the player has their scoreboard enabled.
 */
@OptIn(ExperimentalUuidApi::class)
internal object PlayerTable : Table() {
    /**
     * Player's Mojang UUID (primary key).
     */
    val uuid = uuid("uuid")

    /**
     * Optional custom nickname for the player.
     */
    val nickname = varchar("nickname", 255).nullable()

    /**
     * Whether the player's scoreboard is visible.
     */
    val scoreboardVisibility = bool("scoreboard_visibility").default(false)

    override val primaryKey = PrimaryKey(uuid)

    /**
     * Utility accessors for Player-related database fields.
     *
     * Provides extension properties for directly reading/writing
     * persistent player data stored in the PlayerTable.
     *
     * All operations are executed inside Exposed transactions.
     */
    object Utils {
        /**
         * Persistent nickname stored for the player.
         *
         * Reads from and writes to the database directly.
         * Returns name if no nickname is set.
         */
        var Player.nickname: String
            get() =
                transaction {
                    selectAll()
                        .where { uuid eq this@nickname.uniqueId.toKotlinUuid() }
                        .singleOrNull()
                        ?.get(PlayerTable.nickname)
                        ?: name
                }
            set(value) =
                transaction {
                    update({ uuid eq this@nickname.uniqueId.toKotlinUuid() }) {
                        it[PlayerTable.nickname] = value
                    }
                }

        /**
         * Whether the player's scoreboard is enabled.
         *
         * Defaults to false if no database entry exists.
         */
        var Player.scoreboardVisibility: Boolean
            get() =
                transaction {
                    selectAll()
                        .where { uuid eq this@scoreboardVisibility.uniqueId.toKotlinUuid() }
                        .singleOrNull()
                        ?.get(PlayerTable.scoreboardVisibility)
                        ?: false
                }
            set(value) =
                transaction {
                    update({ uuid eq this@scoreboardVisibility.uniqueId.toKotlinUuid() }) {
                        it[PlayerTable.scoreboardVisibility] = value
                    }
                }
    }
}
