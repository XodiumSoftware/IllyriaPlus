package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriacore.dialogs.NicknameDialog
import org.xodium.illyriacore.dialogs.NicknameDialog.nickname
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player nicknames within the system. */
internal object NicknameMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("nickname")
                    .playerExecuted { player, _ -> player.showDialog(NicknameDialog(player)) },
                "Opens the nickname dialog",
                listOf("nick"),
            ),
        )

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = event.player.nickname()
}
