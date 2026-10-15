package org.xodium.illyriabending.abilities.air.passive

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * AirAgility: draw on the wind to move with unnatural lightness.
 *
 * On activation, the player is granted a burst of Speed and Jump Boost for a short
 * duration while cloud particles swirl at their feet. Passively, an airbender's
 * reflexes let them outpace and outmaneuver their foes.
 *
 * Effect duration and amplifier scale with level.
 */
internal object AirAgility : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.INSTANT

    /** Base potion effect duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 120

    /** Additional effect duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 40

    /** Speed effect amplifier (0 = Speed I). */
    private const val SPEED_AMPLIFIER = 1

    /** Base jump boost amplifier (0 = Jump Boost I); increases every other level. */
    private const val JUMP_AMPLIFIER = 0

    /** Number of cloud particles spawned at the player's feet. */
    private const val PARTICLE_COUNT = 20

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL
        val jumpAmplifier = JUMP_AMPLIFIER + (level - 1) / 2

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED,
                duration,
                SPEED_AMPLIFIER,
                false,
                false,
                true,
            ),
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.JUMP_BOOST,
                duration,
                jumpAmplifier,
                false,
                false,
                true,
            ),
        )

        val feet = player.location
        player.world.spawnParticle(
            Particle.CLOUD,
            feet,
            PARTICLE_COUNT,
            0.3,
            0.1,
            0.3,
            0.02,
        )
        player.world.playSound(feet, Sound.ENTITY_BREEZE_WIND_BURST, 0.6f, 1.6f)

        complete(player)
    }
}
