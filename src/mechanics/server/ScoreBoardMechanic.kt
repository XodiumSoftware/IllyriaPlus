package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.Utils.PlayerUtils.applyScoreboard
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.tables.PlayerTable.Utils.scoreboardVisibility

/** Represents a mechanic handling scoreboard display within the system. */
internal object ScoreBoardMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("leaderboard")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ ->
                        player.also { it.scoreboardVisibility = !it.scoreboardVisibility }.applyScoreboard()
                    },
                "This command allows you to open the leaderboard",
                listOf("lb", "board"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${IllyriaPlus.instance.javaClass.simpleName}.leaderboard".lowercase(),
                "Allows use of the leaderboard command",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler
    fun on(event: PlayerJoinEvent) = event.player.applyScoreboard()
}
