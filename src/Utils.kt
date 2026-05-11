@file:Suppress("ktlint:standard:no-wildcard-imports", "Unused")

package org.xodium.illyriaplus

import com.google.gson.JsonParser
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.*
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
import org.xodium.illyriaplus.pdcs.ItemPDC.selectedSpell
import org.xodium.illyriaplus.pdcs.PlayerPDC.nickname
import org.xodium.illyriaplus.pdcs.PlayerPDC.scoreboardVisibility
import java.net.URI
import javax.imageio.ImageIO
import kotlin.io.encoding.Base64

/** General utilities. */
internal object Utils {
    /** MiniMessage instance for parsing formatted strings. */
    val MM: MiniMessage = MiniMessage.miniMessage()

    /** The standardized prefix for IllyriaPlus messages. */
    val IllyriaPlus.prefix: String
        get() =
            "<gradient:#FFA751:#FFE259>[</gradient><gradient:#CB2D3E:#EF473A>" +
                "${this.javaClass.simpleName}" +
                "</gradient><gradient:#FFE259:#FFA751>]</gradient>"

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

    /** World-related utilities. */
    object WorldUtils {
        /**
         * Gets a string representation of the world's weather.
         *
         * @param thundering Value for thunder.
         * @param storm Value for storm.
         * @param clear Value for clear weather.
         * @return The matching weather string.
         */
        fun World.weather(
            thundering: String,
            storm: String,
            clear: String,
        ): String =
            when {
                isThundering -> thundering
                hasStorm() -> storm
                else -> clear
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

        internal val DANGEROUS =
            setOf(
                Material.LAVA,
                Material.FIRE,
                Material.SOUL_FIRE,
                Material.CACTUS,
                Material.SWEET_BERRY_BUSH,
                Material.CAMPFIRE,
                Material.SOUL_CAMPFIRE,
                Material.MAGMA_BLOCK,
                Material.WITHER_ROSE,
            )

        /**
         * Finds a safe location near this location for a player to stand.
         *
         * Searches in a small box (±2 horizontally, ±3 vertically) for a spot
         * with solid ground, air at feet/head, and no dangerous blocks.
         *
         * @param radius Horizontal search radius in blocks.
         * @param vertical Vertical search range in blocks.
         * @return A safe [Location] or null if none is found.
         */
        fun Location.findSafe(
            radius: Int = 2,
            vertical: Int = 3,
        ): Location? {
            val w = world ?: return null
            val bx = blockX
            val by = blockY
            val bz = blockZ

            for (dy in -vertical..vertical) {
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        val x = bx + dx
                        val y = by + dy
                        val z = bz + dz
                        val feet = w.getBlockAt(x, y, z)
                        val head = w.getBlockAt(x, y + 1, z)
                        val ground = w.getBlockAt(x, y - 1, z)

                        if (isSafe(ground, feet, head)) return Location(w, x + 0.5, y.toDouble(), z + 0.5, yaw, pitch)
                    }
                }
            }
            return null
        }

        private fun isSafe(
            ground: Block,
            feet: Block,
            head: Block,
        ): Boolean =
            (ground.type.isSolid || ground.type == Material.WATER) &&
                !feet.type.isSolid && !head.type.isSolid &&
                feet.type !in DANGEROUS && head.type !in DANGEROUS && ground.type !in DANGEROUS
    }

    /** Item-related utilities. */
    object ItemUtils {
        /**
         * Gets the custom name of an item.
         *
         * @return The serialized custom name, or null.
         */
        @Suppress("UnstableApiUsage")
        fun ItemStack.getCustomName(): String? = getData(DataComponentTypes.CUSTOM_NAME)?.let { MM.serialize(it) }
    }

    /** Player-related utilities. */
    object PlayerUtils {
        private const val FACE_X = 8
        private const val FACE_Y = 8
        private const val FACE_WIDTH = 8
        private const val FACE_HEIGHT = 8
        private const val MAX_COORDINATE = 7
        private const val COLOR_MASK = 0xFF
        private const val BLACK_COLOR = "#000000"
        private const val PIXEL_CHAR = "█"
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8

        /**
         * Generates a MiniMessage string representing the player's face.
         *
         * @param size Output size in pixels.
         * @return The rendered face string.
         */
        fun Player.face(size: Int = 8): String {
            val texturesProp =
                playerProfile.properties.firstOrNull { it.name == "textures" }
                    ?: error("Player has no skin texture")

            val json =
                JsonParser
                    .parseString(Base64.decode(texturesProp.value).decodeToString())
                    .asJsonObject

            val skinUrl =
                json
                    .getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .asString

            val fullImg =
                ImageIO.read(URI.create(skinUrl).toURL())
                    ?: error("Failed to load skin image from URL: $skinUrl")

            val face = fullImg.getSubimage(FACE_X, FACE_Y, FACE_WIDTH, FACE_HEIGHT)
            val scale = FACE_WIDTH.toDouble() / size

            return buildString {
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val px = (x * scale).toInt().coerceAtMost(MAX_COORDINATE)
                        val py = (y * scale).toInt().coerceAtMost(MAX_COORDINATE)
                        val rgb = face.getRGB(px, py)

                        val a = (rgb ushr ALPHA_SHIFT) and COLOR_MASK
                        val r = (rgb shr RED_SHIFT) and COLOR_MASK
                        val g = (rgb shr GREEN_SHIFT) and COLOR_MASK
                        val b = rgb and COLOR_MASK

                        if (a == 0) {
                            append("<color:$BLACK_COLOR>$PIXEL_CHAR</color>")
                        } else {
                            append("<color:#%02x%02x%02x>$PIXEL_CHAR</color>".format(r, g, b))
                        }
                    }
                    append("\n")
                }
            }
        }

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

        /**
         * Applies the correct scoreboard.
         */
        fun Player.applyScoreboard() {
            scoreboard =
                if (scoreboardVisibility) {
                    instance.server.scoreboardManager.newScoreboard
                } else {
                    instance.server.scoreboardManager.mainScoreboard
                }
        }

        /**
         * Sets waypoint color.
         *
         * @param color Optional color.
         */
        fun Player.locator(color: TextColor? = null) {
            waypointColor = color?.let { Color.fromRGB(it.value()) }
            sendActionBar(Component.text("Locator color changed!", color))
        }

        /**
         * Applies nickname to display name.
         */
        fun Player.setNickname() = displayName(MM.deserialize(nickname))

        /**
         * Creates a player head item.
         *
         * @return The head ItemStack.
         */
        @Suppress("UnstableApiUsage")
        fun Player.head(): ItemStack =
            ItemStack.of(Material.PLAYER_HEAD).apply {
                setData(
                    DataComponentTypes.PROFILE,
                    ResolvableProfile.resolvableProfile(playerProfile),
                )
            }
    }
}
