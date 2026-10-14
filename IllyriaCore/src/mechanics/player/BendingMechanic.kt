package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.xodium.illyriacore.dialogs.BendingDialog
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player bending within the system. */
internal object BendingMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("bending")
                    .playerExecuted { player, _ -> player.showDialog(BendingDialog(player)) },
                "Opens the bending selection dialog",
            ),
        )
}
