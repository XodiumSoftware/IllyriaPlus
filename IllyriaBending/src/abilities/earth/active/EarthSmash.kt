package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import kotlin.math.cos
import kotlin.math.sin

/**
 * EarthSmash: slam the ground with a titanic stomp that ripples outward.
 *
 * The caster drops their weight and calls up a crushing wave of loose earth that
 * bursts out in a ring around them. Every living entity caught inside the radius is
 * hurled upward, takes heavy damage, and is knocked flat. Stone and dirt particles
 * swirl up in an expanding circle, then the ground rumbles back to silence.
 *
 * Radius, damage, and knockback scale with level.
 */
internal object EarthSmash : AbilityInterface {
    override val cooldownMs: Long = 11000
    override val activationType = ActivationType.INSTANT

    /** Hit radius in blocks (before level scaling). */
    private const val RADIUS = 4.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.6

    /** Damage in hearts dealt to each caught entity (before level scaling). */
    private const val DAMAGE = 2.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.5

    /** Upward launch applied to struck entities. */
    private const val KNOCKUP = 0.7

    /** Horizontal knockback applied to struck entities (before level scaling). */
    private const val KNOCKBACK = 0.6

    /** Additional horizontal knockback per level. */
    private const val KNOCKBACK_PER_LEVEL = 0.1

    /** Number of radial particle rays emitted in the shock ring. */
    private const val RING_RAYS = 28

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val feet = player.location.clone()

        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val knockback = KNOCKBACK + (level - 1) * KNOCKBACK_PER_LEVEL

        // Concentric rings of dust radiating from the caster's feet.
        var ringDistance = 0.4
        var ringIndex = 0
        while (ringDistance <= radius) {
            renderRing(feet, ringDistance, ringIndex)
            ringDistance += 0.45
            ringIndex++
        }

        world.playSound(feet, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f)
        world.playSound(feet, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f)
        world.playSound(feet, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 0.8f)

        // Damage and launch entities in the area.
        world
            .getNearbyEntities(feet, radius, 2.5, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(damage, player)
                val away =
                    entity
                        .location
                        .toVector()
                        .subtract(feet.toVector())
                        .setY(0.0)
                        .let { if (it.lengthSquared() < 1.0e-4) Vector(1.0, 0.0, 0.0) else it.normalize() }
                entity.velocity =
                    entity
                        .velocity
                        .add(away.multiply(knockback))
                        .add(Vector(0.0, KNOCKUP, 0.0))
            }
        complete(player)
    }

    /**
     * Renders one ring of the expanding shockwave at [distance] from the epicenter.
     * The [ringIndex] is used to subtly alternate dirt vs. stone so the wave has a
     * layered look as it rolls outward.
     */
    private fun renderRing(
        center: Location,
        distance: Double,
        ringIndex: Int,
    ) {
        val world = center.world ?: return
        val data =
            if (ringIndex % 2 == 0) {
                Material.DIRT.createBlockData()
            } else {
                Material.COBBLESTONE.createBlockData()
            }

        for (i in 0 until RING_RAYS) {
            val angle = i * (2.0 * Math.PI / RING_RAYS)
            val x = cos(angle) * distance
            val z = sin(angle) * distance
            val point = center.clone().add(x, 0.15, z)
            world.spawnParticle(
                Particle.BLOCK,
                point,
                3,
                0.08,
                0.08,
                0.08,
                0.02,
                data,
            )
        }
    }
}
