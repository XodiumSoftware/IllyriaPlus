package org.xodium.illyriabending.abilities.water.sequence

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import org.xodium.illyriabending.rotateAround
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WaterGimbal: conjure two orbiting rings of water, then fire them as twin waves.
 *
 * **Phase one:** two rotating rings of splash particles orbit the player — one horizontal,
 * one vertical — for a short duration while the caster charges the technique.
 * **Phase two:** the two rings erupt forward as a dual wave, damaging and shoving
 * entities in a corridor ahead of the caster.
 *
 * Ring radius, wave damage, knockback, and charge duration scale with level.
 */
internal object WaterGimbal : AbilityInterface {
    override val cooldownMs: Long = 8000
    override val activationType = ActivationTypeEnum.CHARGE
    override val hasSession = true

    /** Ring orbit radius in blocks (before level scaling). */
    private const val RADIUS = 1.4

    /** Additional ring radius per level. */
    private const val RADIUS_PER_LEVEL = 0.15

    /** Base number of ticks the rings orbit before launching. */
    private const val CHARGE_TICKS = 40L

    /** Additional charge ticks per level. */
    private const val CHARGE_PER_LEVEL = 10L

    /** Wave damage in hearts (before level scaling). */
    private const val DAMAGE = 2.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.5

    /** Wave knockback strength (before level scaling). */
    private const val KNOCKBACK = 1.2

    /** Additional knockback per level. */
    private const val KNOCKBACK_PER_LEVEL = 0.15

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.3

    /** Wave travel reach in blocks. */
    private const val WAVE_RANGE = 10.0

    /** Hit half-extent for wave collision checks. */
    private const val HIT_RADIUS = 1.2

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Rotation step per tick for the orbiting rings. */
    private const val ROTATION_STEP = PI / 16

    /** Per-player gimbal state for active sessions. */
    private val gimbals = ConcurrentHashMap<UUID, GimbalData>()

    /** Holds state for an active WaterGimbal session: orbiting rings, then launched wave. */
    private class GimbalData(
        val player: Player,
        val level: Int,
        var phase: Phase = Phase.RINGS,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    /** Lifecycle phases of a WaterGimbal activation. */
    private enum class Phase {
        /** The two rings orbit the caster while charging. */
        RINGS,

        /** The twin waves surge forward, damaging entities in front. */
        WAVES,
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        gimbals[uuid] = GimbalData(player, level)
        SessionManager.start(this, uuid)
    }

    override fun tickSession(uuid: UUID) {
        val data = gimbals[uuid] ?: return
        val player = data.player

        data.ticks += TICK_PERIOD
        data.angle += ROTATION_STEP

        if (!player.isOnline || player.isDead) {
            gimbals.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        when (data.phase) {
            Phase.RINGS -> {
                if (!player.isSneaking) {
                    gimbals.remove(uuid)
                    SessionManager.remove(this, uuid)
                    return
                }
                val maxCharge = CHARGE_TICKS + (data.level - 1) * CHARGE_PER_LEVEL
                if (data.ticks >= maxCharge) {
                    data.phase = Phase.WAVES
                    data.ticks = 0
                    launchWaves(data)
                    complete(player)
                } else {
                    renderRings(data)
                }
            }

            Phase.WAVES -> {
                if (data.ticks > TICK_PERIOD) {
                    gimbals.remove(uuid)
                    SessionManager.remove(this, uuid)
                    return
                }
            }
        }
    }

    /** Renders the two orbiting rings around the caster. */
    private fun renderRings(data: GimbalData) {
        val player = data.player
        val world = player.world
        val origin = player.location.clone().add(0.0, 1.1, 0.0)
        val radius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL

        val spokes = 16
        for (i in 0 until spokes) {
            val angle = data.angle + i * (2.0 * PI / spokes)
            val offset = Vector(cos(angle) * radius, 0.0, sin(angle) * radius)
            world.spawnParticle(Particle.SPLASH, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
        }

        val facing =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val rotAxis = facing.clone().crossProduct(Vector(0.0, 1.0, 0.0)).normalize()
        val up = Vector(0.0, 1.0, 0.0)
        for (i in 0 until spokes) {
            val angle = -data.angle + i * (2.0 * PI / spokes)
            val offset = up.clone().multiply(radius).rotateAround(rotAxis, angle)
            world.spawnParticle(
                Particle.BUBBLE,
                origin.clone().add(offset),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }

        if (Math.random() < 1.0 / 8.0) {
            world.playSound(origin, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.6f, 1.2f)
        }
    }

    /** Launches the twin wave attack, damaging and shoving entities in front. */
    private fun launchWaves(data: GimbalData) {
        val player = data.player
        val world = player.world
        val origin = player.eyeLocation.clone()
        val dir = origin.direction.clone().normalize()

        val damage = DAMAGE + (data.level - 1) * DAMAGE_PER_LEVEL
        val knockback = KNOCKBACK + (data.level - 1) * KNOCKBACK_PER_LEVEL

        renderWave(origin, dir)
        world.playSound(origin, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f)

        world
            .getNearbyEntities(origin, WAVE_RANGE, WAVE_RANGE, WAVE_RANGE)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity =
                    entity
                        .eyeLocation
                        .toVector()
                        .subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.5..WAVE_RANGE) return@filter false
                val normalized =
                    toEntity
                        .clone()
                        .normalize()
                normalized.dot(dir) >= 0.85
            }.forEach { entity ->
                entity.damage(damage, player)
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(knockback)
                entity.velocity = entity.velocity.add(push)
            }
    }

    /** Renders the twin wave burst traveling forward. */
    private fun renderWave(
        origin: Location,
        dir: Vector,
    ) {
        val world = origin.world ?: return
        val step = 0.4
        var d = 0.5
        var twist = 0.0

        val axisY = Vector(0.0, 1.0, 0.0)
        val side = axisY.clone().crossProduct(dir).normalize()
        val up = dir.clone().crossProduct(side).normalize()

        while (d <= WAVE_RANGE) {
            twist += 0.5
            val radiusAt = HIT_RADIUS * (1.0 - d / WAVE_RANGE) + 0.3
            val base = origin.clone().add(dir.clone().multiply(d))

            val rays = 8
            for (i in 0 until rays) {
                val angle = twist + i * (2.0 * PI / rays)
                val offset =
                    side
                        .clone()
                        .multiply(cos(angle) * radiusAt)
                        .add(up.clone().multiply(sin(angle) * radiusAt))
                world.spawnParticle(Particle.SPLASH, base.clone().add(offset), 2, 0.05, 0.05, 0.05, 0.0)
                world.spawnParticle(Particle.BUBBLE, base.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
            d += step
        }
    }
}
