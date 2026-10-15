package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WaterBubble: create a pocket of breathable water around the caster underwater.
 *
 * While sneaking, the player is granted Water Breathing and Conduit Power, water
 * blocks inside their breathing space are temporarily turned to air, and a spinning
 * ring of bubble particles surrounds their head. The bubble collapses when sneak is
 * released, the duration expires, or the player leaves the water.
 *
 * Effect duration and bubble radius scale with level.
 */
internal object WaterBubble : AbilityInterface {
    override val cooldownMs: Long = 9000

    /** Maximum bubble duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 160L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 40L

    /** Refresh interval in ticks for the Water Breathing effect. */
    private const val EFFECT_REFRESH_TICKS = 100

    /** Bubble radius in blocks (before level scaling). */
    private const val RADIUS = 1.3

    /** Additional bubble radius per level. */
    private const val RADIUS_PER_LEVEL = 0.15

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the bubble ring render. */
    private const val ROTATION_STEP = 0.35

    /** How many ring segments to draw each tick. */
    private const val RING_SEGMENTS = 12

    /** Active bubble sessions keyed by caster UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active WaterBubble channel.
     */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        // Kick everything off with an initial effect application so it lands instantly.
        applyEffects(player, level)

        val session = Session(player, level)
        sessions[player.uniqueId] = session
        player.world.playSound(player.location, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0f, 1.2f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a bubble session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        val headBlock = player.eyeLocation.block

        val stillValid =
            player.isOnline &&
                !player.isDead &&
                player.isSneaking &&
                session.ticks <= maxTicks &&
                headBlock.isLiquid

        if (!stillValid) {
            remove(uuid)
            return
        }

        // Refresh breathing effects periodically so they never lapse.
        if (session.ticks % EFFECT_REFRESH_TICKS == 0L) {
            applyEffects(player, session.level)
        }

        render(session)
        carve(session)

        if (Math.random() < 1.0 / 10.0) {
            player.world.playSound(
                player.location,
                Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                0.5f,
                1.5f,
            )
        }
    }

    /**
     * Applies the breathing and conduit effects. Duration is padded beyond the refresh
     * interval so effects never expire between pulses.
     */
    private fun applyEffects(
        player: Player,
        level: Int,
    ) {
        val durationTicks = EFFECT_REFRESH_TICKS * 2
        player.addPotionEffect(
            PotionEffect(PotionEffectType.WATER_BREATHING, durationTicks, 0, false, false, true),
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.CONDUIT_POWER,
                durationTicks,
                (level - 1).coerceAtMost(1),
                false,
                false,
                true,
            ),
        )
    }

    /**
     * Replaces water blocks inside the bubble with air. This simulates a pocket of
     * breathable water and lets the player see clearly. Restores water after the
     * carve so the change is purely temporary.
     */
    private fun carve(session: Session) {
        val player = session.player
        val world = player.world
        val eye = player.eyeLocation
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL
        val r = radius.toInt().coerceAtLeast(1)

        val carved = mutableListOf<Block>()
        for (dx in -r..r) {
            for (dy in -r..r) {
                for (dz in -r..r) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue
                    val block = world.getBlockAt(eye.blockX + dx, eye.blockY + dy, eye.blockZ + dz)
                    if (block.type == Material.WATER) {
                        block.type = Material.AIR
                        carved.add(block)
                    }
                }
            }
        }

        if (carved.isEmpty()) return

        // Restore the carved blocks next tick so the bubble appears to follow the player.
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                carved.forEach {
                    if (it.type == Material.AIR) it.type = Material.WATER
                }
            },
            TICK_PERIOD * 3,
        )
    }

    /** Renders the spinning ring of bubble particles around the caster's head. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val head = player.eyeLocation.clone()
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL

        for (i in 0 until RING_SEGMENTS) {
            val angle = session.angle + i * (2.0 * PI / RING_SEGMENTS)
            val offset =
                Vector(
                    cos(angle) * radius,
                    sin(angle * 2.0) * 0.1,
                    sin(angle) * radius,
                )
            world.spawnParticle(Particle.BUBBLE, head.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            if (Math.random() < 0.15) {
                world.spawnParticle(
                    Particle.BUBBLE_COLUMN_UP,
                    head.clone().add(offset.multiply(0.9)),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
            }
        }

        // Outer dome of rising bubbles above the head.
        world.spawnParticle(
            Particle.BUBBLE_POP,
            head.clone().add(0.0, 0.4, 0.0),
            4,
            0.4,
            0.2,
            0.4,
            0.0,
        )
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.let { session ->
            val player = session.player
            session.task?.cancel()
            player.world.playSound(player.location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.6f, 0.5f)
        }
    }
}
