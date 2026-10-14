package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriacore.advancements.BendingAdvancement
import org.xodium.illyriacore.dialogs.BendingDialog
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.bendingType
import org.xodium.illyriacore.pdcs.PlayerPDC.hasBendingType
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player bending within the system. */
internal object BendingMechanic : MechanicInterface {
    private const val PLAYER_IS_NOT_ONLINE_MSG = "<red>That player is not online!"
    private const val RESET_SENDER_MSG = "<green>Reset bending type for <gold><target></gold>."
    private const val RESET_TARGET_MSG = "<green>Your bending type has been reset."

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("bending")
                    .playerExecuted { player, _ -> player.showDialog(BendingDialog(player)) }
                    .then(
                        Commands
                            .literal("reset")
                            .requiresPlayer { isOp }
                            .then(
                                Commands
                                    .argument("target", ArgumentTypes.player())
                                    .playerExecuted { player, ctx ->
                                        val target =
                                            ctx
                                                .getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                                .resolve(ctx.source)
                                                .singleOrNull()
                                                ?: return@playerExecuted player.sendActionBar(
                                                    MM.deserialize(PLAYER_IS_NOT_ONLINE_MSG),
                                                )
                                        target.bendingType = null
                                        BendingAdvancement.revokeElements(target)
                                        player.sendActionBar(
                                            MM.deserialize(
                                                RESET_SENDER_MSG,
                                                Placeholder.unparsed("target", target.name),
                                            ),
                                        )
                                        target.sendActionBar(MM.deserialize(RESET_TARGET_MSG))
                                    },
                            ),
                    ),
                "Opens the bending selection dialog",
            ),
        )

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        if (!event.player.hasBendingType) BendingAdvancement.grantRoot(event.player)
    }
}
