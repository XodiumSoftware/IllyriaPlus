package org.xodium.illyriabending.abilities.water.passive

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface

/**
 * HydroSink: pull water under the feet to plunge rapidly downward.
 *
 * On activation, the caster is hurled toward the seafloor with a sharp downward
 * impulse and granted Water Breathing so they can survive the depths while the
 * effect persists. Bubble particles stream upward past them as they sink.
 *
 * Sink strength and breathing duration scale with level.
 */
internal object HydroSink : AbilityInterface {
    override val cooldownMs: Long = 6000

    /** Base downward velocity applied to the caster. */
    private const val SINK_VELOCITY = 1.6

    /** Additional downward velocity per level. */
    private const val SINK_PER_LEVEL = 0.3

    /** Base Water Breathing duration in ticks (before level scaling). */
    private const val BREATH_DURATION_TICKS = 200

    /** Additional Water Breathing duration in ticks per level. */
    private const val BREATH_PER_LEVEL = 60

    /** Number of bubble particles streaming past the caster. */
    private const val PARTICLE_COUNT = 40

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val sink = SINK_VELOCITY + (level - 1) * SINK_PER_LEVEL
        val breathDuration = BREATH_DURATION_TICKS + (level - 1) * BREATH_PER_LEVEL

        player.velocity = Vector(player.velocity.x, -sink, player.velocity.z)

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.WATER_BREATHING,
                breathDuration,
                0,
                false,
                false,
                true,
            ),
        )

        val body = player.location
        player.world.spawnParticle(
            Particle.BUBBLE_COLUMN_UP,
            body,
            PARTICLE_COUNT,
            0.3,
            0.8,
            0.3,
            0.05,
        )
        player.world.spawnParticle(
            Particle.BUBBLE,
            body,
            PARTICLE_COUNT,
            0.4,
            0.6,
            0.4,
            0.05,
        )
        player.world.playSound(body, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 0.9f)
    }
}
