package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * FireBreath: exhale a searing cone of flame that scorches enemies and ignites terrain.
 *
 * The caster breathes a blistering plume ahead; entities caught in the cone take fire damage
 * and are set aflame. Water in the path is turned to steam (removed); flammable terrain has a
 * chance to catch fire.
 *
 * Cone angle and damage scale with level.
 */
internal object FireBreath : AbilityInterface {
    override val cooldownMs: Long = 4500
    override val activationType: ActivationType = ActivationType.INSTANT

    /** Cone reach in blocks. */
    private const val RANGE = 7.0

    /** Cone half-angle in degrees (widens with level). */
    private const val CONE_ANGLE_DEG = 28.0

    /** Damage in hearts dealt to caught entities (before level scaling). */
    private const val DAMAGE = 1.25

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Ticks of fire applied to caught entities. */
    private const val FIRE_TICKS = 100

    /** Horizontal knockback applied to caught entities. */
    private const val KNOCKBACK = 0.4

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.1

    /** Particle density step along each ray. */
    private const val STEP = 0.4

    /** Per-block chance for flammable blocks to ignite. */
    private const val IGNITE_CHANCE = 0.15

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
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f)
        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 0.6f, 0.9f)

        scorch(origin, direction, angleRad)
        damageAndIgnite(player, origin, direction, angleRad, damage)

        complete(player)
    }

    /**
     * Renders the fiery cone with flame and smoke particles.
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
                world.spawnParticle(Particle.FLAME, point, 2, 0.05, 0.05, 0.05, 0.0)
                if (Math.random() < 0.25) {
                    world.spawnParticle(Particle.SMOKE, point, 1, 0.0, 0.0, 0.0, 0.0)
                }
                d += STEP
            }
        }
    }

    /**
     * Evaporates water and ignites flammable blocks in the cone.
     */
    private fun scorch(
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
                        when {
                            block.type == Material.WATER -> {
                                block.type = Material.AIR
                                world.spawnParticle(
                                    Particle.CLOUD,
                                    block.location.clone().add(0.5, 0.5, 0.5),
                                    3,
                                    0.3,
                                    0.3,
                                    0.3,
                                )
                            }

                            block.type.isFlammable && Math.random() < IGNITE_CHANCE -> {
                                block.type = Material.FIRE
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Damages and ignites living entities caught in the cone.
     */
    private fun damageAndIgnite(
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
                entity.fireTicks = FIRE_TICKS
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
