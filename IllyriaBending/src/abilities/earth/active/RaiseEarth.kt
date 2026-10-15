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
import org.xodium.illyriabending.abilities.ActivationType

/**
 * RaiseEarth: tear a column of solid ground up from the earth ahead of the caster.
 *
 * The caster stamps forward, pulling a rough monolith of dirt and stone out of the
 * ground a few blocks ahead. Entities standing on the site are launched upward and
 * take moderate impact damage. The column settles back down after [SETTLE_AFTER_TICKS],
 * restoring the terrain to air.
 *
 * Column height, eruption radius, and damage scale with level.
 */
internal object RaiseEarth : AbilityInterface {
    override val cooldownMs: Long = 5500
    override val activationType = ActivationType.INSTANT

    /** Horizontal distance ahead of the caster where the pillar erupts. */
    private const val SPAWN_DISTANCE = 3.0

    /** Maximum vertical search depth when locating the ground surface. */
    private const val MAX_DEPTH = 6

    /** Base column height in blocks (before level scaling). */
    private const val HEIGHT = 3

    /** Additional column height every two levels. */
    private const val HEIGHT_PER_TWO_LEVELS = 1

    /** Base damage in hearts dealt to launched entities. */
    private const val DAMAGE = 1.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Upward launch strength applied to entities above the pillar. */
    private const val KNOCKUP = 0.9

    /** Horizontal knockback applied to launched entities. */
    private const val KNOCKBACK = 0.35

    /** Hit radius around the eruption column. */
    private const val HIT_RADIUS = 1.1

    /** How long the raised pillar stands before settling, in ticks. */
    private const val SETTLE_AFTER_TICKS = 60L

    /** Additional settle delay in ticks per level. */
    private const val SETTLE_PER_LEVEL = 20L

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

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
        val knockup = KNOCKUP + (level - 1) * 0.08
        val settleAfter = SETTLE_AFTER_TICKS + (level - 1) * SETTLE_PER_LEVEL

        val placed = erupt(ground, height)
        if (placed.isEmpty()) return

        world.playSound(ground, Sound.BLOCK_STONE_BREAK, 1.0f, 0.7f)
        world.playSound(ground, Sound.BLOCK_GRAVEL_PLACE, 0.9f, 0.8f)
        damageAndLaunch(player, ground, height, damage, knockup)
        scheduleSettle(placed, settleAfter)
        complete(player)
    }

    /**
     * Walks downward from [origin] until it finds a solid block. Returns the location of
     * the first air/liquid block *above* that solid block — the eruption seat — or null
     * if nothing is reachable.
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
     * Raises a jagged pillar of alternating dirt and cobblestone from [seat] up to
     * [height] blocks, replacing only air, snow, and water. Returns the blocks placed.
     */
    private fun erupt(
        seat: Location,
        height: Int,
    ): List<Block> {
        val world = seat.world ?: return emptyList()
        val placed = mutableListOf<Block>()
        for (i in 0 until height) {
            val block = world.getBlockAt(seat.blockX, seat.blockY + i, seat.blockZ)
            if (!block.isPassable && block.type != Material.WATER) continue
            block.type = if (i % 2 == 0) Material.DIRT else Material.COBBLESTONE
            placed.add(block)
        }

        world.spawnParticle(
            Particle.BLOCK,
            seat.clone().add(0.5, 0.5, 0.5),
            32,
            0.5,
            0.6,
            0.5,
            0.06,
            Material.DIRT.createBlockData(),
        )
        return placed
    }

    /**
     * Damages and launches entities standing within a small radius of the pillar.
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

    /**
     * Schedules the pillar to settle back to air after [delay] ticks. Cells that have
     * since been replaced by something else are left untouched.
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
            delay,
        )
    }
}
