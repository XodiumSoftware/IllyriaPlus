package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * HeatControl: command heat itself by reshaping thermal blocks around the caster.
 *
 * On activation, the caster's thermal will radiates outward:
 *
 *  - Ice and snow melt into water.
 *  - Lava solidifies into obsidian.
 *  - Burning flames are snuffed out.
 *  - Water freezes into ice directly under the caster's feet (brief stepping disk).
 *
 * Radius scales with level.
 */
internal object HeatControl : AbilityInterface {
    override val cooldownMs: Long = 4000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.INSTANT

    /** Manipulation radius in blocks. */
    private const val RADIUS = 6

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 1

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val center = player.location
        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL

        var transformed = 0
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue

                    val block =
                        world.getBlockAt(
                            center.blockX + dx,
                            center.blockY + dy,
                            center.blockZ + dz,
                        )

                    val loc = block.location.clone().add(0.5, 0.5, 0.5)
                    when (block.type) {
                        Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.FROSTED_ICE -> {
                            block.type = Material.WATER
                            world.spawnParticle(Particle.DRIPPING_WATER, loc, 4, 0.2, 0.2, 0.2)
                            transformed++
                        }

                        Material.SNOW, Material.SNOW_BLOCK, Material.POWDER_SNOW -> {
                            block.type = Material.AIR
                            world.spawnParticle(Particle.CLOUD, loc, 4, 0.2, 0.2, 0.2)
                            transformed++
                        }

                        Material.LAVA -> {
                            block.type = Material.OBSIDIAN
                            world.spawnParticle(Particle.SMOKE, loc, 6, 0.2, 0.2, 0.2)
                            world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.0f)
                            transformed++
                        }

                        Material.FIRE, Material.SOUL_FIRE -> {
                            block.type = Material.AIR
                            world.spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.2, 0.2)
                            transformed++
                        }

                        else -> {}
                    }
                }
            }
        }

        if (transformed > 0) {
            world.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 1.4f)
        }

        complete(player)
    }
}
