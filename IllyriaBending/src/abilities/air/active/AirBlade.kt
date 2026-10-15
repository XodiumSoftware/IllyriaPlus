package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import org.xodium.illyriabending.nearbyLivingEntities
import org.xodium.illyriabending.rotateAround
import java.util.UUID
import kotlin.math.PI
import kotlin.math.ceil

/**
 * AirBlade: charge a spinning ring of compressed air, then launch it forward.
 *
 * Hold sneak to charge — a growing ring of air particles appears in front of the player.
 * Release sneak to launch the blade. Fully charging multiplies damage, range, radius, and
 * speed by [CHARGE_FACTOR].
 *
 * The blade travels forward as a flat disc, stepping over one-block obstacles and
 * extinguishing fires it passes through. Entities caught in its path take damage and are
 * knocked back and upward.
 */
internal object AirBlade : AbilityInterface {
    override val cooldownMs: Long = 4000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.CHARGE
    override val hasSession: Boolean = true

    /** Blade ring radius in blocks (before charge factor). */
    private const val RADIUS = 1.2

    /** Damage in hearts dealt by the blade (before charge factor). */
    private const val DAMAGE = 1.5

    /** Horizontal knockback strength applied to hit entities. */
    private const val KNOCKBACK = 0.8

    /** Vertical knockback component applied to hit entities. */
    private const val KNOCKUP = 0.15

    /** Maximum travel distance in blocks (before charge factor). */
    private const val RANGE = 12.0

    /** How many blocks the blade advances every tick (before charge factor). */
    private const val SPEED = 0.8

    /** Damage and range multiplier at full charge. Radius and speed are only halved-affected. */
    private const val CHARGE_FACTOR = 3.0

    /** Ticks between particle renders. */
    private const val TICK_PERIOD = 1L

    /** Current angle for the rotating ring animation, incremented each tick. */
    private var chargingPoint = 0.0

    /** Charge start times keyed by player UUID. */
    private val chargeStartMs = mutableMapOf<UUID, Long>()

    /** Active blade projectiles keyed by player UUID. */
    private val blades = mutableMapOf<UUID, Blade>()

    /**
     * The launched blade projectile: a flat disc that travels forward, rendering particles
     * and damaging entities in its path.
     */
    private class Blade(
        val player: Player,
        origin: Location,
        direction: Vector,
        val radius: Double,
        speed: Double,
        val factor: Double,
    ) {
        /** Current center location of the blade. */
        var location: Location = origin.clone()

        /** Normalized travel direction. */
        val dir: Vector = direction.clone().normalize()

        /** Per-tick movement vector. */
        val velocity: Vector = dir.clone().multiply(speed)

        /** Maximum ticks before the blade expires, derived from effective range. */
        val maxTicks: Int = ceil(RANGE * factor / speed).toInt()

        var ticksAlive = 0
        var rotationAngle = 0.0

        /** Advances the blade one tick. Returns `false` when the blade should be removed. */
        fun update(): Boolean {
            if (++ticksAlive > maxTicks) return false

            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) return false

            val below = location.clone().subtract(0.0, radius + 0.25, 0.0).block
            if (below.type == Material.FIRE) below.type = Material.AIR

            if (block.isLiquid) return false

            render()
            damageNearby()
            return true
        }

        /** Renders the rotating ring of air particles around the blade. */
        private fun render() {
            rotationAngle += PI / 36
            val count = ceil(22.0 * radius).toInt()
            val axisY = Vector(0, 1, 0)
            val rotAxis = axisY.clone().crossProduct(dir).normalize()

            for (i in 0 until count) {
                val angle = rotationAngle + i * (2.0 * PI / count)
                val offset = dir.clone().multiply(radius).rotateAround(rotAxis, angle)
                val point = location.clone().add(offset)
                location.world?.spawnParticle(Particle.CLOUD, point, 1, 0.0, 0.0, 0.0, 0.0)
            }

            if (Math.random() < 1.0 / 8.0) {
                location.world?.playSound(location, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.0f)
            }
        }

        /** Damages and knocks back living entities overlapping the blade disc. */
        private fun damageNearby() {
            location
                .nearbyLivingEntities(radius)
                .filter { it != player }
                .forEach { entity ->
                    entity.damage(DAMAGE * factor, player)
                    val knockDir =
                        dir
                            .clone()
                            .setY(KNOCKUP)
                            .normalize()
                            .multiply(KNOCKBACK)
                    entity.velocity = entity.velocity.add(knockDir)
                }
        }
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        if (SessionManager.has(this, player.uniqueId)) return

        chargeStartMs[player.uniqueId] = System.currentTimeMillis()
        SessionManager.start(this, player.uniqueId, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            SessionManager.remove(this, uuid)
            blades.remove(uuid)
            chargeStartMs.remove(uuid)
            complete(player)
            return
        }

        val blade = blades[uuid]
        if (blade != null) {
            tickBlade(player, blade)
        } else {
            tickCharging(player, uuid)
        }
    }

    /** Ticks while the blade is being charged. */
    private fun tickCharging(
        player: Player,
        uuid: UUID,
    ) {
        val direction =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val maxRadius = RADIUS * CHARGE_FACTOR * 0.5
        val origin =
            player
                .location
                .clone()
                .add(direction.clone())
                .add(0.0, maxRadius, 0.0)

        if (origin.block.isLiquid) {
            SessionManager.remove(this, uuid)
            chargeStartMs.remove(uuid)
            return
        }

        if (player.isSneaking) {
            val elapsed = System.currentTimeMillis() - (chargeStartMs[uuid] ?: return)
            val timeFactor = minOf(0.9, elapsed.toDouble() / 2000L)
            val r = RADIUS * CHARGE_FACTOR * timeFactor * 0.5
            val count = ceil(r * 10).toInt()
            val axisY = Vector(0, 1, 0)
            val rotAxis = axisY.clone().crossProduct(direction).normalize()

            chargingPoint += PI / 36
            for (i in 0 until count) {
                val angle = chargingPoint + i * (2.0 * PI / count)
                val offset = direction.clone().multiply(r).rotateAround(rotAxis, angle)
                origin.world?.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }

            if (Math.random() < 1.0 / 8.0) {
                origin.world?.playSound(origin, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.0f)
            }
        } else {
            launch(player, uuid, direction, origin)
        }
    }

    /** Ticks while the blade is in flight. */
    private fun tickBlade(
        player: Player,
        blade: Blade,
    ) {
        if (!blade.update()) {
            blades.remove(player.uniqueId)
            SessionManager.remove(this, player.uniqueId)
            chargeStartMs.remove(player.uniqueId)
            complete(player)
        }
    }

    /** Launches the blade from a charging session. */
    private fun launch(
        player: Player,
        uuid: UUID,
        direction: Vector,
        origin: Location,
    ) {
        val elapsed = System.currentTimeMillis() - (chargeStartMs[uuid] ?: 0L)
        val factor =
            when {
                elapsed >= 2000L -> CHARGE_FACTOR
                elapsed > 600L -> 1.0 + (CHARGE_FACTOR - 1.0) * elapsed.toDouble() / 2000L
                else -> 1.0
            }

        blades[uuid] =
            Blade(
                player,
                origin,
                direction,
                RADIUS * factor * 0.5,
                SPEED * factor * 0.5,
                factor,
            )
    }
}
