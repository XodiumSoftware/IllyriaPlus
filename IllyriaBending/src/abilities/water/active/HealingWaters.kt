package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
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
 * HealingWaters: channel cleansing water around the caster to mend wounds.
 *
 * While the caster sneaks, healing water spirals around them. Every pulse, either the
 * caster or the most-injured allied player within [HEAL_RADIUS] receives a small
 * regeneration tick. The channel ends when sneak is released or after
 * [MAX_DURATION_TICKS].
 *
 * Heal amount, radius, and channel duration all scale with level.
 */
internal object HealingWaters : AbilityInterface {
    override val cooldownMs: Long = 10000
    override val activationType = ActivationTypeEnum.DURATION
    override val hasSession = true

    /** Radius in blocks for finding wounded allies (before level scaling). */
    private const val HEAL_RADIUS = 4.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.5

    /** Health restored per healing pulse, in hearts. Half applies to self. */
    private const val HEAL_PER_PULSE = 0.5

    /** Additional healing per pulse per level. */
    private const val HEAL_PER_LEVEL = 0.25

    /** Ticks between healing pulses. */
    private const val HEAL_PERIOD_TICKS = 20L

    /** Maximum channel duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 100L

    /** Additional channel duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = 0.45

    /** Height of the particle column for the render. */
    private const val RENDER_HEIGHT = 2.0

    /** Per-player healing state for active sessions. */
    private val channels = ConcurrentHashMap<UUID, ChannelData>()

    /** Holds state for an active HealingWaters channel. */
    private class ChannelData(
        val player: Player,
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

        channels[uuid] = ChannelData(player, level)
        SessionManager.start(this, uuid)
    }

    override fun tickSession(uuid: UUID) {
        val data = channels[uuid] ?: return
        val player = data.player

        data.ticks += TICK_PERIOD
        data.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (data.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || !player.isSneaking || data.ticks > maxTicks) {
            channels.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        render(data)

        if (data.ticks % HEAL_PERIOD_TICKS == 0L) {
            heal(data)
        }

        if (Math.random() < 1.0 / 8.0) {
            player.world.playSound(player.location, Sound.BLOCK_WATER_AMBIENT, 0.6f, 1.6f)
        }
    }

    /**
     * Picks the most-injured allied player (including the caster) inside the heal
     * radius and restores a small amount of health.
     */
    private fun heal(data: ChannelData) {
        val player = data.player
        val radius = HEAL_RADIUS + (data.level - 1) * RADIUS_PER_LEVEL
        val healAmount = HEAL_PER_PULSE + (data.level - 1) * HEAL_PER_LEVEL

        val world = player.world
        val candidates =
            world
                .getNearbyEntities(player.location, radius, radius, radius)
                .filterIsInstance<Player>()
                .filter { !it.isDead }

        if (candidates.isEmpty()) return

        val target =
            candidates
                .filter { candidate ->
                    val max = candidate.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                    max - candidate.health >= HEAL_PER_PULSE
                }.maxByOrNull { candidate ->
                    val max = candidate.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                    (max - candidate.health) / max
                } ?: return

        val targetMax = target.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        target.health = (target.health + healAmount).coerceIn(0.0, targetMax)
        val loc = target.location.clone().add(0.0, 1.0, 0.0)
        target.world.spawnParticle(Particle.HEART, loc, 1, 0.3, 0.3, 0.3, 0.0)
        target.world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.7f, 1.4f)
    }

    /** Renders the spiraling column of water particles around the caster. */
    private fun render(data: ChannelData) {
        val player = data.player
        val world = player.world
        val base = player.location.clone()

        var y = 0.2
        while (y <= RENDER_HEIGHT) {
            val rays = 4
            for (i in 0 until rays) {
                val angle = data.angle + i * (2.0 * PI / rays) + y * 0.7
                val offset = Vector(cos(angle) * 0.7, y, sin(angle) * 0.7)
                world.spawnParticle(Particle.SPLASH, base.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
                if (Math.random() < 0.3) {
                    world.spawnParticle(
                        Particle.BUBBLE_COLUMN_UP,
                        base.clone().add(offset.multiply(0.6)),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                }
            }
            y += 0.35
        }

        // Soft falling-water drips above the caster's head.
        world.spawnParticle(
            Particle.FALLING_WATER,
            base.clone().add(0.0, 2.1, 0.0),
            2,
            0.2,
            0.1,
            0.2,
            0.0,
        )
    }
}
