@file:Suppress("Unused")

package org.xodium.illyrialib

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** General utilities shared across the IllyriaPlus plugin modules. */
object Utils {
    /** MiniMessage instance for parsing formatted strings with custom gradient aliases. */
    val MM: MiniMessage =
        MiniMessage
            .builder()
            .editTags {
                listOf(
                    "mango" to "#FFE259:#FFA751",
                    "mango_r" to "#FFA751:#FFE259",
                    "firewatch" to "#CB2D3E:#EF473A",
                    "skyline" to "#1488CC:#2B32B2",
                    "deep-ocean" to "#13547a:#80d0c7",
                    "rose" to "#F4C4F3:#FC67FA",
                ).forEach { (name, colors) -> it.tag(name, Tag.preProcessParsed("<gradient:$colors>")) }
            }.build()

    /** Command-related utilities. */
    object Command {
        /**
         * Adds a safe execution handler with error logging.
         *
         * @param action Command execution logic.
         * @return The modified [ArgumentBuilder].
         */
        fun <T : ArgumentBuilder<CommandSourceStack, T>> T.executesCatching(
            action: (CommandContext<CommandSourceStack>) -> Unit,
        ): T {
            executes { ctx ->
                runCatching { action(ctx) }
                    .onFailure {
                        Bukkit.getLogger().severe(
                            """
                            Command error: ${it.message}
                            ${it.stackTraceToString()}
                            """.trimIndent(),
                        )
                        (ctx.source.sender as? Player)?.sendActionBar(
                            MM.deserialize("<red>An error has occurred. Check server logs for details."),
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

        /**
         * Executes a command restricted to players.
         *
         * @param action Execution logic with player context.
         * @return The modified [ArgumentBuilder].
         */
        fun <T : ArgumentBuilder<CommandSourceStack, T>> T.playerExecuted(
            action: (Player, CommandContext<CommandSourceStack>) -> Unit,
        ): T {
            executesCatching {
                action(
                    it.source.sender as? Player ?: run {
                        Bukkit.getLogger().warning("Command can only be executed by a Player!")
                        return@executesCatching
                    },
                    it,
                )
            }
            return this
        }
    }
}
