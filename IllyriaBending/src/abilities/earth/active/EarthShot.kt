package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * EarthShot: pelt the target with a rapid burst of compacted pebbles.
 *
 * On activation, the caster summons a string of small stone projectiles that fire
 * one after another in the facing direction, striking for modest damage and light
 * knockback. Each shot is a one-tick hop forward, so the volley tracks quickly and
 * feels like a hail of dirt and gravel.
 *
 * Number of shots, damage per shot, and spread scale with level.
 */
internal object EarthShot : AbilityInterface {
    override val cooldownMs: Long = 2500

    /** Base number of stones hurled per activation. */
    private const val SHOTS = 3

    /** Additional shots every two levels. */
    private const val SHOTS_PER_TWO_LEVELS = 1

    /** Damage in hearts per stone (before level scaling). */
    private const val DAMAGE = 0.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.1

    /** Horizontal knockback per stone. */
    private const val KNOCKBACK = 0.25

    /** Vertical knockback per stone. */
    private const val KNOCKUP = 0.1

    /** Per-tick projectile speed in blocks. */
    private const val SPEED = 1.2

    /** Maximum travel range of a single stone in blocks. */
    private const val RANGE = 18.0

    /** Ticks between successive shots in the volley. */
    private const val SHOT_INTERVAL_TICKS = 2L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active volley sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a single EarthShot burst: fires [shotsLeft] stones one at a time. */
    private class Session(
        val player: Player,
        val level: Int,
        var shotsLeft: Int,
        var ticks: Long = 0L,
        val projectiles: MutableList<Stone> = mutableListOf(),
        var task: BukkitTask? = null,
    )

    /** A single flying stone. */
    private class Stone(
        val player: Player,
        origin: Location,
        direction: Vector,
        val damage: Double,
        val knockback: Double,
    ) {
        /** Current location of the stone. */
        var location: Location = origin.clone()

        /** Normalized travel direction with a slight random jitter. */
        val dir: Vector = direction.clone().normalize()

        /** Per-tick movement vector. */
        val velocity: Vector = dir.clone().multiply(SPEED)

        /** Maximum ticks before the stone expires. */
        val maxTicks: Int = ceil(RANGE / SPEED).toInt()

        var ticksAlive = 0

        /** Advances the stone one tick. Returns `false` when it should be removed. */
        fun update(): Boolean {
            if (++ticksAlive > maxTicks) return false
            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) {
                impact()
                return false
            }
            if (block.isLiquid) {
                impact()
                return false
            }

            render()
            return !checkHit()
        }

        /** Renders the trailing stone particles. */
        private fun render() {
            location.world?.spawnParticle(
                Particle.BLOCK,
                location,
                2,
                0.04,
                0.04,
                0.04,
                0.0,
                Material.COBBLESTONE.createBlockData(),
            )
        }

        /** Damages the first living entity overlapping the stone's hitbox. */
        private fun checkHit(): Boolean {
            val world = location.world ?: return false
            val entity =
                world
                    .getNearbyEntities(location, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)
                    .filterIsInstance<LivingEntity>()
                    .firstOrNull { it != player }
                    ?: return false
            entity.damage(damage, player)
            val kb =
                dir
                    .clone()
                    .setY(KNOCKUP)
                    .normalize()
                    .multiply(knockback)
            entity.velocity = entity.velocity.add(kb)
            impact()
            return true
        }

        /** Splinters the stone into particles and sound at its terminal location. */
        private fun impact() {
            location.world?.playSound(location, Sound.BLOCK_STONE_HIT, 0.7f, 1.2f)
            location.world?.spawnParticle(
                Particle.BLOCK,
                location,
                8,
                0.2,
                0.2,
                0.2,
                0.04,
                Material.COBBLESTONE.createBlockData(),
            )
        }

        /** Collision radius for hit detection. */
        private companion object {
            const val HIT_RADIUS = 0.7
        }
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

        val shots = SHOTS + (level - 1) / 2 * SHOTS_PER_TWO_LEVELS
        val session = Session(player, level, shots)
        sessions[uuid] = session

        player.world.playSound(player.eyeLocation, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 1.0f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a volley session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        session.ticks += TICK_PERIOD

        // Fire the next stone when it's time.
        if (session.shotsLeft > 0 && session.ticks % SHOT_INTERVAL_TICKS == 0L) {
            fire(session)
            session.shotsLeft--
        }

        // Update in-flight stones.
        val iterator = session.projectiles.iterator()
        while (iterator.hasNext()) {
            val stone = iterator.next()
            if (!stone.update()) {
                iterator.remove()
            }
        }

        if (session.shotsLeft <= 0 && session.projectiles.isEmpty()) {
            remove(uuid)
        }
    }

    /** Launches a single stone from the caster's eye with a small random spread. */
    private fun fire(session: Session) {
        val player = session.player
        val origin = player.eyeLocation.clone()
        val baseDir = origin.direction.clone()

        // Small random cone spread so the volley feels organic.
        val jitter =
            Vector(
                (Math.random() - 0.5) * 0.05,
                (Math.random() - 0.5) * 0.04,
                (Math.random() - 0.5) * 0.05,
            )
        val dir = baseDir.add(jitter).normalize()

        val damage = DAMAGE + (session.level - 1) * DAMAGE_PER_LEVEL
        val stone = Stone(player, origin, dir, damage, KNOCKBACK)
        session.projectiles.add(stone)
        player.world.playSound(origin, Sound.BLOCK_STONE_PLACE, 0.55f, 1.0f + 0.08f * session.shotsLeft)
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
