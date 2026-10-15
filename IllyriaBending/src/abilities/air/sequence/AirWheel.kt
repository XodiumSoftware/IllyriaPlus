package org.xodium.illyriabending.abilities.air.sequence

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.AbilityInterface.Companion.rotate
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.PI

/**
 * AirWheel: summon a rolling wheel of compressed air beneath the caster and ride it.
 *
 * A vertical disc of spinning air particles forms around the player's feet and rolls
 * forward along the ground, carrying the player with it. Sneak again while riding
 * to dismount early; otherwise the wheel expires after [MAX_DURATION_TICKS].
 *
 * Wheel radius, speed, and duration scale with level.
 */
internal object AirWheel : AbilityInterface {
    override val cooldownMs: Long = 6000
    override val activationType: ActivationType = ActivationType.TOGGLE
    override val hasSession: Boolean = true

    /** Wheel ring radius in blocks (before level scaling). */
    private const val RADIUS = 1.0

    /** Horizontal propulsion speed per tick (before level scaling). */
    private const val SPEED = 0.5

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.1

    /** Maximum ride duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 100L

    /** Additional ride duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Rotational increment per tick for the spinning wheel render. */
    private const val ROTATION_STEP = PI / 18

    /** Height the wheel rides above the ground. */
    private const val RIDE_HEIGHT = 0.1

    /** Per-player ride state. */
    private val rideStates = mutableMapOf<UUID, RideState>()

    /**
     * Tracks ride state for a player.
     */
    private data class RideState(
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId

        if (SessionManager.has(this, uuid)) {
            dismount(uuid)
            return
        }

        if (isOnCooldown(player)) return

        rideStates[uuid] = RideState(level)
        SessionManager.start(this, uuid, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = rideStates[uuid] ?: return

        state.ticks += TICK_PERIOD
        state.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (state.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || state.ticks > maxTicks) {
            dismount(uuid)
            return
        }

        if (!player.isSneaking) {
            dismount(uuid)
            return
        }

        propel(player, state)
        render(player, state)
        playSound(player)
    }

    /** Pushes the player horizontally along their facing direction each tick. */
    private fun propel(
        player: Player,
        state: RideState,
    ) {
        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val speed = SPEED + (state.level - 1) * SPEED_PER_LEVEL

        val velocity = dir.multiply(speed)
        val current = player.velocity
        val y = if (current.y < 0.0) RIDE_HEIGHT else current.y
        player.velocity = Vector(velocity.x, y, velocity.z)
    }

    /** Renders the spinning vertical wheel of air around the player's feet. */
    private fun render(
        player: Player,
        state: RideState,
    ) {
        val world = player.world
        val radius = RADIUS

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
            val angle = state.angle + i * (2.0 * PI / spokes)
            val offset = rotate(up.clone().multiply(radius), rotAxis, angle)
            world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
        }

        val rimRays = 4
        for (i in 0 until rimRays) {
            val angle = state.angle + i * (2.0 * PI / rimRays)
            var d = 0.2
            while (d < radius) {
                val offset = rotate(up.clone().multiply(d), rotAxis, angle)
                world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
                d += 0.3
            }
        }
    }

    /** Plays periodic rolling wind sound. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 10.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_BREEZE_SLIDE,
                0.5f,
                1.4f,
            )
        }
    }

    /** Dismounts the player and cleans up. */
    private fun dismount(uuid: UUID) {
        SessionManager.remove(this, uuid)
        rideStates.remove(uuid)

        instance.server.getPlayer(uuid)?.let { player ->
            complete(player)
        }
    }
}
