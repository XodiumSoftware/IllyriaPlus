package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bulwark: heave a rugged rampart of stone up between the caster and danger.
 *
 * A curved wall of cobblestone and packed earth arcs in front of the player, one block
 * thick and two to four blocks high. The bulwark blocks projectiles and movement, then
 * crumbles back to dust after [CRUMBLE_AFTER_TICKS]. Only air and water cells are
 * displaced; existing solid blocks make the wall hug around them.
 *
 * Wall width, height, and crumble delay scale with level.
 */
internal object Bulwark : AbilityInterface {
    override val cooldownMs: Long = 9000

    /** Horizontal distance ahead of the caster where the wall is raised. */
    private const val SPAWN_DISTANCE = 2.5

    /** Half-width of the wall's arc in blocks (before level scaling). */
    private const val HALF_WIDTH = 2

    /** Additional half-width every two levels. */
    private const val WIDTH_PER_TWO_LEVELS = 1

    /** Base wall height in blocks (before level scaling). */
    private const val HEIGHT = 3

    /** Additional height every other level. */
    private const val HEIGHT_PER_TWO_LEVELS = 1

    /** Wall arc extent in radians (curvature across the width). */
    private const val ARC_RADIANS = PI / 3.5

    /** How long the wall stands before crumbling, in ticks. */
    private const val CRUMBLE_AFTER_TICKS = 240L

    /** Additional crumble delay in ticks per level. */
    private const val CRUMBLE_PER_LEVEL = 40L

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
        val side = Vector(-forward.z, 0.0, forward.x)

        val halfWidth = HALF_WIDTH + (level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val height = HEIGHT + (level - 1) / 2 * HEIGHT_PER_TWO_LEVELS
        val crumbleAfter = CRUMBLE_AFTER_TICKS + (level - 1) * CRUMBLE_PER_LEVEL

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
                    .add(forward.clone().multiply(depth * 0.3))

            for (vertical in 0 until height) {
                val block =
                    world.getBlockAt(
                        base.blockX,
                        base.blockY + vertical,
                        base.blockZ,
                    )
                if (block.type == Material.AIR || block.type == Material.WATER || block.type == Material.SNOW) {
                    // Lower rows are packed dirt; the top rows are cobblestone for a
                    // battlements look.
                    block.type = if (vertical < height - 1) Material.DIRT else Material.COBBLESTONE
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
        world.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f)
        world.playSound(center, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.0f)
        world.spawnParticle(
            Particle.BLOCK,
            center.clone().add(0.0, 1.5, 0.0),
            36,
            1.6,
            1.2,
            1.6,
            0.04,
            Material.COBBLESTONE.createBlockData(),
        )

        scheduleCrumble(placed, crumbleAfter)
    }

    /**
     * Schedules the wall to crumble back to air after [delay] ticks. Cells that have
     * since been replaced by something else are left untouched.
     */
    private fun scheduleCrumble(
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
                        block.world.playSound(block.location, Sound.BLOCK_GRAVEL_FALL, 0.6f, 0.9f)
                        block.type = Material.AIR
                    }
                }
            },
            delay,
        )
    }
}
