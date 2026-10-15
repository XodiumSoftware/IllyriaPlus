package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType

/**
 * EarthArmor: wrap the caster in a second skin of stone and dust.
 *
 * On activation the caster is encased in compacted earth, granting Resistance and
 * Absorption while shards of gravel and stone swirl around them. Their movements
 * slow noticeably under the weight until the armor sloughs off.
 *
 * Effect duration, absorption hearts, and the resistance amplifier scale with level;
 * the accompanying slowness also lengthens with level, rewarding decisive timing.
 */
internal object EarthArmor : AbilityInterface {
    override val cooldownMs: Long = 20000
    override val activationType = ActivationType.INSTANT

    /** Base effect duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 140

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 30

    /** Base resistance amplifier (0 = Resistance I). */
    private const val RESISTANCE_AMPLIFIER = 0

    /** Additional resistance amplifier every three levels. */
    private const val RESISTANCE_PER_THREE_LEVELS = 1

    /** Base absorption amplifier; each point grants 4 absorption hearts. */
    private const val ABSORPTION_AMPLIFIER = 0

    /** Additional absorption amplifier every other level. */
    private const val ABSORPTION_PER_TWO_LEVELS = 1

    /** Base slowness amplifier (0 = Slowness I). */
    private const val SLOWNESS_AMPLIFIER = 0

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL
        val resistance = RESISTANCE_AMPLIFIER + (level - 1) / 3 * RESISTANCE_PER_THREE_LEVELS
        val absorption = ABSORPTION_AMPLIFIER + (level - 1) / 2 * ABSORPTION_PER_TWO_LEVELS

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
                PotionEffectType.ABSORPTION,
                duration,
                absorption,
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
        val chest = player.location.clone().add(0.0, 1.0, 0.0)
        world.spawnParticle(
            Particle.BLOCK,
            chest,
            40,
            0.5,
            0.6,
            0.5,
            0.04,
            Material.COBBLESTONE.createBlockData(),
        )
        world.spawnParticle(
            Particle.BLOCK,
            chest,
            24,
            0.4,
            0.5,
            0.4,
            0.03,
            Material.GRAVEL.createBlockData(),
        )
        world.playSound(player.location, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f)
        world.playSound(player.location, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.0f)
        complete(player)
    }
}
