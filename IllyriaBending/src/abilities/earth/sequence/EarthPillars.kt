package org.xodium.illyriabending.abilities.earth.sequence

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
import org.xodium.illyriabending.abilities.ActivationType
import kotlin.math.cos
import kotlin.math.sin

/**
 * EarthPillars: raise a ring of stone pillars around the caster, one by one.
 *
 * A sequence ability. The caster sweeps their arm in a great circle, and one by one
 * tall pillars of earth punch up out of the ground at evenly spaced points around
 * them — a corral of stone that traps enemies and shields the caster. Each pillar
 * staggers whatever it lifts, then the whole ring collapses into rubble a moment
 * later.
 *
 * Pillar count, height, and damage scale with level.
 */
internal object EarthPillars : AbilityInterface {
    override val cooldownMs: Long = 18000
    override val activationType = ActivationType.INSTANT

    /** Base number of pillars in the ring (before level scaling). */
    private const val PILLARS = 6

    /** Additional pillars every two levels. */
    private const val PILLARS_PER_TWO_LEVELS = 1

    /** Horizontal ring radius in blocks (before level scaling). */
    private const val RING_RADIUS = 4.0

    /** Additional ring radius per level. */
    private const val RING_RADIUS_PER_LEVEL = 0.4

    /** Base pillar height in blocks (before level scaling). */
    private const val HEIGHT = 4

    /** Additional height every two levels. */
    private const val HEIGHT_PER_TWO_LEVELS = 1

    /** Damage dealt to entities atop an erupting pillar (before level scaling). */
    private const val DAMAGE = 1.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.3

    /** Vertical launch applied to entities atop an erupting pillar. */
    private const val KNOCKUP = 0.9

    /** Horizontal knockback applied to entities atop an erupting pillar. */
    private const val KNOCKBACK = 0.4

    /** Hit radius around each pillar column. */
    private const val HIT_RADIUS = 1.0

    /** Maximum vertical search depth when locating the ground surface. */
    private const val MAX_DEPTH = 8

    /** Ticks between successive pillar eruptions. */
    private const val ERUPT_INTERVAL_TICKS = 4L

    /** How long the final pillar stands before everything crumbles, in ticks. */
    private const val FINAL_HOLD_TICKS = 60L

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val pillarCount = PILLARS + (level - 1) / 2 * PILLARS_PER_TWO_LEVELS
        val height = HEIGHT + (level - 1) / 2 * HEIGHT_PER_TWO_LEVELS
        val ringRadius = RING_RADIUS + (level - 1) * RING_RADIUS_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL

        world.playSound(player.location, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f)
        world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.8f)

        val placedBlocks = mutableListOf<Block>()
        val center = player.location.clone()

        for (i in 0 until pillarCount) {
            val angle = i * (2.0 * Math.PI / pillarCount)
            val offset = Vector(cos(angle) * ringRadius, 0.0, sin(angle) * ringRadius)
            val target =
                center
                    .clone()
                    .add(offset)

            // Erupt each pillar with a brief delay so the ring visibly rolls out.
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    val seat = findGround(target) ?: return@Runnable
                    val blocks = eruptPillar(player, seat, height, damage)
                    placedBlocks += blocks
                },
                i * ERUPT_INTERVAL_TICKS,
            )
        }

        // After the last pillar has been standing for FINAL_HOLD_TICKS, crumble everything.
        val teardownDelay = pillarCount * ERUPT_INTERVAL_TICKS + FINAL_HOLD_TICKS
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                placedBlocks.forEach { block ->
                    if (block.type == Material.DIRT || block.type == Material.COBBLESTONE ||
                        block.type == Material.GRAVEL
                    ) {
                        block.world.spawnParticle(
                            Particle.BLOCK,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            8,
                            0.3,
                            0.3,
                            0.3,
                            0.05,
                            block.blockData,
                        )
                        block.type = Material.AIR
                    }
                }
                if (placedBlocks.isNotEmpty()) {
                    world.playSound(
                        placedBlocks[0].location,
                        Sound.BLOCK_GRAVEL_FALL,
                        0.9f,
                        0.7f,
                    )
                }
            },
            teardownDelay,
        )
        complete(player)
    }

    /**
     * Walks downward from [origin] until it finds a solid block, returning the location
     * of the first air block above it — the pillar seat — or null.
     */
    private fun findGround(origin: Location): Location? {
        val world = origin.world ?: return null
        val base = origin.block
        if (!base.isPassable && !base.isLiquid) {
            return base.location.clone().add(0.0, 1.0, 0.0)
        }
        for (i in 0 until MAX_DEPTH) {
            val below = world.getBlockAt(base.x, base.y - i, base.z)
            if (!below.isPassable && !below.isLiquid) {
                return world.getBlockAt(base.x, base.y - i + 1, base.z).location
            }
        }
        return null
    }

    /**
     * Erects a single pillar of alternating dirt/cobble at [seat] and damages launches
     * entities standing on it. Returns the blocks placed.
     */
    private fun eruptPillar(
        player: Player,
        seat: Location,
        height: Int,
        damage: Double,
    ): List<Block> {
        val world = seat.world ?: return emptyList()
        val placed = mutableListOf<Block>()
        for (i in 0 until height) {
            val block = world.getBlockAt(seat.blockX, seat.blockY + i, seat.blockZ)
            if (!block.isPassable && block.type != Material.WATER) continue
            block.type =
                when (i % 3) {
                    0 -> Material.DIRT
                    1 -> Material.COBBLESTONE
                    else -> Material.GRAVEL
                }
            placed.add(block)
        }

        world.spawnParticle(
            Particle.BLOCK,
            seat.clone().add(0.5, 0.5, 0.5),
            24,
            0.4,
            0.6,
            0.4,
            0.06,
            Material.COBBLESTONE.createBlockData(),
        )
        world.playSound(seat, Sound.BLOCK_STONE_BREAK, 0.9f, 0.7f)

        world
            .getNearbyEntities(seat, HIT_RADIUS, height + 1.0, HIT_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.location.y >= seat.y - 0.5 && it.location.y <= seat.y + height + 0.5 }
            .forEach { entity ->
                entity.damage(damage, player)
                val away =
                    entity
                        .location
                        .toVector()
                        .subtract(seat.toVector())
                        .setY(0.0)
                        .let { if (it.lengthSquared() < 1.0e-4) Vector(1.0, 0.0, 0.0) else it.normalize() }
                entity.velocity =
                    entity
                        .velocity
                        .add(away.multiply(KNOCKBACK))
                        .add(Vector(0.0, KNOCKUP, 0.0))
            }

        return placed
    }
}
