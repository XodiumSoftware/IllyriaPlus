package org.xodium.illyriabending.abilities.earth.passive

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * DensityShift: harden the caster's stance like the earth beneath their feet.
 *
 * Passively, an earthbending master tightens their grip on the loam. On activation,
 * the caster grounds themselves: they gain Resistance and a noticeable Slowness
 * penalty while dust and small stones wick up around their ankles. Rooted and heavy,
 * they shrug off blows that would stagger a normal fighter.
 *
 * Effect duration and the resistance amplifier scale with level.
 */
internal object DensityShift : AbilityInterface {
    override val cooldownMs: Long = 12000
    override val activationType = ActivationTypeEnum.INSTANT

    /** Base potion duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 100

    /** Additional duration per level in ticks. */
    private const val DURATION_PER_LEVEL = 30

    /** Base resistance amplifier (0 = Resistance I). */
    private const val RESISTANCE_AMPLIFIER = 0

    /** Additional resistance amplifier every three levels. */
    private const val RESISTANCE_PER_THREE_LEVELS = 1

    /** Slowness amplifier; the harder the caster roots, the slower they move. */
    private const val SLOWNESS_AMPLIFIER = 0

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL
        val resistance = RESISTANCE_AMPLIFIER + (level - 1) / 3 * RESISTANCE_PER_THREE_LEVELS

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.RESISTANCE,
                duration,
                resistance,
                false,
                false,
                true,
            ),
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                duration,
                SLOWNESS_AMPLIFIER,
                false,
                false,
                true,
            ),
        )

        val world = player.world
        val feet = player.location.clone()
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().add(0.0, 0.1, 0.0),
            24,
            0.4,
            0.15,
            0.4,
            0.04,
            Material.DIRT.createBlockData(),
        )
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().add(0.0, 0.2, 0.0),
            14,
            0.3,
            0.1,
            0.3,
            0.03,
            Material.GRAVEL.createBlockData(),
        )
        world.playSound(feet, Sound.BLOCK_STONE_PLACE, 0.9f, 0.7f)
        world.playSound(feet, Sound.BLOCK_GRAVEL_PLACE, 0.7f, 0.9f)
        complete(player)
    }
}
