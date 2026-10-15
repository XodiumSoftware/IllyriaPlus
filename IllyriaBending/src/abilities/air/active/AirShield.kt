package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirShield: while sneaking, maintain a swirling shell of wind that deflects projectiles
 * and pushes away nearby entities.
 *
 * The shield persists until sneak is released or the maximum duration expires.
 * The shell radius and repel force both scale with level.
 */
internal object AirShield : AbilityInterface {
    override val cooldownMs: Long = 4000
    override val activationType: ActivationType = ActivationType.DURATION
    override val hasSession: Boolean = true

    /** Shield shell radius in blocks (before level scaling). */
    private const val RADIUS = 2.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.3

    /** Repulsion force applied to entities touching the shield. */
    private const val REPEL = 0.8

    /** Projectile deflection velocity multiplier. */
    private const val DEFLECT = 0.6

    /** Maximum shield duration in ticks. */
    private const val MAX_DURATION_TICKS = 160L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = PI / 24

    /** Per-session shield state keyed by player UUID. */
    private val states = mutableMapOf<UUID, ShieldState>()

    /** Holds per-session state for an active shield. */
    private class ShieldState(
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        if (SessionManager.has(this, player.uniqueId)) return

        states[player.uniqueId] = ShieldState(level)
        SessionManager.start(this, player.uniqueId, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        state.ticks += TICK_PERIOD
        state.angle += ROTATION_STEP

        if (!player.isOnline || player.isDead || !player.isSneaking || state.ticks > MAX_DURATION_TICKS) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        render(player, state)
        repel(player, state)
        playSound(player)
    }

    /** Renders the swirling shell of air particles around the player. */
    private fun render(
        player: Player,
        state: ShieldState,
    ) {
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (state.level - 1) * RADIUS_PER_LEVEL

        val rings = 3
        for (ring in 0 until rings) {
            val phi = ring * PI / rings + state.angle
            val rays = 16
            for (i in 0 until rays) {
                val theta = i * (2.0 * PI / rays) + state.angle
                val offset =
                    Vector(
                        cos(theta) * sin(phi) * radius,
                        cos(phi) * radius,
                        sin(theta) * sin(phi) * radius,
                    )
                world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    /** Repels nearby entities and deflects projectiles. */
    private fun repel(
        player: Player,
        state: ShieldState,
    ) {
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (state.level - 1) * RADIUS_PER_LEVEL
        val levelScale = 1.0 + (state.level - 1) * 0.2

        world
            .getNearbyEntities(origin, radius, radius, radius)
            .forEach { entity ->
                when (entity) {
                    is Projectile -> {
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .normalize()
                        entity.velocity = away.multiply(entity.velocity.length() * DEFLECT)
                    }

                    is LivingEntity -> {
                        if (entity == player) return@forEach
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .setY(0.2)
                                .normalize()
                                .multiply(REPEL * levelScale)
                        entity.velocity = entity.velocity.add(away)
                    }
                }
            }
    }

    /** Plays periodic wind sound. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 8.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.5f,
                1.2f,
            )
        }
    }
}
