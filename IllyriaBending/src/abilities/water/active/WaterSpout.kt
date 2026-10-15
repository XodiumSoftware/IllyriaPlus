package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WaterSpout: summon a swirling column of water that lifts the caster upward.
 *
 * A vertical spout of splash and droplet particles rises beneath the player, granting
 * steady vertical lift while sneaking. Nearby entities caught in the column are also
 * pushed up. The spout ends when sneak is released, the caster leaves the water-less
 * ground too far behind, or the maximum duration expires.
 *
 * Lift strength and duration scale with level.
 */
internal object WaterSpout : AbilityInterface {
    override val cooldownMs: Long = 4000
    override val activationType = ActivationType.CHARGE
    override val hasSession = true

    /** Vertical lift applied per tick to the caster (before level scaling). */
    private const val LIFT = 0.16

    /** Additional lift per level. */
    private const val LIFT_PER_LEVEL = 0.03

    /** Lift applied to nearby entities per tick. */
    private const val ENTITY_LIFT = 0.24

    /** Spout column radius affecting nearby entities. */
    private const val COLUMN_RADIUS = 1.6

    /** Maximum spout duration in ticks. */
    private const val MAX_DURATION_TICKS = 120L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Height of the spout column for the render. */
    private const val RENDER_HEIGHT = 3.2

    /** Rotational increment per tick for the render. */
    private const val ROTATION_STEP = 0.5

    /** Per-player spout state for active sessions. */
    private val spouts = ConcurrentHashMap<UUID, SpoutData>()

    /** Holds state for a single WaterSpout channel. */
    private class SpoutData(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        spouts[uuid] = SpoutData(player, level)
        SessionManager.start(this, uuid)
    }

    override fun tickSession(uuid: UUID) {
        val data = spouts[uuid] ?: return
        val player = data.player

        data.ticks += 1L
        data.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (data.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || !player.isSneaking || data.ticks > maxTicks) {
            spouts.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        lift(data)
        render(data)
        playSound(data)
    }

    /** Applies lift to the caster and nearby entities. */
    private fun lift(data: SpoutData) {
        val player = data.player
        val lift = LIFT + (data.level - 1) * LIFT_PER_LEVEL

        val current = player.velocity
        player.velocity = Vector(current.x, lift, current.z)

        val world = player.world
        world
            .getNearbyEntities(player.location, COLUMN_RADIUS, COLUMN_RADIUS, COLUMN_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                val eCurrent = entity.velocity
                entity.velocity = Vector(eCurrent.x, ENTITY_LIFT, eCurrent.z)
            }
    }

    /** Renders the spinning column of water beneath the player. */
    private fun render(data: SpoutData) {
        val player = data.player
        val world = player.world
        val base = player.location.clone().subtract(0.0, 0.5, 0.0)

        var y = 0.0
        while (y <= RENDER_HEIGHT) {
            val radiusAt = 0.7 - (y / RENDER_HEIGHT) * 0.3
            val rays = 5
            for (i in 0 until rays) {
                val angle = data.angle + i * (2.0 * PI / rays) + y * 0.9
                val offset =
                    Vector(
                        cos(angle) * radiusAt,
                        -y,
                        sin(angle) * radiusAt,
                    )
                world.spawnParticle(
                    Particle.SPLASH,
                    base.clone().add(offset),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
                if (Math.random() < 0.2) {
                    world.spawnParticle(
                        Particle.BUBBLE_COLUMN_UP,
                        base.clone().add(offset.multiply(0.8)),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                }
            }
            y += 0.35
        }
    }

    /** Plays periodic water sounds. */
    private fun playSound(data: SpoutData) {
        if (Math.random() < 1.0 / 6.0) {
            data.player.world.playSound(
                data.player.location,
                Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT,
                0.6f,
                1.1f,
            )
        }
    }
}
