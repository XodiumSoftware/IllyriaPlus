package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
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
 * Blaze: charge up, then launch a searing ball of fire.
 *
 * Hold sneak to gather flame — a growing sphere of fire particles swirls in front of the player.
 * Release sneak to hurl the fireball in the player's facing direction. Fully charging multiplies
 * damage and radius by [CHARGE_FACTOR].
 *
 * The projectile ignites flammable blocks and sets living entities on fire on contact.
 */
internal object Blaze : AbilityInterface {
    override val cooldownMs: Long = 4500

    /** Projectile hit radius in blocks (before charge factor). */
    private const val RADIUS = 1.0

    /** Base damage in hearts dealt on direct hit. */
    private const val DAMAGE = 2.0

    /** How many blocks the projectile advances every tick (before charge factor). */
    private const val SPEED = 0.9

    /** Maximum travel distance in blocks. */
    private const val RANGE = 20.0

    /** Ticks of fire applied to ignited entities. */
    private const val FIRE_TICKS = 60

    /** Charge time in milliseconds to reach maximum effect. */
    private const val MAX_CHARGE_TIME_MS = 2000L

    /** Damage, radius, and fire-tick multiplier at full charge. */
    private const val CHARGE_FACTOR = 2.5

    /** Ticks between updates. */
    private const val TICK_PERIOD = 1L

    /** Active sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a single Blaze activation: charging then projectile. */
    private class Session(
        val player: Player,
        val direction: Vector,
        val origin: Location,
        var factor: Double = 1.0,
        var charging: Boolean = true,
        val startTime: Long = System.currentTimeMillis(),
        var projectile: Projectile? = null,
        var task: BukkitTask? = null,
    ) {
        fun cancel() {
            task?.cancel()
        }
    }

    /** The launched fireball: travels forward, rendering particles and damaging on contact. */
    private class Projectile(
        val player: Player,
        origin: Location,
        direction: Vector,
        val radius: Double,
        speed: Double,
        val factor: Double,
    ) {
        /** Current center location. */
        var location: Location = origin.clone()

        /** Normalized travel direction. */
        val dir: Vector = direction.clone().normalize()

        /** Per-tick movement vector. */
        val velocity: Vector = dir.clone().multiply(speed)

        /** Maximum ticks before expiration. */
        val maxTicks: Int = ceil(RANGE / speed).toInt()

        var ticksAlive = 0

        /** Advances one tick. Returns `false` when the projectile should be removed. */
        fun update(): Boolean {
            if (++ticksAlive > maxTicks) return false

            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) {
                igniteBlock()
                return false
            }

            render()
            damageNearby()
            return true
        }

        /** Ignites the block the projectile strikes. */
        private fun igniteBlock() {
            val block = location.block
            if (block.type.isFlammable) {
                block.type = Material.FIRE
            }
        }

        /** Renders the swirling fire effect. */
        private fun render() {
            val count = (10 * radius).toInt()
            for (i in 0 until count) {
                val offset =
                    Vector(
                        (Math.random() - 0.5) * radius,
                        (Math.random() - 0.5) * radius,
                        (Math.random() - 0.5) * radius,
                    )
                location.world?.spawnParticle(
                    Particle.FLAME,
                    location.clone().add(offset),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
            }
            location.world?.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.0f)
        }

        /** Damages and ignites entities in the fireball's path. */
        private fun damageNearby() {
            location
                .world
                ?.getNearbyEntities(location, radius, radius, radius)
                ?.filterIsInstance<LivingEntity>()
                ?.filter { it != player }
                ?.forEach { entity ->
                    entity.damage(DAMAGE * factor, player)
                    entity.fireTicks = (FIRE_TICKS * factor).toInt()
                }
        }
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val direction =
            player
                .location
                .direction
                .clone()
                .normalize()
        val origin =
            player
                .eyeLocation
                .clone()
                .add(direction.clone())

        val session = Session(player, direction, origin)
        sessions[player.uniqueId] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a session by one tick. Called every [TICK_PERIOD] ticks. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        if (session.charging) {
            tickCharging(session)
        } else {
            tickProjectile(session)
        }
    }

    /** Ticks while charging. */
    private fun tickCharging(session: Session) {
        val player = session.player

        if (!player.isSneaking) {
            launch(session)
            return
        }

        val elapsed = System.currentTimeMillis() - session.startTime
        val timeFactor = minOf(1.0, elapsed.toDouble() / MAX_CHARGE_TIME_MS)
        val r = RADIUS * (1.0 + timeFactor)

        for (i in 0 until (12 * r).toInt()) {
            val angle = (2.0 * PI * i) / (12 * r)
            val x = r * cos(angle)
            val z = r * sin(angle)
            session.origin.world?.spawnParticle(
                Particle.FLAME,
                session.origin.clone().add(x, 0.0, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }

        session.origin.world?.playSound(session.origin, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
    }

    /** Ticks while the projectile is in flight. */
    private fun tickProjectile(session: Session) {
        val projectile =
            session.projectile ?: run {
                remove(session.player.uniqueId)
                return
            }

        if (!projectile.update()) {
            remove(session.player.uniqueId)
        }
    }

    /** Launches the fireball from a charging session. */
    private fun launch(session: Session) {
        val elapsed = System.currentTimeMillis() - session.startTime
        session.factor =
            when {
                elapsed >= MAX_CHARGE_TIME_MS -> {
                    CHARGE_FACTOR
                }

                elapsed > 0.3 * MAX_CHARGE_TIME_MS -> {
                    1.0 +
                        (CHARGE_FACTOR - 1.0) *
                        elapsed.toDouble() /
                        MAX_CHARGE_TIME_MS
                }

                else -> {
                    1.0
                }
            }

        session.charging = false
        session.projectile =
            Projectile(
                session.player,
                session.origin,
                session.direction,
                RADIUS * session.factor * 0.5,
                SPEED * session.factor * 0.5,
                session.factor,
            )
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.cancel()
    }
}
