package org.xodium.illyriaseasons

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.xodium.illyrialib.Utils.Command.executesCatching
import org.xodium.illyrialib.Utils.MM

/** Provides the `/season` command for inspecting and controlling the seasonal cycle. */
internal object SeasonCommand {
    private const val NAME = "season"

    /** Registers the `/season` command with Paper's modern command lifecycle. */
    @Suppress("UnstableApiUsage")
    fun register(plugin: IllyriaSeasons) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                build().build(),
                "Inspects and controls the current season.",
                listOf("seasons"),
            )
        }
    }

    private fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(NAME)
            .requires { (it.sender as? Player)?.isOp == true }
            .executesCatching { ctx ->
                val world = (ctx.source.sender as? Player)?.world ?: return@executesCatching
                val season = SeasonState.currentSeason(world)
                val day = SeasonState.dayInSeason(world)
                ctx.source.sender.sendMessage(
                    MM.deserialize(
                        "<gray>It is currently <green>${
                            season.name.lowercase().replaceFirstChar(Char::uppercase)
                        }</green>, " +
                            "day <yellow>$day</yellow>/<yellow>${season.lengthInDays}</yellow> " +
                            "(${SeasonState.seasonalDay(world)}/${SeasonState.DAYS_PER_YEAR}).",
                    ),
                )
            }.then(
                Commands.literal("next").executesCatching {
                    val world = (it.source.sender as? Player)?.world ?: return@executesCatching
                    SeasonState.advanceSeason(world)
                    it.source.sender.sendMessage(
                        MM.deserialize("<gray>Skipped to the next season."),
                    )
                },
            )
}
