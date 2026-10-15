package org.xodium.illyriabending.abilities.fire.sequence

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
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireWheel: summon a blazing wheel of flame beneath the caster and ride it.
 *
 * A vertical disc of spinning fire particles forms around the player's feet and rolls
 * forward along the ground, carrying the player with it. Sneak again while riding to
 * dismount early; otherwise the wheel expires after [MAX_DURATION_TICKS]. Entities in the
 * wheel's path take damage and are set alight.
 *
 * Wheel radius, speed, and duration scale with level.
 */
internal object FireWheel : AbilityInterface {
    override val cooldownMs: Long = 8000

    /** Wheel ring radius in blocks (before level scaling). */
    private const val RADIUS = 1.0

    /** Additional radius every two levels. */
    private const val RADIUS_PER_TWO_LEVELS = 0.25

    /** Horizontal propulsion speed per tick (before level scaling). */
    private const val SPEED = 0.5

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.1

    /** Maximum ride duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 100L

    /** Additional ride duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Damage in hearts per tick to entities the wheel runs into. */
    private const val DAMAGE_PER_TICK = 0.4

    /** Ticks of fire applied to entities the wheel runs into. */
    private const val FIRE_TICKS = 60

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Rotational increment per tick for the spinning wheel render. */
    private const val ROTATION_STEP = PI / 18

    /** Height the wheel rides above the ground. */
    private const val RIDE_HEIGHT = 0.1

    /** Active wheel sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for an active FireWheel ride. */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) {
            remove(uuid)
            return
        }

        val session = Session(player, level)
        sessions[uuid] = session

        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.8f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a wheel session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || session.ticks > maxTicks) {
            remove(uuid)
            return
        }

        propel(session)
        render(session)
        burn(session)
        playSound(session)
    }

    /** Pushes the player horizontally along their facing direction each tick. */
    private fun propel(session: Session) {
        val player = session.player
        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val speed = SPEED + (session.level - 1) * SPEED_PER_LEVEL

        val velocity = dir.multiply(speed)
        val current = player.velocity
        val y = if (current.y < 0.0) RIDE_HEIGHT else current.y
        player.velocity = Vector(velocity.x, y, velocity.z)
    }

    /** Renders the spinning vertical wheel of flame around the player's feet. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val radius = RADIUS + (session.level - 1) / 2 * RADIUS_PER_TWO_LEVELS

        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val rotAxis = dir.clone().crossProduct(Vector(0.0, 1.0, 0.0)).normalize()
        val origin = player.location.clone().add(0.0, radius, 0.0)

        val up = Vector(0.0, 1.0, 0.0)
        val spokes = 16
        for (i in 0 until spokes) {
            val angle = session.angle + i * (2.0 * PI / spokes)
            val offset = rotate(up.clone().multiply(radius), rotAxis, angle)
            world.spawnParticle(Particle.FLAME, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
        }

        val rimRays = 4
        for (i in 0 until rimRays) {
            val angle = session.angle + i * (2.0 * PI / rimRays)
            var d = 0.2
            while (d < radius) {
                val offset = rotate(up.clone().multiply(d), rotAxis, angle)
                world.spawnParticle(Particle.FLAME, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
                d += 0.3
            }
        }

        // Ember accents.
        if (Math.random() < 0.4) {
            world.spawnParticle(Particle.LAVA, origin, 1, 0.3, 0.3, 0.3, 0.0)
        }
    }

    /** Burns entities the wheel runs over. */
    private fun burn(session: Session) {
        val player = session.player

        player
            .world
            .getNearbyEntities(player.location, 1.4, 1.4, 1.4)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(DAMAGE_PER_TICK, player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)
            }
    }

    /** Plays periodic rolling fire sound. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 10.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.5f,
                0.9f,
            )
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
