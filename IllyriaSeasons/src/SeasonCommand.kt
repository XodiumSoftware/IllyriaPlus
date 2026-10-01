package org.xodium.illyriaseasons

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.xodium.illyrialib.Utils.MM

/** Provides the `/season` command for inspecting and controlling the seasonal cycle. */
internal object SeasonCommand {
    private const val NAME = "season"
    private const val PERM = "illyriaseasons.command.season"

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
            .requires { it.sender.hasPermission(PERM) }
            .executesCatching { ctx ->
                val season = SeasonState.of(SeasonSystem.dayOfYear())
                val progression = SeasonState.progress(SeasonSystem.dayOfYear())
                val day = (progression * season.lengthInDays).toInt() + 1
                ctx.source.sender.sendMessage(
                    MM.deserialize(
                        "<gray>It is currently <green>${
                            season.name.lowercase().replaceFirstChar { it.uppercase() }
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

    /**
     * Adds a safe execution handler with error logging.
     *
     * @param action Command execution logic.
     * @return The modified [LiteralArgumentBuilder].
     */
    private fun <T : com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T.executesCatching(
        action: (com.mojang.brigadier.context.CommandContext<CommandSourceStack>) -> Unit,
    ): T {
        executes { ctx ->
            runCatching { action(ctx) }
                .onFailure {
                    IllyriaSeasons.instance.logger.severe("Season command error: ${it.message}")
                    ctx.source.sender.sendActionBar(
                        Component.text("An error occurred. Check server logs."),
                    )
                }
            com
                .mojang
                .brigadier
                .Command
                .SINGLE_SUCCESS
        }
        return this
    }
}
