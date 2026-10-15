package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireWall: raise a rampart of roaring flame ahead of the caster.
 *
 * A curved wall of flame arcs in front of the player, one block thick and two to three
 * blocks high. Entities that step into the wall are damaged and ignited. The wall blocks
 * movement but not sight, then gutters out after [DURATION_TICKS].
 *
 * Wall width, height, and duration scale with level.
 */
internal object FireWall : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.INSTANT

    /** Horizontal distance ahead of the caster where the wall is raised. */
    private const val SPAWN_DISTANCE = 3.0

    /** Half-width of the wall's arc in blocks (before level scaling). */
    private const val HALF_WIDTH = 2

    /** Additional half-width every two levels. */
    private const val WIDTH_PER_TWO_LEVELS = 1

    /** Base wall height in blocks (before level scaling). */
    private const val HEIGHT = 2

    /** Additional height every other level. */
    private const val HEIGHT_PER_TWO_LEVELS = 1

    /** Wall arc extent in radians (curvature across the width). */
    private const val ARC_RADIANS = PI / 3.0

    /** How long the wall stands before guttering out, in ticks. */
    private const val DURATION_TICKS = 140L

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 30L

    /** Damage in hearts dealt to entities standing in the wall per tick. */
    private const val DAMAGE_PER_TICK = 0.15

    /** Ticks of fire applied to entities that touch the wall. */
    private const val FIRE_TICKS = 60

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
        val side = Vector(-forward.z, 0.0, forward.x)

        val halfWidth = HALF_WIDTH + (level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val height = HEIGHT + (level - 1) / 2 * HEIGHT_PER_TWO_LEVELS
        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL

        val placed = mutableListOf<Block>()

        for (lateralIndex in -halfWidth..halfWidth) {
            val t = lateralIndex.toDouble() / halfWidth.coerceAtLeast(1)
            val swing = sin(t * ARC_RADIANS)
            val depth = cos(t * ARC_RADIANS)

            val base =
                player
                    .location
                    .clone()
                    .add(side.clone().multiply(lateralIndex.toDouble()))
                    .add(forward.clone().multiply(SPAWN_DISTANCE + swing))
                    .add(forward.clone().multiply(depth * 0.4))

            for (vertical in 0 until height) {
                val block =
                    world.getBlockAt(
                        base.blockX,
                        base.blockY + vertical,
                        base.blockZ,
                    )
                if (block.type == Material.AIR || block.type == Material.SNOW) {
                    block.type = Material.FIRE
                    placed.add(block)
                }
            }
        }

        if (placed.isEmpty()) return

        val center =
            player
                .location
                .clone()
                .add(forward.clone().multiply(SPAWN_DISTANCE))
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.9f)
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.9f)
        world.spawnParticle(Particle.FLAME, center.clone().add(0.0, 1.5, 0.0), 40, 1.5, 1.0, 1.5, 0.0)

        burnTouchers(player, placed, duration)
        scheduleExtinguish(placed, duration)

        complete(player)
    }

    /**
     * Periodically burns entities standing inside the wall while it lasts.
     */
    private fun burnTouchers(
        player: Player,
        placed: List<Block>,
        durationTicks: Long,
    ) {
        instance
            .server
            .scheduler
            .runTaskTimer(
                instance,
                Runnable {
                    placed.forEach { block ->
                        if (block.type != Material.FIRE) return@forEach
                        block
                            .world
                            .getNearbyEntities(
                                block.location.clone().add(0.5, 0.5, 0.5),
                                0.7,
                                0.7,
                                0.7,
                            ).filterIsInstance<LivingEntity>()
                            .filter { it != player }
                            .forEach { entity ->
                                entity.damage(DAMAGE_PER_TICK, player)
                                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)
                            }
                    }
                },
                0L,
                5L,
            ).also { task ->
                instance.server.scheduler.runTaskLater(instance, Runnable { task.cancel() }, durationTicks)
            }
    }

    /**
     * Schedules the wall to extinguish back to air after the given delay.
     * Cells that have since been replaced are left untouched.
     */
    private fun scheduleExtinguish(
        placed: List<Block>,
        durationTicks: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                placed.forEach { block ->
                    if (block.type == Material.FIRE) {
                        block.world.spawnParticle(
                            Particle.SMOKE,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            6,
                            0.3,
                            0.3,
                            0.3,
                            0.02,
                        )
                        block.type = Material.AIR
                    }
                }
            },
            durationTicks,
        )
    }
}
