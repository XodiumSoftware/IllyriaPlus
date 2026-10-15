package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Combustion: charge, then launch an enormous explosion ahead of the caster.
 *
 * Hold sneak to build up energy. The longer the charge, the larger the explosion and damage
 * radius. Release sneak to unleash the blast, dealing heavy damage in a spherical area and
 * igniting blocks in the impact zone.
 *
 * Combustion does not damage the caster but leaves a brief scorch mark on the ground.
 */
internal object Combustion : AbilityInterface {
    override val cooldownMs: Long = 7500
    override val activationType: ActivationType = ActivationType.CHARGE
    override val hasSession: Boolean = true

    /** Explosion radius in blocks (before charge factor). */
    private const val RADIUS = 2.5

    /** Base damage in hearts at the center. */
    private const val DAMAGE = 3.5

    /** How long to charge in milliseconds. */
    private const val MAX_CHARGE_TIME_MS = 2500L

    /** Radius and damage multiplier at full charge. */
    private const val CHARGE_FACTOR = 2.0

    /** Maximum range in blocks. */
    private const val RANGE = 25.0

    /** Ticks between updates. */
    private const val TICK_PERIOD = 1L

    /** Ticks to apply fire to hit entities. */
    private const val FIRE_TICKS = 100

    /** Active Combustion states keyed by player UUID. */
    private val states = mutableMapOf<UUID, CombustionState>()

    /** Holds a Combustion session: charging, then explosion. */
    private class CombustionState(
        val direction: Vector,
        var factor: Double = 1.0,
        var charging: Boolean = true,
        val startTime: Long = System.currentTimeMillis(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        if (SessionManager.has(this, player.uniqueId)) return

        val direction =
            player
                .location
                .direction
                .clone()
                .normalize()

        states[player.uniqueId] = CombustionState(direction)
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
            tickCharging(player, state)
        } else {
            detonate(player, state)
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
        }
    }

    /** Ticks while charging. */
    private fun tickCharging(
        player: Player,
        state: CombustionState,
    ) {
        if (!player.isSneaking) {
            launch(state)
            return
        }

        val eyeLoc = player.eyeLocation.clone().add(state.direction)

        for (i in 0 until 16) {
            val angle = (2.0 * Math.PI * i) / 16
            val x = 1.5 * cos(angle)
            val z = 1.5 * sin(angle)
            eyeLoc.world?.spawnParticle(
                Particle.FLAME,
                eyeLoc.clone().add(x, 0.5, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }

        eyeLoc.world?.playSound(eyeLoc, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 1.2f)
    }

    /** Launches the explosion from a charging session. */
    private fun launch(state: CombustionState) {
        val elapsed = System.currentTimeMillis() - state.startTime
        state.factor =
            when {
                elapsed >= MAX_CHARGE_TIME_MS -> CHARGE_FACTOR
                else -> 1.0 + (CHARGE_FACTOR - 1.0) * elapsed.toDouble() / MAX_CHARGE_TIME_MS
            }

        state.charging = false
    }

    /** Triggers the explosion at a forward location. */
    private fun detonate(
        player: Player,
        state: CombustionState,
    ) {
        val blastCenter =
            player
                .eyeLocation
                .clone()
                .add(state.direction.clone().multiply(RANGE * 0.7))
        val world = blastCenter.world ?: return

        world.createExplosion(blastCenter, (RADIUS * state.factor).toFloat(), true, true, player)

        for (i in 0 until 24) {
            val angle = (2.0 * Math.PI * i) / 24
            val x = (RADIUS * state.factor) * cos(angle)
            val z = (RADIUS * state.factor) * sin(angle)
            world.spawnParticle(
                Particle.EXPLOSION,
                blastCenter.clone().add(x, 0.0, z),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }

        world
            .getNearbyEntities(blastCenter, RADIUS * state.factor, RADIUS * state.factor, RADIUS * state.factor)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(DAMAGE * state.factor, player)
                entity.fireTicks = FIRE_TICKS
            }

        world.playSound(blastCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f)
    }
}
