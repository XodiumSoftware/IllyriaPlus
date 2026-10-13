@file:Suppress("Unused", "UnstableApiUsage")

package org.xodium.illyrialib

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.time.Duration

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

    /** Converts a [Duration] to Minecraft ticks (20 ticks per second). */
    fun Duration.toTicks(): Int = inWholeSeconds.toInt() * 20

    /**
     * Converts a snake_case string to Proper Case with spaces.
     *
     * @return The formatted string in Proper Case.
     */
    fun String.snakeToProperCase(): String =
        split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    /**
     * Converts a class name to a snake_case registry key fragment, removing a suffix.
     *
     * @return The generated registry key fragment.
     */
    inline fun <reified T> Class<*>.toRegistryKeyFragment(): String = toRegistryKeyFragment(T::class.simpleName ?: "")

    fun Class<*>.toRegistryKeyFragment(suffix: String): String =
        simpleName
            .removeSuffix(suffix)
            .split(Regex("(?=[A-Z])"))
            .filter { it.isNotEmpty() }
            .joinToString("_") { it.lowercase() }

    /**
     * Validates that the server version matches the plugin's target version.
     *
     * Disables the plugin if the server version does not contain the target version
     * substring (the version before the `+` in the version string).
     */
    fun Plugin.validateVersion() {
        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
        }
    }

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
         * Restricts a command to players matching the given [predicate].
         *
         * @param predicate Condition the [Player] must satisfy.
         * @return The modified [ArgumentBuilder].
         */
        fun <T : ArgumentBuilder<CommandSourceStack, T>> T.requiresPlayer(predicate: Player.() -> Boolean): T {
            requires { ((it.sender as? Player)?.predicate()) == true }
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

    /** Enchantment-related utilities. */
    object Enchantment {
        /**
         * Gets the display name of an enchantment key.
         *
         * @return The formatted display name as a Component.
         */
        fun TypedKey<org.bukkit.enchantments.Enchantment>.displayName(): Component =
            MM.deserialize(value().snakeToProperCase())
    }
}
