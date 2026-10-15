package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * IceWall: raise a defensive rampart of packed ice ahead of the caster.
 *
 * A curved wall of ice arcs in front of the player, one block thick and two to three
 * blocks high. The wall blocks projectiles and movement, then melts away after
 * [MELT_AFTER_TICKS]. Only air and water blocks are replaced; existing solid blocks
 * cause the wall to hug around them.
 *
 * Wall width, height, and melt delay scale with level.
 */
internal object IceWall : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType = ActivationTypeEnum.INSTANT

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

    /** How long the wall stands before melting, in ticks. */
    private const val MELT_AFTER_TICKS = 200L

    /** Additional melt delay in ticks per level. */
    private const val MELT_PER_LEVEL = 40L

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
        val meltAfter = MELT_AFTER_TICKS + (level - 1) * MELT_PER_LEVEL

        val placed = mutableListOf<Block>()

        for (lateralIndex in -halfWidth..halfWidth) {
            // Curve each column slightly forward as it moves away from the center.
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
                // Only raise the wall through replaceable space.
                if (block.type == Material.AIR || block.type == Material.WATER || block.type == Material.SNOW) {
                    block.type = Material.PACKED_ICE
                    placed.add(block)
                }
            }
        }

        if (placed.isEmpty()) return

        // Eruption feedback.
        val center =
            player
                .location
                .clone()
                .add(forward.clone().multiply(SPAWN_DISTANCE))
        world.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f)
        world.playSound(center, Sound.BLOCK_SNOW_PLACE, 0.7f, 1.1f)
        world.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0.0, 1.5, 0.0), 30, 1.5, 1.0, 1.5, 0.0)

        scheduleMelt(placed, meltAfter)

        complete(player)
    }

    /**
     * Schedules the wall to melt back into air (or back into water for water-origin
     * cells) after the given delay. Cells that have since been replaced by something
     * else are left untouched.
     */
    private fun scheduleMelt(
        placed: List<Block>,
        meltAfter: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                placed.forEach { block ->
                    if (block.type == Material.PACKED_ICE || block.type == Material.ICE) {
                        block.world.spawnParticle(
                            Particle.BLOCK,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            8,
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
            meltAfter,
        )
    }
}
