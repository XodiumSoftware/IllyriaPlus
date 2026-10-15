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
 * WaterWave: ride a curling wave of water that sweeps the caster forward.
 *
 * The caster mounts a horizontal wave that rolls them forward at speed, splashing
 * water particles in a trailing arc. Entities in the wave's path take damage and are
 * knocked aside. The ride ends when the caster stops sneaking, hits a wall, or the
 * maximum ride time expires.
 *
 * Ride speed and duration scale with level.
 */
internal object WaterWave : AbilityInterface {
    override val cooldownMs: Long = 6000
    override val activationType = ActivationType.DURATION
    override val hasSession = true

    /** Horizontal speed per tick (before level scaling). */
    private const val SPEED = 0.55

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.08

    /** Maximum ride duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 80L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Damage in hearts dealt to entities pushed aside by the wave. */
    private const val DAMAGE = 1.0

    /** Knockback strength applied to entities pushed aside. */
    private const val KNOCKBACK = 0.9

    /** Vertical knockback component for entities pushed aside. */
    private const val KNOCKUP = 0.25

    /** Radius in which entities are knocked aside by the wave. */
    private const val PUSH_RADIUS = 1.6

    /** Rotational increment per tick for the wave render. */
    private const val ROTATION_STEP = 0.6

    /** Per-player wave state for active sessions. */
    private val waves = ConcurrentHashMap<UUID, WaveData>()

    /** Holds state for an active WaterWave ride. */
    private class WaveData(
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

        waves[uuid] = WaveData(player, level)
        SessionManager.start(this, uuid)

        player.world.playSound(player.location, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.9f)
    }

    override fun tickSession(uuid: UUID) {
        val data = waves[uuid] ?: return
        val player = data.player

        data.ticks += 1L
        data.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (data.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || data.ticks > maxTicks) {
            waves.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        // Stop if we hit a wall.
        val heading =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val next = player.location.clone().add(heading)
        if (!next.block.isPassable && !next.block.isLiquid) {
            waves.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        propel(data)
        render(data)
        knockback(data)
    }

    /** Moves the player forward with the wave. */
    private fun propel(data: WaveData) {
        val player = data.player
        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val speed = SPEED + (data.level - 1) * SPEED_PER_LEVEL
        val velocity = dir.multiply(speed)
        val current = player.velocity
        val y = if (current.y < 0.0) 0.05 else current.y
        player.velocity = Vector(velocity.x, y, velocity.z)
    }

    /** Renders the curling wave around the player. */
    private fun render(data: WaveData) {
        val player = data.player
        val world = player.world
        val base = player.location.clone()

        val dir =
            base
                .direction
                .clone()
                .setY(0)
                .normalize()
        val side = Vector(-dir.z, 0.0, dir.x)

        // Curling front arc.
        for (i in 0 until 14) {
            val angle = data.angle + i * (2.0 * PI / 14)
            val height = 1.2 + 0.4 * sin(angle)
            val offsetForward = 0.6 + 0.4 * cos(angle)
            val offsetSide = sin(angle * 0.7) * 0.8
            val offset =
                dir
                    .clone()
                    .multiply(offsetForward)
                    .add(side.clone().multiply(offsetSide))
                    .add(Vector(0.0, height, 0.0))
            world.spawnParticle(Particle.SPLASH, base.clone().add(offset), 2, 0.05, 0.05, 0.05, 0.0)
            if (Math.random() < 0.3) {
                world.spawnParticle(
                    Particle.FALLING_WATER,
                    base.clone().add(offset).add(0.0, 0.4, 0.0),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
            }
        }

        // Foam trail behind the player.
        world.spawnParticle(
            Particle.BUBBLE,
            base.clone().add(dir.clone().multiply(-0.4)),
            4,
            0.4,
            0.2,
            0.4,
            0.0,
        )
    }

    /** Knocks entities in front of the wave sideways. */
    private fun knockback(data: WaveData) {
        val player = data.player
        val world = player.world
        val base = player.location.clone()
        val forward =
            base
                .direction
                .clone()
                .setY(0)
                .normalize()

        world
            .getNearbyEntities(base, PUSH_RADIUS + 1.0, 2.0, PUSH_RADIUS + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity =
                    entity
                        .location
                        .toVector()
                        .subtract(base.toVector())
                        .setY(0.0)
                toEntity.length() in 0.4..(PUSH_RADIUS + 1.5) &&
                    toEntity.clone().normalize().dot(forward) > 0.4
            }.forEach { entity ->
                entity.damage(DAMAGE, player)
                val away =
                    entity
                        .location
                        .toVector()
                        .subtract(base.toVector())
                        .setY(0.0)
                        .normalize()
                        .multiply(KNOCKBACK)
                        .add(forward.clone().multiply(0.3))
                entity.velocity =
                    entity
                        .velocity
                        .add(away)
                        .add(Vector(0.0, KNOCKUP, 0.0))
            }
    }
}
