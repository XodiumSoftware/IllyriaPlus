package org.xodium.illyriaplus.dialogs

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.dialog.Dialog
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.DialogInterface

/** Represents a dialog handling faq within the system. */
@Suppress("UnstableApiUsage")
internal object FaqDialog : DialogInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("faq")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.showDialog(dialog) },
                "Opens the FAQ dialog",
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.faq".lowercase(),
                "Allows use of the /faq command",
                PermissionDefault.TRUE,
            ),
        )

    override val dialog =
        Dialog.create {
        }
}
