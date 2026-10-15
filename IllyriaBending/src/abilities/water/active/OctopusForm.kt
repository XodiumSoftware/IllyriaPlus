package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * OctopusForm: sprout whip-like water tendrils that spin around the caster.
 *
 * While sneaking, several tendrils of water orbit the player at shoulder height,
 * sweeping anything nearby. Entities hit by the tendrils take damage and are flung
 * outward. The form holds until sneak is released or the maximum duration expires.
 *
 * Tendril count, damage, and radius scale with level.
 */
internal object OctopusForm : AbilityInterface {
    override val cooldownMs: Long = 9000
    override val activationType = ActivationTypeEnum.DURATION
    override val hasSession = true

    /** Tendril reach radius in blocks (before level scaling). */
    private const val RADIUS = 2.4

    /** Additional reach per level. */
    private const val RADIUS_PER_LEVEL = 0.2

    /** Number of tendrils (before level scaling). */
    private const val TENDRILS = 3

    /** Additional tendril every other level. */
    private const val TENDRILS_PER_TWO_LEVELS = 1

    /** Damage in hearts dealt per tendril hit. */
    private const val DAMAGE = 1.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Knockback strength applied to tendril hits. */
    private const val KNOCKBACK = 0.9

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.25

    /** Maximum duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 120L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Damage cooldown applied per entity so the tendrils don't shred every tick. */
    private const val DAMAGE_COOLDOWN_TICKS = 12L

    /** Rotational increment per tick for the tendril orbit. */
    private const val ROTATION_STEP = 0.35

    /** Per-player octopus state for active sessions. */
    private val octopi = ConcurrentHashMap<UUID, OctopusData>()

    /** Holds state for an active OctopusForm. */
    private class OctopusData(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        val lastHit: MutableMap<UUID, Long> = mutableMapOf(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        octopi[uuid] = OctopusData(player, level)
        SessionManager.start(this, uuid)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.1f)
    }

    override fun tickSession(uuid: UUID) {
        val data = octopi[uuid] ?: return
        val player = data.player

        data.ticks += 1L
        data.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (data.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || !player.isSneaking || data.ticks > maxTicks) {
            octopi.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        render(data)
        damage(data)

        if (Math.random() < 1.0 / 8.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_GENERIC_SPLASH,
                0.5f,
                1.3f,
            )
        }
    }

    /** Renders the whirling water tendrils. */
    private fun render(data: OctopusData) {
        val player = data.player
        val world = player.world
        val center = player.location.clone().add(0.0, 1.1, 0.0)
        val radius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL
        val tendrils = TENDRILS + (data.level - 1) / 2 * TENDRILS_PER_TWO_LEVELS

        for (tendrilIndex in 0 until tendrils) {
            val phase = data.angle + tendrilIndex * (2.0 * PI / tendrils)

            // Each tendril sweeps from mid-radius out to full reach in a curved arc.
            var t = radius * 0.4
            while (t <= radius) {
                val wave = sin(phase * 2.0 + t) * 0.15
                val offset =
                    Vector(
                        cos(phase + t * 0.6) * t,
                        wave,
                        sin(phase + t * 0.6) * t,
                    )
                val point = center.clone().add(offset)
                world.spawnParticle(Particle.SPLASH, point, 1, 0.0, 0.0, 0.0, 0.0)
                if (Math.random() < 0.2) {
                    world.spawnParticle(Particle.BUBBLE, point, 1, 0.0, 0.0, 0.0, 0.0)
                }
                t += 0.4
            }
        }
    }

    /** Sweeps entities near the tendril tips with damage and knockback. */
    private fun damage(data: OctopusData) {
        val player = data.player
        val world = player.world
        val center = player.location.clone().add(0.0, 1.1, 0.0)
        val radius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL
        val damage = DAMAGE + (data.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(center, radius + 0.5, 1.5, radius + 0.5)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toVec =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .setY(0.0)
                val dist = toVec.length()
                dist in 0.6..radius
            }.forEach { entity ->
                val lastHit = data.lastHit[entity.uniqueId] ?: 0L
                if (data.ticks - lastHit < DAMAGE_COOLDOWN_TICKS) return@forEach
                data.lastHit[entity.uniqueId] = data.ticks

                entity.damage(damage, player)
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .setY(0.0)
                        .normalize()
                        .multiply(KNOCKBACK)
                entity.velocity =
                    entity
                        .velocity
                        .add(push)
                        .add(Vector(0.0, KNOCKUP, 0.0))

                world.spawnParticle(
                    Particle.SPLASH,
                    entity.location.clone().add(0.0, 1.0, 0.0),
                    6,
                    0.3,
                    0.3,
                    0.3,
                    0.0,
                )
            }
    }
}
