package org.xodium.illyriabending.abilities.air.passive

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * GracefulDescent: let the wind carry you gently to the ground.
 *
 * On activation, the player is granted Slow Falling for a short duration while a soft
 * cushion of cloud particles gathers beneath them. An airbender trusts the air to
 * catch them, turning any fall into a graceful glide.
 *
 * Effect duration scales with level.
 */
internal object GracefulDescent : AbilityInterface {
    override val cooldownMs: Long = 6000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.INSTANT

    /** Base slow falling duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 120

    /** Additional effect duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 40

    /** Number of cloud particles spawned beneath the player. */
    private const val PARTICLE_COUNT = 20

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW_FALLING,
                duration,
                0,
                false,
                false,
                true,
            ),
        )

        val feet = player.location.clone().subtract(0.0, 0.3, 0.0)
        player.world.spawnParticle(
            Particle.CLOUD,
            feet,
            PARTICLE_COUNT,
            0.4,
            0.1,
            0.4,
            0.01,
        )
        player.world.playSound(feet, Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 1.8f)

        complete(player)
    }
}
