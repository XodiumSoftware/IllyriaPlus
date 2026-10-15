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
 * WaterRing: summon a rotating ring of water around the player for defense.
 *
 * While sneaking, a horizontal ring of swirling water particles orbits the caster.
 * Entities that approach the ring's radius take damage and are knocked outward. The
 * ring collapses when the caster stops sneaking or the duration expires.
 *
 * Ring radius, damage, and duration scale with level.
 */
internal object WaterRing : AbilityInterface {
    override val cooldownMs: Long = 7000
    override val activationType = ActivationTypeEnum.DURATION
    override val hasSession = true

    /** Ring orbit radius in blocks (before level scaling). */
    private const val RADIUS = 2.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.25

    /** Damage in hearts dealt to entities touching the ring. */
    private const val DAMAGE = 0.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Knockback strength applied to entities touching the ring. */
    private const val KNOCKBACK = 0.8

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.2

    /** Damage check radius around each ring particle for collisions. */
    private const val HIT_RADIUS = 0.9

    /** Maximum ring duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 120L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 30L

    /** Rotational increment per tick for the ring render. */
    private const val ROTATION_STEP = 0.25

    /** Damage cooldown applied per entity so the ring doesn't shred every tick. */
    private const val DAMAGE_COOLDOWN_TICKS = 10L

    /** Per-player ring state for active sessions. */
    private val rings = ConcurrentHashMap<UUID, RingData>()

    /** Holds state for an active WaterRing. */
    private class RingData(
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

        rings[uuid] = RingData(player, level)
        SessionManager.start(this, uuid)
        player.world.playSound(player.location, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.3f)
    }

    override fun tickSession(uuid: UUID) {
        val data = rings[uuid] ?: return
        val player = data.player

        data.ticks += 1L
        data.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (data.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || !player.isSneaking || data.ticks > maxTicks) {
            rings.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        render(data)
        damage(data)
    }

    /** Renders the spinning ring of water particles. */
    private fun render(data: RingData) {
        val player = data.player
        val world = player.world
        val center = player.location.clone().add(0.0, 0.9, 0.0)
        val radius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL

        val segments = 18
        for (i in 0 until segments) {
            val angle = data.angle + i * (2.0 * PI / segments)
            val offset = Vector(cos(angle) * radius, 0.1 * sin(angle * 3.0), sin(angle) * radius)
            world.spawnParticle(Particle.SPLASH, center.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            if (Math.random() < 0.2) {
                world.spawnParticle(
                    Particle.BUBBLE,
                    center.clone().add(offset.multiply(0.95)),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
            }
        }

        // Drip particles at the top of the ring.
        world.spawnParticle(
            Particle.DRIPPING_WATER,
            center.clone().add(0.0, 0.4, 0.0),
            2,
            radius * 0.5,
            0.1,
            radius * 0.5,
            0.0,
        )
    }

    /** Damages entities near the ring's outer edge. */
    private fun damage(data: RingData) {
        val player = data.player
        val world = player.world
        val center = player.location.clone().add(0.0, 0.9, 0.0)
        val radius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL
        val damage = DAMAGE + (data.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(center, radius + HIT_RADIUS, 1.5, radius + HIT_RADIUS)
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
                dist in (radius - HIT_RADIUS)..(radius + HIT_RADIUS)
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
            }
    }
}
