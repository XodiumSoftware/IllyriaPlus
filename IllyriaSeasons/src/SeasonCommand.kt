package org.xodium.illyriaseasons

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import org.xodium.illyrialib.CommandInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.MM

/** Provides the `/season` command for inspecting and controlling the seasonal cycle. */
internal object SeasonCommand : CommandInterface {
    override fun ReloadableRegistrarEvent<Commands>.onRegister() {
        registrar().register(
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
                ).build(),
            "Inspects and controls the current season.",
        )
    }
}
