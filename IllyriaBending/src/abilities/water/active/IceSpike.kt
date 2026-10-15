package org.xodium.illyriabending.abilities.water.active

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
 * IceSpike: erupt a jagged spike of ice from the ground a few blocks ahead.
 *
 * A spike of packed ice bursts upward at a target location in front of the player,
 * damaging and knocking up any entity standing on it, then shatters back into air a
 * moment later. Water at the site flash-freezes; solid ground below [MAX_DEPTH] is
 * used as the spike's root so the eruption comes from the surface.
 *
 * Spike height, damage, and the knock-up strength scale with level.
 */
internal object IceSpike : AbilityInterface {
    override val cooldownMs: Long = 4500

    /** Horizontal distance ahead of the player where the spike erupts. */
    private const val SPAWN_DISTANCE = 3.5

    /** Maximum vertical search depth when locating the ground surface. */
    private const val MAX_DEPTH = 6

    /** Base spike height in blocks above the ground (before level scaling). */
    private const val HEIGHT = 3

    /** Additional spike height every two levels. */
    private const val HEIGHT_PER_TWO_LEVELS = 1

    /** Base damage in hearts dealt to entities above the eruption. */
    private const val DAMAGE = 2.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Horizontal knockback applied to launched entities. */
    private const val KNOCKBACK = 0.4

    /** Vertical launch strength applied to entities above the spike. */
    private const val KNOCKUP = 0.85

    /** Damage radius around the eruption column. */
    private const val HIT_RADIUS = 1.2

    /** How long the ice spike remains before shattering, in ticks. */
    private const val MELT_DELAY_TICKS = 40L

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
        val target =
            player
                .location
                .clone()
                .add(forward.multiply(SPAWN_DISTANCE))
        val ground = findGround(target) ?: return

        val height = HEIGHT + (level - 1) / 2 * HEIGHT_PER_TWO_LEVELS
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val knockup = KNOCKUP + (level - 1) * 0.1

        val placed = erupt(ground, height)
        if (placed.isNotEmpty()) {
            world.playSound(ground, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.6f)
            world.playSound(ground, Sound.ENTITY_CREAKING_UNFREEZE, 0.5f, 1.2f)
            damageAndLaunch(player, ground, height, damage, knockup)

            // Schedule the spike to shatter back to air.
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    placed.forEach { block ->
                        if (block.type == Material.PACKED_ICE || block.type == Material.ICE) {
                            block.world.spawnParticle(
                                Particle.BLOCK,
                                block.location.clone().add(0.5, 0.5, 0.5),
                                10,
                                0.3,
                                0.3,
                                0.3,
                                0.05,
                                block.blockData,
                            )
                            block.type = Material.AIR
                        }
                    }
                },
                MELT_DELAY_TICKS,
            )
        }
    }

    /**
     * Walks downward from [origin] until it finds a solid block. Returns the location
     * of the first air/liquid block *above* that solid block — the eruption seat — or
     * null if nothing is reachable.
     */
    private fun findGround(origin: Location): Location? {
        val world = origin.world ?: return null
        val base = origin.block
        if (!base.isPassable && !base.isLiquid) {
            // Inside a solid block already — step up one to be safe.
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
     * Erects a vertical column of packed ice from [seat] up to [height] blocks,
     * converting water to ice where applicable. Returns the blocks actually placed.
     */
    private fun erupt(
        seat: Location,
        height: Int,
    ): List<Block> {
        val world = seat.world ?: return emptyList()
        val placed = mutableListOf<Block>()
        for (i in 0 until height) {
            val block = world.getBlockAt(seat.blockX, seat.blockY + i, seat.blockZ)
            // Skip blocks we shouldn't displace (containers, bedrock, etc.).
            if (!block.isPassable && block.type != Material.WATER) continue
            block.type = Material.PACKED_ICE
            placed.add(block)
        }

        // Eruption burst.
        world.spawnParticle(
            Particle.BLOCK,
            seat.clone().add(0.5, 0.5, 0.5),
            24,
            0.4,
            0.4,
            0.4,
            0.05,
            Material.PACKED_ICE.createBlockData(),
        )
        world.spawnParticle(
            Particle.SNOWFLAKE,
            seat.clone().add(0.5, 0.5, 0.5),
            12,
            0.4,
            0.4,
            0.4,
            0.01,
        )
        return placed
    }

    /**
     * Damages and launches entities standing within a small radius of the spike column.
     */
    private fun damageAndLaunch(
        player: Player,
        seat: Location,
        height: Int,
        damage: Double,
        knockup: Double,
    ) {
        val world = seat.world ?: return
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
                        .let { if (it.lengthSquared() < 1.0e-4) Vector(0.0, 0.0, 0.0) else it.normalize() }
                entity.velocity =
                    entity
                        .velocity
                        .add(away.multiply(KNOCKBACK))
                        .add(Vector(0.0, knockup, 0.0))
            }
    }
}
