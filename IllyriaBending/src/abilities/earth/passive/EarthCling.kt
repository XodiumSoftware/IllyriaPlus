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
 * EarthCling: feel the pull of the mountains and scamper along their flanks.
 *
 * Passively, an earthbender's footing on rock is sure-handed. On activation, the
 * caster's hands and feet bind briefly to the earth: they gain a strong Jump Boost
 * and a surge of Speed while small pebbles patter up to meet each step, letting
 * them scale ledges and ridges that would slow anyone else.
 *
 * Effect duration and the jump amplifier scale with level.
 */
internal object EarthCling : AbilityInterface {
    override val cooldownMs: Long = 14000
    override val activationType = ActivationTypeEnum.INSTANT

    /** Base effect duration in ticks (before level scaling). */
    private const val DURATION_TICKS = 120

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 40

    /** Jump boost amplifier (0 = Jump Boost I). */
    private const val JUMP_AMPLIFIER = 1

    /** Additional jump amplifier every three levels. */
    private const val JUMP_PER_THREE_LEVELS = 1

    /** Speed amplifier. */
    private const val SPEED_AMPLIFIER = 0

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val duration = DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL
        val jump = JUMP_AMPLIFIER + (level - 1) / 3 * JUMP_PER_THREE_LEVELS

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.JUMP_BOOST,
                duration,
                jump,
                false,
                false,
                true,
            ),
        )
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

        val world = player.world
        val feet = player.location.clone()
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().add(0.0, 0.1, 0.0),
            18,
            0.4,
            0.2,
            0.4,
            0.04,
            Material.COBBLESTONE.createBlockData(),
        )
        world.playSound(feet, Sound.BLOCK_GRAVEL_STEP, 0.9f, 1.2f)
        world.playSound(feet, Sound.BLOCK_STONE_STEP, 0.8f, 1.0f)
        complete(player)
    }
}
