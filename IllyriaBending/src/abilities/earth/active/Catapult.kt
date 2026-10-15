package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType

/**
 * Catapult: slam a fist of stone up from the ground to hurl the caster skyward.
 *
 * The caster summons a concentrated upwelling of earth beneath their feet, launching
 * them straight up with a burst of dust and loose gravel. Slow-fall padding wraps the
 * landing so the caster survives the descent, but the effect is brief — the bender
 * must still aim to land on something soft.
 *
 * Launch height scales with level.
 */
internal object Catapult : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType = ActivationType.INSTANT

    /** Base upward velocity applied to the caster. */
    private const val LAUNCH_POWER = 1.4

    /** Additional launch velocity per level. */
    private const val LAUNCH_PER_LEVEL = 0.18

    /** Slow-fall duration in ticks, applied so the caster doesn't splatter. */
    private const val SLOW_FALL_TICKS = 80

    /** Additional slow-fall ticks per level. */
    private const val SLOW_FALL_PER_LEVEL = 20

    /** Slow-fall amplifier (0 = Slow Falling I). */
    private const val SLOW_FALL_AMPLIFIER = 0

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val feet = player.location.clone()

        val power = LAUNCH_POWER + (level - 1) * LAUNCH_PER_LEVEL
        val slowFallTicks = SLOW_FALL_TICKS + (level - 1) * SLOW_FALL_PER_LEVEL

        // Visual feedback — the ground buckles and cracks before the launch.
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().subtract(0.0, 0.1, 0.0),
            40,
            0.5,
            0.15,
            0.5,
            0.08,
            Material.DIRT.createBlockData(),
        )
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().subtract(0.0, 0.2, 0.0),
            24,
            0.4,
            0.1,
            0.4,
            0.06,
            Material.COBBLESTONE.createBlockData(),
        )
        world.playSound(feet, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f)
        world.playSound(feet, Sound.BLOCK_STONE_BREAK, 0.9f, 0.8f)

        // Apply launch velocity and cushioning.
        player.velocity = player.velocity.add(Vector(0.0, power, 0.0))
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW_FALLING,
                slowFallTicks,
                SLOW_FALL_AMPLIFIER,
                false,
                false,
                true,
            ),
        )
        complete(player)
    }
}
