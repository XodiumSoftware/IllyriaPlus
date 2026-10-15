package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType

/**
 * AirPunch: a short-ranged, lunging jab of compressed air.
 *
 * The caster lunges forward slightly and strikes the closest entity directly ahead,
 * dealing damage and heavy knockback. Range scales with level.
 */
internal object AirPunch : AbilityInterface {
    override val cooldownMs: Long = 1500
    override val activationType: ActivationType = ActivationType.INSTANT

    /** Jab reach in blocks (before level scaling). */
    private const val RANGE = 2.5

    /** Additional reach per level. */
    private const val RANGE_PER_LEVEL = 0.5

    /** Damage in hearts dealt to the target. */
    private const val DAMAGE = 1.5

    /** Knockback strength applied to the target. */
    private const val KNOCKBACK = 1.8

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.3

    /** Self-lunge strength applied to the caster. */
    private const val LUNGE = 0.5

    /** Particle density step along the jab ray. */
    private const val STEP = 0.3

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        val range = RANGE + (level - 1) * RANGE_PER_LEVEL

        world.playSound(origin, Sound.ENTITY_BREEZE_LAND, 1.0f, 1.3f)
        world.playSound(origin, Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.8f)

        render(origin, direction, range)
        strike(player, origin, direction, range, level)

        player.velocity = player.velocity.add(direction.clone().multiply(LUNGE))

        complete(player)
    }

    /**
     * Renders the jab trail of air particles.
     *
     * @param origin The starting point of the punch.
     * @param direction The normalized punch direction.
     * @param range The punch reach in blocks.
     */
    private fun render(
        origin: org.bukkit.Location,
        direction: Vector,
        range: Double,
    ) {
        val world = origin.world ?: return
        var d = 0.5
        while (d <= range) {
            world.spawnParticle(
                Particle.CLOUD,
                origin.clone().add(direction.clone().multiply(d)),
                2,
                0.05,
                0.05,
                0.05,
                0.05,
            )
            d += STEP
        }
    }

    /**
     * Finds and damages the closest living entity directly in front of the player.
     *
     * @param player The casting player.
     * @param origin The starting point of the punch.
     * @param direction The normalized punch direction.
     * @param range The punch reach in blocks.
     * @param level The caster's bending level.
     */
    private fun strike(
        player: Player,
        origin: org.bukkit.Location,
        direction: Vector,
        range: Double,
        level: Int,
    ) {
        val world = origin.world ?: return
        val damageScale = 1.0 + (level - 1) * 0.25

        world
            .getNearbyEntities(origin, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity = entity.location.toVector().subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.5..range) return@filter false
                toEntity.normalize().dot(direction) >= 0.8
            }.minByOrNull { it.location.distanceSquared(origin) }
            ?.let { target ->
                target.damage(DAMAGE * damageScale, player)
                val push =
                    direction
                        .clone()
                        .setY(KNOCKUP)
                        .normalize()
                        .multiply(KNOCKBACK * damageScale)
                target.velocity = target.velocity.add(push)
            }
    }
}
