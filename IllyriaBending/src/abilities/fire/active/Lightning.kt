package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightning: channel firebending's purest form — a devastating bolt of lightning.
 *
 * Hold sneak to charge the bolt. The longer the charge, the greater the range and damage.
 * Release sneak to unleash lightning in a straight line from the caster's eye, striking:
 *
 *  - Living entities along the path (takes damage, ignition, and heavy knockback).
 *  - The block at the path's end (real lightning + explosion effect, ignites terrain).
 *
 * Charge time, damage, and range scale with level.
 */
internal object Lightning : AbilityInterface {
    override val cooldownMs: Long = 9000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.CHARGE
    override val hasSession: Boolean = true

    /** Damage in hearts dealt at point of strike (before charge factor). */
    private const val DAMAGE = 4.0

    /** Damage multiplier per level. */
    private const val DAMAGE_PER_LEVEL = 0.4

    /** Maximum bolt travel distance in blocks (before charge factor). */
    private const val RANGE = 24.0

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 2.0

    /** Charge time in milliseconds for full power. */
    private const val MAX_CHARGE_TIME_MS = 1800L

    /** Damage and range multiplier at full charge. */
    private const val CHARGE_FACTOR = 1.5

    /** Ticks of fire applied to struck entities. */
    private const val FIRE_TICKS = 100

    /** Knockback strength applied to struck entities. */
    private const val KNOCKBACK = 1.6

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.4

    /** Bolt particle spacing in blocks. */
    private const val STEP = 0.5

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active charging states keyed by player UUID. */
    private val states = mutableMapOf<UUID, LightningState>()

    /** Holds state for a Lightning activation: charging, then fired. */
    private class LightningState(
        val level: Int,
        var charging: Boolean = true,
        val startTime: Long = System.currentTimeMillis(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        if (SessionManager.has(this, player.uniqueId)) return

        states[player.uniqueId] = LightningState(level)
        SessionManager.start(this, player.uniqueId, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        if (!player.isOnline || player.isDead) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        if (state.charging) {
            tickCharging(player, uuid, state)
        }
    }

    /** Ticks while charging. */
    private fun tickCharging(
        player: Player,
        uuid: UUID,
        state: LightningState,
    ) {
        if (!player.isSneaking) {
            fire(player, state)
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        val eye = player.eyeLocation
        val world = eye.world ?: return

        // Swirling charge particles above the player's head.
        val elapsed = System.currentTimeMillis() - state.startTime
        val intensity = (1 + (elapsed / 200)).coerceAtMost(8)
        for (i in 0 until intensity.toInt()) {
            val angle = (elapsed.toDouble() / 50.0) + i * (2.0 * Math.PI / intensity)
            val point =
                eye
                    .clone()
                    .add(0.0, 0.7, 0.0)
                    .add(cos(angle) * 0.5, 0.0, sin(angle) * 0.5)
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.0, 0.0, 0.0, 0.0)
        }

        if (Math.random() < 1.0 / 5.0) {
            world.playSound(eye, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.6f)
        }
    }

    /** Fires the lightning bolt from the charged session. */
    private fun fire(
        player: Player,
        state: LightningState,
    ) {
        val level = state.level
        val eye = player.eyeLocation
        val world = eye.world ?: return

        val elapsed = System.currentTimeMillis() - state.startTime
        val factor =
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

        val direction = eye.direction.clone().normalize()
        val damage = (DAMAGE + (level - 1) * DAMAGE_PER_LEVEL) * factor
        val range = (RANGE + (level - 1) * RANGE_PER_LEVEL) * factor

        // Trace the bolt with small random jitters for visual chaos.
        val hit = traceBolt(player, eye, direction, range, damage)
        renderBolt(eye, direction, range, hit)

        // Real lightning flash + thunder sound at the terminus.
        val terminus = hit ?: eye.clone().add(direction.clone().multiply(range))
        world.strikeLightningEffect(terminus)
        world.playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.1f)

        // Ground burst impact.
        world.spawnParticle(Particle.FLASH, terminus, 1, 0.0, 0.0, 0.0, 0.0)
        world.spawnParticle(Particle.EXPLOSION, terminus, 2, 0.4, 0.4, 0.4, 0.0)
    }

    /**
     * Traces the bolt forward and damages the first living entity it hits.
     *
     * @return The hit entity's location or `null` if no entity was struck.
     */
    private fun traceBolt(
        player: Player,
        eye: Location,
        direction: Vector,
        range: Double,
        damage: Double,
    ): Location? {
        val world = eye.world ?: return null
        var d = 1.0

        while (d <= range) {
            val point = eye.clone().add(direction.clone().multiply(d))
            val block = point.block
            if (!block.isPassable && !block.type.isAir) return null

            val found =
                world
                    .getNearbyEntities(point, 0.9, 0.9, 0.9)
                    .filterIsInstance<LivingEntity>()
                    .firstOrNull { it != player }

            if (found != null) {
                found.damage(damage, player)
                found.fireTicks = FIRE_TICKS
                val shove =
                    direction
                        .clone()
                        .setY(KNOCKUP)
                        .normalize()
                        .multiply(KNOCKBACK)
                found.velocity = found.velocity.add(shove)

                // Chain-lightning shockwave visuals at the strike point.
                world.spawnParticle(Particle.ELECTRIC_SPARK, found.eyeLocation, 32, 0.4, 0.4, 0.4, 0.1)
                world.playSound(found.location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.0f)
                return found.location
            }

            d += STEP
        }
        return null
    }

    /**
     * Renders the bolt as a chain of electric spark particles from eye to the hit location.
     */
    private fun renderBolt(
        eye: Location,
        direction: Vector,
        range: Double,
        hit: Location?,
    ) {
        val world = eye.world ?: return
        val end = hit ?: eye.clone().add(direction.clone().multiply(range))
        val segments = 30

        var prev = eye
        for (i in 1..segments) {
            val t = i.toDouble() / segments
            val target =
                eye
                    .clone()
                    .add(
                        direction.clone().multiply(end.distance(eye) * t),
                    )
            // Random jitter for lightning chaos.
            val jitter =
                Vector(
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.4,
                )
            target.add(jitter)

            val steps = prev.distance(target).coerceAtLeast(0.5) / 0.15
            var s = 0.0
            while (s < steps) {
                val point =
                    prev
                        .clone()
                        .add(
                            target
                                .clone()
                                .subtract(prev)
                                .toVector()
                                .multiply(s / steps),
                        )
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.0, 0.0, 0.0, 0.0)
                s += 1.0
            }
            prev = target
        }
    }
}
