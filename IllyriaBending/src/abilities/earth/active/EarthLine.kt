package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface

/**
 * EarthLine: trace a jagged line of earth spikes along the ground ahead of the caster.
 *
 * Starting at the caster's feet and rolling straight forward, each [SEGMENT_SPACING]
 * blocks along the line a short burst of stone and dirt juts up from the ground,
 * damaging and stumbling anyone standing on it. The spikes sink back after
 * [SETTLE_AFTER_TICKS], restoring the terrain.
 *
 * Line length, spike height, and damage scale with level.
 */
internal object EarthLine : AbilityInterface {
    override val cooldownMs: Long = 6000

    /** Maximum line length in blocks (before level scaling). */
    private const val LENGTH = 10.0

    /** Additional line length per level. */
    private const val LENGTH_PER_LEVEL = 2.0

    /** Horizontal spacing between successive spike columns, in blocks. */
    private const val SEGMENT_SPACING = 1.5

    /** Spike height above the ground in blocks. */
    private const val SPIKE_HEIGHT = 2

    /** Damage per spike eruption in hearts (before level scaling). */
    private const val DAMAGE = 1.25

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.2

    /** Vertical knock applied to entities above an erupting spike. */
    private const val KNOCKUP = 0.5

    /** Hit radius around each spike. */
    private const val HIT_RADIUS = 0.9

    /** How long spikes remain before settling back, in ticks. */
    private const val SETTLE_AFTER_TICKS = 50L

    /** Additional settle delay in ticks per level. */
    private const val SETTLE_PER_LEVEL = 10L

    /** Ticks between successive spike eruptions along the line. */
    private const val ERUPT_INTERVAL_TICKS = 1L

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val world = player.world
        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val length = LENGTH + (level - 1) * LENGTH_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val settleAfter = SETTLE_AFTER_TICKS + (level - 1) * SETTLE_PER_LEVEL

        val origin = player.location.clone().add(forward.clone().multiply(SEGMENT_SPACING))
        world.playSound(origin, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 0.9f)

        val placed = mutableListOf<Block>()

        // Erupt spikes progressively along the line for a rolling feel.
        val totalSegments = (length / SEGMENT_SPACING).toInt().coerceAtLeast(1)
        for (segment in 0 until totalSegments) {
            val delay = segment * ERUPT_INTERVAL_TICKS
            val offset = SEGMENT_SPACING * (segment + 1)
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    val seat =
                        findGround(player.location.clone().add(forward.clone().multiply(offset))) ?: return@Runnable
                    val newBlocks = erupt(seat, damage, player)
                    placed += newBlocks
                    if (segment == totalSegments - 1 && placed.isNotEmpty()) {
                        scheduleSettle(placed, settleAfter)
                    }
                },
                delay,
            )
        }
    }

    /**
     * Walks downward from [origin] until it finds a solid block; returns the location of
     * the first air block above it, or null if nothing is reachable.
     */
    private fun findGround(origin: Location): Location? {
        val world = origin.world ?: return null
        val base = origin.block
        if (!base.isPassable && !base.isLiquid) {
            return base.location.clone().add(0.0, 1.0, 0.0)
        }
        for (i in 0 until 8) {
            val below = world.getBlockAt(base.x, base.y - i, base.z)
            if (!below.isPassable && !below.isLiquid) {
                return world.getBlockAt(base.x, base.y - i + 1, base.z).location
            }
        }
        return null
    }

    /**
     * Erupts a single spike column of dirt and cobble at [seat], damaging any entity
     * standing on it. Returns the blocks actually placed.
     */
    private fun erupt(
        seat: Location,
        damage: Double,
        player: Player,
    ): List<Block> {
        val world = seat.world ?: return emptyList()
        val placed = mutableListOf<Block>()
        for (i in 0 until SPIKE_HEIGHT) {
            val block = world.getBlockAt(seat.blockX, seat.blockY + i, seat.blockZ)
            if (!block.isPassable && block.type != Material.WATER) continue
            block.type = if (i % 2 == 0) Material.DIRT else Material.COBBLESTONE
            placed.add(block)
        }

        world.spawnParticle(
            Particle.BLOCK,
            seat.clone().add(0.5, 0.5, 0.5),
            18,
            0.4,
            0.4,
            0.4,
            0.05,
            Material.DIRT.createBlockData(),
        )
        world.playSound(seat, Sound.BLOCK_STONE_BREAK, 0.8f, 0.95f)

        world
            .getNearbyEntities(seat, HIT_RADIUS, SPIKE_HEIGHT + 1.0, HIT_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.location.y >= seat.y - 0.5 && it.location.y <= seat.y + SPIKE_HEIGHT + 0.5 }
            .forEach { entity ->
                entity.damage(damage, player)
                entity.velocity = entity.velocity.add(Vector(0.0, KNOCKUP, 0.0))
            }

        return placed
    }

    /**
     * Schedules all placed spike blocks to settle back to air after [delay] ticks.
     * Cells already replaced by something else are left untouched.
     */
    private fun scheduleSettle(
        placed: List<Block>,
        delay: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                placed.forEach { block ->
                    if (block.type == Material.DIRT || block.type == Material.COBBLESTONE) {
                        block.world.spawnParticle(
                            Particle.BLOCK,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            6,
                            0.3,
                            0.3,
                            0.3,
                            0.04,
                            block.blockData,
                        )
                        block.type = Material.AIR
                    }
                }
            },
            delay,
        )
    }
}
