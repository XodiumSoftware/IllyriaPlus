package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireShield: while sneaking, maintain a swirling shell of flame that scorches attackers
 * and incinerates projectiles.
 *
 * The shield persists until sneak is released or the maximum duration expires.
 * The shell radius and burn effect both scale with level.
 */
internal object FireShield : AbilityInterface {
    override val cooldownMs: Long = 5000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.DURATION
    override val hasSession: Boolean = true

    /** Shield shell radius in blocks (before level scaling). */
    private const val RADIUS = 2.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.3

    /** Damage in hearts dealt to entities touching the shield per pulse. */
    private const val DAMAGE = 0.5

    /** Ticks of fire applied to entities touching the shield. */
    private const val FIRE_TICKS = 60

    /** Repulsion force applied to entities touching the shield. */
    private const val REPEL = 0.6

    /** Maximum shield duration in ticks. */
    private const val MAX_DURATION_TICKS = 160L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = PI / 24

    /** Active shield states keyed by player UUID. */
    private val states = mutableMapOf<UUID, ShieldState>()

    /** Holds state for an active FireShield session. */
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

    /** Renders the swirling shell of flame particles around the player. */
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
                world.spawnParticle(Particle.FLAME, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    /** Burns and repels nearby entities; incinerates projectiles. */
    private fun repel(
        player: Player,
        state: ShieldState,
    ) {
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (state.level - 1) * RADIUS_PER_LEVEL

        world
            .getNearbyEntities(origin, radius, radius, radius)
            .forEach { entity ->
                when (entity) {
                    is Projectile -> {
                        entity.remove()
                        world.spawnParticle(
                            Particle.FLAME,
                            entity.location,
                            8,
                            0.2,
                            0.2,
                            0.2,
                            0.0,
                        )
                    }

                    is LivingEntity -> {
                        if (entity == player) return@forEach
                        entity.damage(DAMAGE, player)
                        entity.fireTicks = FIRE_TICKS
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .setY(0.2)
                                .normalize()
                                .multiply(REPEL)
                        entity.velocity = entity.velocity.add(away)
                    }
                }
            }
    }

    /** Plays periodic flame sound. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 8.0) {
            player.world.playSound(
                player.location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.6f,
                1.0f,
            )
        }
    }
}
