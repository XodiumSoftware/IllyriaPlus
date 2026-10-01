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
    /** Registers the `/season` command with Paper's modern command lifecycle. */
    @Suppress("UnstableApiUsage")
    fun register(plugin: IllyriaSeasons) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                build().build(),
                "Inspects and controls the current season.",
            )
        }
    }

    private fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("season")
            .requires { (it.sender as? Player)?.isOp == true }
            .executesCatching {
                val dayOfYear = SeasonSystem.dayOfYear()
                val season = SeasonState.of(dayOfYear)
                val day = (SeasonState.progress(dayOfYear) * season.lengthInDays).toInt() + 1
                it.source.sender.sendMessage(
                    MM.deserialize(
                        "<gray>It is currently <green>${
                            season.name.lowercase().replaceFirstChar(Char::uppercase)
                        }</green>, " +
                            "day <yellow>$day</yellow> of <yellow>${season.lengthInDays}</yellow>.",
                    ),
                )
            }.then(
                Commands.literal("next").executesCatching {
                    SeasonSystem.advance()
                    it.source.sender.sendMessage(
                        MM.deserialize("<gray>Skipped to the next season."),
                    )
                },
            )
}
