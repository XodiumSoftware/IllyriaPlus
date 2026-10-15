package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * FrostBreath: exhale a chilling cone that damages, slows, and freezes the ground.
 *
 * The caster breathes a freezing plume ahead; entities caught in the cone take damage
 * and a brief Slowness effect. Water blocks along the path flash-freeze into
 * [Material.FROSTED_ICE]; existing glass is frost-coated (visual only — left as glass).
 * Fires in the cone are snuffed out.
 *
 * Cone angle and damage scale with level.
 */
internal object FrostBreath : AbilityInterface {
    override val cooldownMs: Long = 4500
    override val activationType = ActivationTypeEnum.INSTANT

    /** Cone reach in blocks. */
    private const val RANGE = 7.0

    /** Cone half-angle in degrees (widens with level). */
    private const val CONE_ANGLE_DEG = 28.0

    /** Damage in hearts dealt to caught entities (before level scaling). */
    private const val DAMAGE = 1.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Slowness effect duration in ticks. */
    private const val SLOWNESS_TICKS = 60

    /** Slowness amplifier (0 = Slowness I). */
    private const val SLOWNESS_AMPLIFIER = 1

    /** Horizontal knockback applied to caught entities. */
    private const val KNOCKBACK = 0.4

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.1

    /** Particle density step along each ray. */
    private const val STEP = 0.4

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        val angleRad = Math.toRadians(CONE_ANGLE_DEG + (level - 1) * 4.0)
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL

        render(origin, direction, angleRad)
        world.playSound(origin, Sound.ENTITY_CREAKING_FREEZE, 1.0f, 1.3f)
        world.playSound(origin, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.6f)

        freeze(origin, direction, angleRad)
        damageAndSlow(player, origin, direction, angleRad, damage)

        complete(player)
    }

    /**
     * Renders the frosty cone with white/blue particles.
     */
    private fun render(
        origin: Location,
        direction: Vector,
        angleRad: Double,
    ) {
        val world = origin.world ?: return
        val axisY = Vector(0, 1, 0)
        val side = axisY.clone().crossProduct(direction).normalize()
        val up = direction.clone().crossProduct(side).normalize()

        val rays = 16
        for (i in 0 until rays) {
            val ringAngle = i * (2.0 * PI / rays)
            val coneDir =
                direction
                    .clone()
                    .multiply(cos(angleRad * Math.random() * 0.5))
                    .add(side.clone().multiply(sin(angleRad * Math.random()) * cos(ringAngle) * 0.5))
                    .add(up.clone().multiply(sin(angleRad * Math.random()) * sin(ringAngle) * 0.5))
                    .normalize()

            var d = STEP
            while (d <= RANGE) {
                val point = origin.clone().add(coneDir.clone().multiply(d))
                world.spawnParticle(Particle.SNOWFLAKE, point, 2, 0.05, 0.05, 0.05, 0.0)
                if (Math.random() < 0.25) {
                    world.spawnParticle(Particle.CLOUD, point, 1, 0.0, 0.0, 0.0, 0.0)
                }
                d += STEP
            }
        }
    }

    /**
     * Freezes water blocks in the cone into frosted ice; snuffs out fires.
     */
    private fun freeze(
        origin: Location,
        direction: Vector,
        angleRad: Double,
    ) {
        val world = origin.world ?: return
        for (i in 1..RANGE.toInt()) {
            val center = origin.clone().add(direction.clone().multiply(i.toDouble()))
            val radius = i * tan(angleRad)
            for (x in -radius.toInt()..radius.toInt()) {
                for (y in -radius.toInt()..radius.toInt()) {
                    for (z in -radius.toInt()..radius.toInt()) {
                        val block = world.getBlockAt(center.blockX + x, center.blockY + y, center.blockZ + z)
                        when (block.type) {
                            Material.WATER -> {
                                block.type = Material.FROSTED_ICE
                            }

                            Material.FIRE -> {
                                block.type = Material.AIR
                            }

                            Material.LAVA -> {
                                block.type = Material.OBSIDIAN
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    /**
     * Damages and slows living entities caught in the cone.
     */
    private fun damageAndSlow(
        player: Player,
        origin: Location,
        direction: Vector,
        angleRad: Double,
        damage: Double,
    ) {
        val world = origin.world ?: return
        world
            .getNearbyEntities(origin, RANGE, RANGE, RANGE)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity = entity.eyeLocation.toVector().subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.5..RANGE) return@filter false
                val cosAngle = toEntity.normalize().dot(direction)
                cosAngle >= cos(angleRad)
            }.forEach { entity ->
                entity.damage(damage, player)
                entity.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER, false, false, true),
                )
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
