package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.xodium.illyriabending.abilities.AbilityInterface
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireBurst: release an omnidirectional ring of flame from the caster.
 *
 * On activation, a wave of fire erupts outward from the caster in all horizontal
 * directions, dealing damage and igniting nearby entities. Flammable blocks on the
 * perimeter are lit ablaze.
 *
 * Damage and radius scale with level.
 */
internal object FireBurst : AbilityInterface {
    override val cooldownMs: Long = 5000

    /** Burst radius in blocks. */
    private const val RADIUS = 6.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.5

    /** Damage in hearts dealt to caught entities (before level scaling). */
    private const val DAMAGE = 1.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Ticks of fire applied to entities. */
    private const val FIRE_TICKS = 80

    /** Horizontal knockback strength. */
    private const val KNOCKBACK = 1.2

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.35

    /** Chance for flammable blocks on the perimeter to ignite. */
    private const val IGNITE_CHANCE = 0.2

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val center = player.location.clone()
        val world = center.world ?: return
        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL

        render(center, radius)
        igniteGround(center, radius)
        damageNearby(player, center, radius, damage)

        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f)
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f)
    }

    /**
     * Renders the expanding ring of flame particles.
     */
    private fun render(
        center: org.bukkit.Location,
        radius: Double,
    ) {
        val world = center.world ?: return

        val rays = 32
        for (i in 0 until rays) {
            val angle = (2.0 * Math.PI * i) / rays
            for (d in 1..radius.toInt()) {
                val x = center.x + cos(angle) * d
                val z = center.z + sin(angle) * d
                val point = org.bukkit.Location(world, x, center.y + 0.5, z)
                world.spawnParticle(Particle.FLAME, point, 1, 0.0, 0.1, 0.0, 0.0)
            }
        }
    }

    /**
     * Ignites flammable blocks at ground level within the burst radius.
     */
    private fun igniteGround(
        center: org.bukkit.Location,
        radius: Double,
    ) {
        val world = center.world ?: return

        for (x in -radius.toInt()..radius.toInt()) {
            for (z in -radius.toInt()..radius.toInt()) {
                if (x * x + z * z > radius * radius) continue
                val ground =
                    world
                        .getBlockAt(center.blockX + x, center.blockY - 1, center.blockZ + z)
                val above =
                    world
                        .getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                if (above.type.isAir && ground.type.isFlammable && Math.random() < IGNITE_CHANCE) {
                    above.type = org.bukkit.Material.FIRE
                }
            }
        }
    }

    /**
     * Damages and pushes back entities in the burst radius.
     */
    private fun damageNearby(
        player: Player,
        center: org.bukkit.Location,
        radius: Double,
        damage: Double,
    ) {
        val world = center.world ?: return

        world
            .getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(damage, player)
                entity.fireTicks = FIRE_TICKS
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
