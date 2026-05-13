package org.xodium.illyriaplus.data

import org.bukkit.entity.Player
import org.xodium.illyriaplus.tables.PlayerTable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents persistent player data stored in the database.
 *
 * @property uuid The player's unique Mojang UUID.
 * @property nickname The player's optional custom display name.
 * @property scoreboardVisibility Whether the player's scoreboard is enabled.
 */
@OptIn(ExperimentalUuidApi::class)
internal data class PlayerData(
    val uuid: Uuid,
    val nickname: String?,
    val scoreboardVisibility: Boolean,
) {
    companion object {
        /**
         * Persistent nickname stored for the player.
         *
         * Reads from and writes to the database via [PlayerTable].
         * Returns the player's name if no nickname is set.
         */
        var Player.nickname: String
            get() = PlayerTable.findByPlayer(this)?.nickname ?: name
            set(value) {
                PlayerTable.updateNickname(this, value)
            }

        /**
         * Whether the player's scoreboard is enabled.
         *
         * Reads from and writes to the database via [PlayerTable].
         * Defaults to false if no database entry exists.
         */
        var Player.scoreboardVisibility: Boolean
            get() = PlayerTable.findByPlayer(this)?.scoreboardVisibility ?: false
            set(value) {
                PlayerTable.updateScoreboardVisibility(this, value)
            }
    }
}
