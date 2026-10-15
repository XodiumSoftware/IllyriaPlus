package org.xodium.illyriabending.abilities.water.passive

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * FastSwim: pull currents around the body to slice through water at speed.
 *
 * On activation, the caster is granted a burst of Dolphin's Grace and Speed, greatly
 * boosting underwater mobility for a short duration. Bubble particles trail the caster
 * as they kick off.
 *
 * Effect duration and amplifier scale with level.
 */
internal object FastSwim : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType = ActivationTypeEnum.INSTANT

    /** Base potion effect duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 120

    /** Additional effect duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 40

    /** Dolphin's Grace amplifier (0 = Dolphin's Grace I). */
    private const val GRACE_AMPLIFIER = 1

    /** Base Speed amplifier; increases every other level. */
    private const val SPEED_AMPLIFIER = 0

    /** Number of bubble particles spawned around the caster. */
    private const val PARTICLE_COUNT = 30

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL
        val speedAmplifier = SPEED_AMPLIFIER + (level - 1) / 2

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                duration,
                GRACE_AMPLIFIER,
                false,
                false,
                true,
            ),
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED,
                duration,
                speedAmplifier,
                false,
                false,
                true,
            ),
        )

        val body = player.location
        player.world.spawnParticle(
            Particle.BUBBLE,
            body,
            PARTICLE_COUNT,
            0.4,
            0.6,
            0.4,
            0.05,
        )
        player.world.playSound(body, Sound.ENTITY_DOLPHIN_SWIM, 0.7f, 1.3f)

        complete(player)
    }
}
