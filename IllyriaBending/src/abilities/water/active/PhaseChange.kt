package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * PhaseChange: freeze water in a radius around the caster.
 *
 * All water blocks within [RADIUS] blocks of the caster flash-freeze into
 * [Material.FROSTED_ICE]; existing ice upgrades to [Material.PACKED_ICE], and packed
 * ice upgrades to [Material.BLUE_ICE]. The frozen water reverts to liquid after
 * [MELT_AFTER_TICKS]; upgrades to packed/blue ice are permanent.
 *
 * Radius and melt delay scale with level.
 */
internal object PhaseChange : AbilityInterface {
    override val cooldownMs: Long = 7000
    override val activationType = ActivationTypeEnum.TOGGLE

    /** Freeze radius in blocks (before level scaling). */
    private const val RADIUS = 5

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 1

    /** How long frozen water stays frozen before melting, in ticks. */
    private const val MELT_AFTER_TICKS = 240L

    /** Additional melt delay per level. */
    private const val MELT_PER_LEVEL = 40L

    /** Particles spawned per converted block. */
    private const val PARTICLES_PER_BLOCK = 1

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val center = player.location
        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL
        val meltAfter = MELT_AFTER_TICKS + (level - 1) * MELT_PER_LEVEL

        val frozenWater = mutableListOf<Block>()
        var upgraded = 0

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
                    when (block.type) {
                        Material.WATER -> {
                            block.type = Material.FROSTED_ICE
                            frozenWater.add(block)
                        }

                        Material.ICE -> {
                            block.type = Material.PACKED_ICE
                            upgraded++
                        }

                        Material.PACKED_ICE -> {
                            block.type = Material.BLUE_ICE
                            upgraded++
                        }

                        else -> {}
                    }
                }
            }
        }

        if (frozenWater.isEmpty() && upgraded == 0) return

        world.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f)
        world.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.5f, 0.8f)

        (frozenWater.take(30)).forEach {
            world.spawnParticle(
                Particle.SNOWFLAKE,
                it.location.clone().add(0.5, 0.5, 0.5),
                PARTICLES_PER_BLOCK,
                0.3,
                0.3,
                0.3,
                0.0,
            )
        }

        scheduleMelt(frozenWater, meltAfter)

        complete(player)
    }

    /**
     * Melts frozen water cells back to [Material.WATER] after the given delay. Cells
     * that have since been replaced by something other than ice variants are left
     * untouched.
     */
    private fun scheduleMelt(
        frozen: List<Block>,
        meltAfter: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                frozen.forEach { block ->
                    if (block.type == Material.FROSTED_ICE || block.type == Material.ICE) {
                        block.type = Material.WATER
                        block.world.spawnParticle(
                            Particle.DRIPPING_WATER,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            3,
                            0.3,
                            0.3,
                            0.3,
                            0.0,
                        )
                    }
                }
            },
            meltAfter,
        )
    }
}
