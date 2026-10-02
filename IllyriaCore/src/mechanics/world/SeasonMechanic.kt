package org.xodium.illyriacore.mechanics.world

import io.papermc.paper.command.brigadier.Commands
import org.xodium.illyriacore.data.SeasonState
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling server MOTD within the system. */
internal object MotdMechanic : MechanicInterface {
    override val cmds: Collection<CommandData>
        = listOf(
            CommandData(
                Commands
                    .literal("season")
                    .requiresPlayer { isOp }
                    .playerExecuted { player, _ ->
                        val world = player.world
                        val season = SeasonState.currentSeason(world)
                        val day = SeasonState.dayInSeason(world)
                        player.sendMessage(
                            MM.deserialize(
                                "<gray>It is currently <green>${
                                    season.name.lowercase().replaceFirstChar(Char::uppercase)
                                }</green>, " +
                                    "day <yellow>$day</yellow>/<yellow>${season.lengthInDays}</yellow> " +
                                    "(${SeasonState.seasonalDay(world)}/${SeasonState.DAYS_PER_YEAR}).",
                            ),
                        )
                    }.then(
                        Commands.literal("next").playerExecuted { player, _ ->
                            SeasonState.advanceSeason(player.world)
                            player.sendMessage(
                                MM.deserialize("<gray>Skipped to the next season."),
                            )
                        },
                    ),
                "Inspects and controls the current season.",
            )
        )
}
