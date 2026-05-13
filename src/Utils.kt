@file:Suppress("ktlint:standard:no-wildcard-imports", "Unused")

package org.xodium.illyriaplus

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.IllyriaPlus.Companion.prefix
import org.xodium.illyriaplus.pdcs.ItemStackPDC.selectedSpell

/** General utilities. */
internal object Utils {
    /** MiniMessage instance for parsing formatted strings. */
    val MM: MiniMessage = MiniMessage.miniMessage()

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
    inline fun <reified T> Class<*>.toRegistryKeyFragment(): String =
        simpleName
            .removeSuffix(T::class.simpleName ?: "")
            .split(Regex("(?=[A-Z])"))
            .filter { it.isNotEmpty() }
            .joinToString("_") { it.lowercase() }

    /** Enchantment-related utilities. */
    object EnchantmentUtils {
        /**
         * Gets the display name of an enchantment key.
         *
         * @return The formatted display name as a Component.
         */
        fun TypedKey<Enchantment>.displayName(): Component = MM.deserialize(value().snakeToProperCase())

        /**
         * Checks if the given item has the specified spell selected.
         *
         * @param item The item to check.
         * @param spell The enchantment representing the spell.
         * @return True if the spell is selected, false otherwise.
         */
        fun isSelectedSpell(
            item: ItemStack?,
            spell: Enchantment,
        ): Boolean = item?.selectedSpell == spell.key.toString()

        /**
         * Validates a spell cast interaction.
         *
         * @param action The interaction action.
         * @param item The item used.
         * @param enchantment The required enchantment.
         * @return True if valid, false otherwise.
         */
        fun validateSpellCast(
            action: Action,
            item: ItemStack,
            enchantment: Enchantment,
        ): Boolean =
            when {
                action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK -> false
                item.type != Material.BLAZE_ROD -> false
                !item.containsEnchantment(enchantment) -> false
                else -> true
            }
    }

    /** Schedule-related utilities. */
    object ScheduleUtils {
        /**
         * Schedules a repeating task.
         *
         * @param delay Initial delay in ticks.
         * @param period Interval between executions.
         * @param duration Optional total runtime.
         * @param content Task logic.
         * @return The scheduled BukkitTask.
         */
        fun schedule(
            delay: Long = 0L,
            period: Long = 2L,
            duration: Long? = null,
            content: () -> Unit,
        ): BukkitTask =
            instance.server.scheduler
                .runTaskTimer(instance, content, delay, period)
                .also { task ->
                    duration?.let {
                        instance.server.scheduler.runTaskLater(
                            instance,
                            task::cancel,
                            it,
                        )
                    }
                }

        /**
         * Spawns a particle trail following an entity.
         *
         * @param entity The entity to follow.
         * @param particles Particle logic per tick.
         * @return The running BukkitTask.
         */
        fun spawnProjectileTrail(
            entity: Entity,
            particles: (Location) -> Unit,
        ): BukkitTask {
            lateinit var task: BukkitTask

            task =
                schedule(delay = 1L, period = 1L) {
                    if (!entity.isValid) {
                        task.cancel()
                        return@schedule
                    }

                    particles(entity.location)
                }

            return task
        }
    }

    /** Command-related utilities. */
    object CommandUtils {
        /**
         * Adds a safe execution handler with error logging.
         *
         * @param action Command execution logic.
         * @return The modified ArgumentBuilder.
         */
        fun <T : ArgumentBuilder<CommandSourceStack, T>> T.executesCatching(
            action: (CommandContext<CommandSourceStack>) -> Unit,
        ): T {
            executes { ctx ->
                runCatching { action(ctx) }
                    .onFailure {
                        instance.logger.severe(
                            """
                            Command error: ${it.message}
                            ${it.stackTraceToString()}
                            """.trimIndent(),
                        )
                        (ctx.source.sender as? Player)?.sendMessage(
                            MM.deserialize(
                                "${instance.prefix} <red>An error has occurred. Check server logs for details.",
                            ),
                        )
                    }
                Command.SINGLE_SUCCESS
            }
            return this
        }

        /**
         * Executes a command restricted to players.
         *
         * @param action Execution logic with player context.
         * @return The modified ArgumentBuilder.
         */
        fun <T : ArgumentBuilder<CommandSourceStack, T>> T.playerExecuted(
            action: (Player, CommandContext<CommandSourceStack>) -> Unit,
        ): T {
            executesCatching {
                action(
                    it.source.sender as? Player ?: run {
                        instance.logger.warning("Command can only be executed by a Player!")
                        return@executesCatching
                    },
                    it,
                )
            }
            return this
        }
    }

    /** Block-related utilities. */
    object BlockUtils {
        /**
         * Gets the center location of a block, handling double chests.
         *
         * @return The center Location.
         */
        fun Block.center(): Location {
            val baseAddition =
                Location(location.world, location.x + 0.5, location.y + 0.5, location.z + 0.5)
            val chestState = state as? Chest ?: return baseAddition
            val holder = chestState.inventory.holder as? DoubleChest ?: return baseAddition
            val leftBlock = (holder.leftSide as? Chest)?.block
            val rightBlock = (holder.rightSide as? Chest)?.block

            if (leftBlock == null || rightBlock == null || leftBlock.world !== rightBlock.world) {
                return baseAddition
            }

            val world = leftBlock.world
            val cx = (leftBlock.x + rightBlock.x) / 2.0 + 0.5
            val cy = (leftBlock.y + rightBlock.y) / 2.0 + 0.5
            val cz = (leftBlock.z + rightBlock.z) / 2.0 + 0.5

            return Location(world, cx, cy, cz)
        }
    }

    /** Player-related utilities. */
    object PlayerUtils {
        /**
         * Gets nearby containers in a chunk radius.
         *
         * @return Set of containers.
         */
        fun Player.getContainersAround(): Set<Container> =
            buildSet {
                for (chunk in getChunksAround()) {
                    for (state in chunk.tileEntities) {
                        if (state is Container) add(state)
                    }
                }
            }

        /**
         * Gets surrounding chunks.
         *
         * @param range Radius in chunks.
         * @return Set of chunks.
         */
        fun Player.getChunksAround(range: Int = 1): Set<Chunk> {
            val (baseX, baseZ) = location.chunk.run { x to z }

            return buildSet {
                for (x in -range..range) {
                    for (z in -range..range) {
                        add(world.getChunkAt(baseX + x, baseZ + z))
                    }
                }
            }
        }

        /**
         * Gets the first leashed tameable entity owned by the player.
         *
         * @param radius Search radius.
         * @return The entity or null.
         */
        fun Player.getLeashedEntity(radius: Double = 10.0): Tameable? =
            getNearbyEntities(radius, radius, radius)
                .filterIsInstance<Tameable>()
                .firstOrNull { it.isLeashed && it.leashHolder == this }
    }
}
