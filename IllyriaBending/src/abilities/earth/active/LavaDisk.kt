package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * LavaDisk: hurl a whirling discus of molten rock.
 *
 * The caster conjures a flat spinning disk of lava and lets it fly in the facing
 * direction. The disk scorches through the air, igniting living entities it connects
 * with and leaving splash burns on terrain it shatters against. The hotter the
 * caster's focus, the larger and longer-lived the disk.
 *
 * Disk radius, speed, reach, and burn duration scale with level.
 */
internal object LavaDisk : AbilityInterface {
    override val cooldownMs: Long = 7000

    /** Disk ring radius in blocks (before level scaling). */
    private const val RADIUS = 0.9

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.15

    /** Damage in hearts on contact (before level scaling). */
    private const val DAMAGE = 2.25

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.3

    /** Fire ticks applied to ignited entities. */
    private const val FIRE_TICKS = 70

    /** Additional fire ticks per level. */
    private const val FIRE_PER_LEVEL = 15

    /** Horizontal knockback applied on impact. */
    private const val KNOCKBACK = 0.65

    /** Vertical knockback component applied on impact. */
    private const val KNOCKUP = 0.2

    /** Per-tick travel speed in blocks. */
    private const val SPEED = 0.95

    /** Maximum travel range in blocks. */
    private const val RANGE = 18.0

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active disk sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a single launched LavaDisk. */
    private class Session(
        val player: Player,
        val level: Int,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

        val session = Session(player, level)
        sessions[uuid] = session

        player.world.playSound(player.eyeLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f)
        player.world.playSound(player.eyeLocation, Sound.BLOCK_LAVA_POP, 0.8f, 1.0f)

        val direction =
            player
                .location
                .direction
                .clone()
                .normalize()
        val origin = player.eyeLocation.clone().add(direction.clone())

        val disk =
            Disk(
                player,
                origin,
                direction,
                RADIUS + (level - 1) * RADIUS_PER_LEVEL,
                DAMAGE + (level - 1) * DAMAGE_PER_LEVEL,
                FIRE_TICKS + (level - 1) * FIRE_PER_LEVEL,
            )

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (!player.isOnline || player.isDead) {
                        remove(uuid)
                        return@Runnable
                    }
                    if (!disk.update()) remove(uuid)
                },
                0L,
                TICK_PERIOD,
            )
    }

    /** The flying molten-disk projectile. */
    private class Disk(
        val player: Player,
        origin: Location,
        direction: Vector,
        val radius: Double,
        val damage: Double,
        val fireTicks: Int,
    ) {
        /** Current disk center. */
        var location: Location = origin.clone()

        /** Normalized travel direction. */
        val dir: Vector = direction.clone().normalize()

        /** Per-tick movement vector. */
        val velocity: Vector = dir.clone().multiply(SPEED)

        /** Maximum ticks before expiration. */
        val maxTicks: Int = ceil(RANGE / SPEED).toInt()

        var ticksAlive = 0
        var rotation = 0.0

        /** Advances the disk one tick. Returns `false` when it should be removed. */
        fun update(): Boolean {
            if (++ticksAlive > maxTicks) return false

            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) {
                impact(block)
                return false
            }
            if (block.isLiquid) {
                if (block.type == Material.WATER) {
                    // Quenched: drop to harmless puffs of smoke.
                    location.world?.spawnParticle(Particle.LARGE_SMOKE, location, 12, 0.2, 0.2, 0.2, 0.02)
                }
                return false
            }

            render()
            return !checkHit()
        }

        /** Renders a spinning ring of lava and ember particles. */
        private fun render() {
            rotation += PI / 16
            val world = location.world ?: return
            val axisY = Vector(0, 1, 0)
            val rotAxis = axisY.clone().crossProduct(dir).normalize()
            val count = ceil(radius * 14.0).toInt().coerceAtLeast(4)

            for (i in 0 until count) {
                val angle = rotation + i * (2.0 * PI / count)
                val offset = rotate(dir.clone().multiply(radius), rotAxis, angle)
                val point = location.clone().add(offset)
                world.spawnParticle(Particle.LAVA, point, 1, 0.0, 0.0, 0.0, 0.0)
                world.spawnParticle(Particle.FLAME, point, 1, 0.0, 0.0, 0.0, 0.0)
            }

            if (Math.random() < 1.0 / 5.0) {
                world.playSound(location, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 1.4f)
            }
        }

        /** Damages and ignites the first living entity the disk overlaps. */
        private fun checkHit(): Boolean {
            val world = location.world ?: return false
            val entity =
                world
                    .getNearbyEntities(location, radius, radius, radius)
                    .filterIsInstance<LivingEntity>()
                    .firstOrNull { it != player }
                    ?: return false
            entity.damage(damage, player)
            entity.fireTicks = fireTicks
            val kb =
                dir
                    .clone()
                    .setY(KNOCKUP)
                    .normalize()
                    .multiply(KNOCKBACK)
            entity.velocity = entity.velocity.add(kb)
            world.playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f)
            return true
        }

        /** Leaves a scorched impact at the disk's terminal block. */
        private fun impact(block: Block) {
            val world = block.world
            world.spawnParticle(
                Particle.LAVA,
                block.location.clone().add(0.5, 0.5, 0.5),
                20,
                0.3,
                0.3,
                0.3,
                0.05,
            )
            world.spawnParticle(
                Particle.LARGE_SMOKE,
                block.location.clone().add(0.5, 0.5, 0.5),
                12,
                0.2,
                0.2,
                0.2,
                0.03,
            )
            world.playSound(block.location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.9f)

            // Splash-burn anything flammable we shattered against.
            if (block.type.isFlammable) {
                block.type = Material.FIRE
            }
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }

    /**
     * Rotates [vec] around [axis] by [angle] radians using Rodrigues' rotation formula.
     *
     * @param vec The vector to rotate.
     * @param axis The rotation axis; will be normalized before use.
     * @param angle The rotation angle in radians.
     * @return The rotated vector.
     */
    private fun rotate(
        vec: Vector,
        axis: Vector,
        angle: Double,
    ): Vector {
        val k = axis.clone().normalize()
        val cosA = cos(angle)
        val sinA = sin(angle)
        val term1 = vec.clone().multiply(cosA)
        val term2 = k.clone().crossProduct(vec).multiply(sinA)
        val term3 = k.clone().multiply(k.dot(vec) * (1.0 - cosA))
        return term1.add(term2).add(term3)
    }
}
