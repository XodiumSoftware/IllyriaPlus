package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.pdcs.PlayerPDC.scoreboardVisibility

/** Represents a mechanic handling scoreboard display within the system. */
internal object ScoreBoardMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("leaderboard")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.toggleScoreboard() },
                "This command allows you to open the leaderboard",
                listOf("lb", "board"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.leaderboard".lowercase(),
                "Allows use of the leaderboard command",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        handleJoin(event)
    }

    /**
     * Configures the scoreboard for the player on join.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun handleJoin(event: PlayerJoinEvent) {
        event.player.configureScoreboard()
    }

    /** Toggles scoreboard visibility and applies the correct scoreboard. */
    private fun Player.toggleScoreboard() {
        scoreboardVisibility = !scoreboardVisibility
        configureScoreboard()
    }

    /** Applies the correct scoreboard based on the player's visibility preference. */
    private fun Player.configureScoreboard() {
        scoreboard =
            if (scoreboardVisibility) {
                instance.server.scoreboardManager.newScoreboard
            } else {
                instance.server.scoreboardManager.mainScoreboard
            }
    }
}
