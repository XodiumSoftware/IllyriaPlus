package org.xodium.illyriaplus

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.Tameable
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import kotlin.math.floor
import kotlin.time.Duration

/** General utilities. */
internal object Utils {
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

    /** Schedule-related utilities. */
    object Schedule {
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
    }

    /** Command-related utilities. */
    object Command {
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
                        (ctx.source.sender as? org.bukkit.entity.Player)?.sendActionBar(
                            MM.deserialize("<red>An error has occurred. Check server logs for details."),
                        )
                    }
                com.mojang.brigadier.Command.SINGLE_SUCCESS
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
            action: (org.bukkit.entity.Player, CommandContext<CommandSourceStack>) -> Unit,
        ): T {
            executesCatching {
                action(
                    it.source.sender as? org.bukkit.entity.Player ?: run {
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
    object Block {
        /**
         * Gets the center location of a block, handling double chests.
         *
         * @return The center Location.
         */
        fun org.bukkit.block.Block.center(): Location {
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
    object Player {
        /**
         * Gets nearby containers in a chunk radius.
         *
         * @return Set of containers.
         */
        fun org.bukkit.entity.Player.getContainersAround(): Set<Container> =
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
        fun org.bukkit.entity.Player.getChunksAround(range: Int = 1): Set<Chunk> {
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
        fun org.bukkit.entity.Player.getLeashedEntity(radius: Double = 10.0): Tameable? =
            getNearbyEntities(radius, radius, radius)
                .filterIsInstance<Tameable>()
                .firstOrNull { it.isLeashed && it.leashHolder == this }
    }

    /** Monster-related utilities. */
    object Monster {
        /**
         * Spawns an [AbstractHorse] mount for this [Monster] with a configurable chance,
         * then applies [attributes] to the horse and makes this monster its passenger.
         *
         * @param H The concrete horse type to spawn.
         * @param chance The percentage chance for the mount to spawn.
         * @param attributes A map of attribute mutations to apply to the spawned horse.
         */
        inline fun <reified H : AbstractHorse> org.bukkit.entity.Monster.trySpawnMount(
            chance: Int,
            attributes: Map<Attribute, (AbstractHorse, AttributeInstance) -> Unit>,
        ) {
            if ((1..100).random() > chance) return
            world
                .spawn(location, H::class.java) { horse ->
                    horse.isTamed = true
                    attributes.forEach { (attribute, apply) ->
                        horse.getAttribute(attribute)?.let { apply(horse, it) }
                    }
                }.addPassenger(this)
        }
    }

    /** Math and noise-related utilities. */
    object Math {
        /**
         * Generates a deterministic 2D gradient noise value in roughly [-1, 1].
         *
         * The same [seed], [x], and [y] inputs always produce the same output,
         * making it suitable for reproducible terrain-like drift patterns.
         */
        fun noise2D(
            x: Double,
            y: Double,
            seed: Long,
        ): Double {
            val floorX = floor(x).toInt()
            val floorY = floor(y).toInt()

            val g00 = gradient(floorX, floorY, seed)
            val g10 = gradient(floorX + 1, floorY, seed)
            val g01 = gradient(floorX, floorY + 1, seed)
            val g11 = gradient(floorX + 1, floorY + 1, seed)

            val xf = x - floorX
            val yf = y - floorY
            val u = smoothStep(xf)
            val v = smoothStep(yf)

            val n00 = g00.first * xf + g00.second * yf
            val n10 = g10.first * (xf - 1) + g10.second * yf
            val n01 = g01.first * xf + g01.second * (yf - 1)
            val n11 = g11.first * (xf - 1) + g11.second * (yf - 1)

            val nx0 = n00 * (1 - u) + n10 * u
            val nx1 = n01 * (1 - u) + n11 * u

            return (nx0 * (1 - v) + nx1 * v).coerceIn(-1.0, 1.0)
        }

        /**
         * Returns a pseudo-random unit gradient vector from a lattice point.
         *
         * @param seed The seed for the noise field.
         * @param x Lattice x coordinate.
         * @param y Lattice y coordinate.
         */
        fun gradient(
            x: Int,
            y: Int,
            seed: Long,
        ): Pair<Double, Double> {
            val hash = mix(seed, x.toLong(), y.toLong())
            val angle = (hash and 0x7fffffff) * (2.0 * kotlin.math.PI / 0x7fffffff)
            return Pair(kotlin.math.cos(angle), kotlin.math.sin(angle))
        }

        private fun mix(
            seed: Long,
            x: Long,
            y: Long,
        ): Long {
            var h = seed xor (x * 374761393L)
            h = h xor (y * 668265263L)
            h = h xor (h ushr 13)
            return h * 1274126177L
        }

        /** Smoothly interpolates [t] in [0, 1] using the classic 3t² - 2t³ curve. */
        private fun smoothStep(t: Double): Double = t * t * (3 - 2 * t)
    }
}
