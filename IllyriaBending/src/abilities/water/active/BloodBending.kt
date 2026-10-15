package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
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
 * BloodBending: seize the water in a living creature's body to puppet them.
 *
 * Requires high mastery — the ability fizzles when cast below [MIN_LEVEL]. While
 * sneaking, the player locks onto the nearest living entity in their facing cone and
 * slowly drags it toward (or away from, if the player looks down) them with crimson
 * water tendrils. The victim is slowed and slightly levitated for the channel.
 *
 * Drag strength and channel duration scale with level.
 */
internal object BloodBending : AbilityInterface {
    override val cooldownMs: Long = 15000
    override val activationType = ActivationTypeEnum.DURATION
    override val hasSession = true

    /** Minimum bending level required to cast Bloodbending. */
    private const val MIN_LEVEL = 4

    /** Maximum distance to the target entity (before level scaling). */
    private const val RANGE = 7.0

    /** Additional range per level above the minimum. */
    private const val RANGE_PER_LEVEL = 1.0

    /** Target facing cone half-angle in radians. */
    private const val CONE_ANGLE_RAD = 0.45

    /** Horizontal pull strength per tick applied to the victim (before level scaling). */
    private const val PULL = 0.18

    /** Additional pull per level. */
    private const val PULL_PER_LEVEL = 0.04

    /** Vertical lift applied per tick while held. */
    private const val LIFT = 0.04

    /** Slowness duration (ticks) refreshed on the victim each tick. */
    private const val SLOWNESS_TICKS = 30

    /** Slowness amplifier (3 = Slowness IV; essentially rooted). */
    private const val SLOWNESS_AMPLIFIER = 3

    /** Maximum channel duration in ticks. */
    private const val MAX_DURATION_TICKS = 80L

    /** Rotational increment per tick for the tendril render. */
    private const val ROTATION_STEP = 0.7

    /** Per-player Bloodbending state for active sessions. */
    private val channels = ConcurrentHashMap<UUID, ChannelData>()

    /** Holds state for an active Bloodbending channel. */
    private class ChannelData(
        val player: Player,
        val target: LivingEntity,
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

        if (level < MIN_LEVEL) {
            player.world.playSound(player.location, Sound.BLOCK_WATER_AMBIENT, 0.6f, 0.4f)
            player.world.spawnParticle(
                Particle.SPLASH,
                player.location.clone().add(0.0, 1.2, 0.0),
                12,
                0.4,
                0.4,
                0.4,
                0.0,
            )
            return
        }

        val target = pickTarget(player, level) ?: return
        channels[uuid] = ChannelData(player, target, level)
        SessionManager.start(this, uuid)

        player.world.playSound(player.location, Sound.ENTITY_CREAKING_FREEZE, 0.8f, 0.6f)
        player.world.playSound(target.location, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f)
    }

    override fun tickSession(uuid: UUID) {
        val data = channels[uuid] ?: return
        val player = data.player
        val target = data.target

        data.ticks += 1L
        data.angle += ROTATION_STEP

        if (!player.isOnline ||
            player.isDead ||
            !player.isSneaking ||
            target.isDead ||
            target.isValid.not() ||
            data.ticks > MAX_DURATION_TICKS
        ) {
            channels.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        hold(data)
        render(data)
        playSound(data)
    }

    /**
     * Picks the nearest living entity in the caster's facing cone inside the range.
     */
    private fun pickTarget(
        player: Player,
        level: Int,
    ): LivingEntity? {
        val world = player.world
        val origin = player.eyeLocation
        val direction = origin.direction.clone().normalize()
        val range = RANGE + (level - MIN_LEVEL) * RANGE_PER_LEVEL

        return world
            .getNearbyEntities(origin, range, range, range)
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { !it.isDead }
            .filter { entity ->
                val toEntity = entity.eyeLocation.toVector().subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.8..range) return@filter false
                toEntity.clone().normalize().dot(direction) >= cos(CONE_ANGLE_RAD)
            }.minByOrNull { it.location.distanceSquared(origin) }
    }

    /**
     * Applies the puppeteering motion: slowness, slight lift, and a directional drag
     * along or away from the line of sight.
     */
    private fun hold(data: ChannelData) {
        val player = data.player
        val target = data.target
        val pull = PULL + (data.level - MIN_LEVEL) * PULL_PER_LEVEL

        target.addPotionEffect(
            PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER, false, false, true),
        )

        val eyeDir =
            player
                .eyeLocation
                .direction
                .clone()
                .normalize()
        val playerPos = player.location.clone().add(0.0, 1.0, 0.0)
        val targetPos = target.location.clone().add(0.0, 1.0, 0.0)
        val toCaster =
            playerPos
                .toVector()
                .subtract(targetPos.toVector())
                .setY(0.0)
        val dist = toCaster.length()

        // Looking down pushes the victim away; otherwise drag inward.
        val pushSign = if (eyeDir.y < -0.4) -1.0 else 1.0
        val drag =
            if (dist > 1.5) {
                toCaster
                    .clone()
                    .normalize()
                    .multiply(pull * pushSign)
            } else {
                Vector(0.0, 0.0, 0.0)
            }

        val current = target.velocity
        target.velocity =
            Vector(
                current.x + drag.x,
                (current.y * 0.7).coerceAtMost(LIFT),
                current.z + drag.z,
            )
    }

    /** Renders crimson tendrils arcing from the caster's hands to the victim. */
    private fun render(data: ChannelData) {
        val player = data.player
        val target = data.target
        val world = player.world

        val a = player.eyeLocation.clone().add(0.0, -0.3, 0.0)
        val b = target.eyeLocation.clone().add(0.0, -0.3, 0.0)
        val span = a.distance(b)
        if (span < 0.5) return

        val dir =
            b
                .clone()
                .subtract(a)
                .toVector()
                .normalize()
        val axisY = Vector(0, 1, 0)
        val side = axisY.clone().crossProduct(dir).normalize()
        val up = dir.clone().crossProduct(side).normalize()

        // Two helical tendrils wrapping around the beam.
        for (helix in 0..1) {
            val phase = data.angle + helix * PI
            var t = 0.0
            val step = 0.4
            while (t <= span) {
                val wave = sin(phase + t * 2.0) * 0.25
                val lift2 = cos(phase + t * 2.0) * 0.25
                val point =
                    a
                        .clone()
                        .add(dir.clone().multiply(t))
                        .add(side.clone().multiply(wave))
                        .add(up.clone().multiply(lift2))
                world.spawnParticle(Particle.BUBBLE, point, 1, 0.0, 0.0, 0.0, 0.0)
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.SPLASH, point, 1, 0.02, 0.02, 0.02, 0.0)
                }
                t += step
            }
        }

        // Drips on the victim.
        world.spawnParticle(
            Particle.DRIPPING_WATER,
            target.location.clone().add(0.0, 0.3, 0.0),
            2,
            0.3,
            0.1,
            0.3,
            0.0,
        )
    }

    /** Plays periodic creaking sounds on the victim. */
    private fun playSound(data: ChannelData) {
        if (Math.random() < 1.0 / 8.0) {
            data.target.world.playSound(
                data.target.location,
                Sound.ENTITY_CREAKING_FREEZE,
                0.5f,
                0.4f,
            )
        }
    }
}
