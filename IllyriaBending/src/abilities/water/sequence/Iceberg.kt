package org.xodium.illyriabending.abilities.water.sequence

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Iceberg: erupt a colossal jagged spire of ice in front of the caster.
 *
 * After activation, a towering cluster of packed ice and blue ice bursts from the
 * ground at a fixed distance ahead, impaling any entities standing above it and
 * knocking them skyward. The iceberg lingers for a short duration, then shatters
 * back into snow particles as it decays.
 *
 * Iceberg radius, height, damage, and duration scale with level.
 */
internal object Iceberg : AbilityInterface {
    override val cooldownMs: Long = 12000
    override val activationType = ActivationTypeEnum.INSTANT

    /** Distance in front of the caster where the iceberg erupts. */
    private const val FORWARD_OFFSET = 5.0

    /** Base iceberg footprint radius in blocks (before level scaling). */
    private const val RADIUS = 2

    /** Additional radius in blocks per level. */
    private const val RADIUS_PER_LEVEL = 1

    /** Base iceberg height in blocks (before level scaling). */
    private const val HEIGHT = 4

    /** Additional height in blocks per level. */
    private const val HEIGHT_PER_LEVEL = 1

    /** Damage in hearts dealt on eruption (before level scaling). */
    private const val DAMAGE = 3.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.5

    /** Upward knockback applied to entities standing above the iceberg. */
    private const val LAUNCH = 0.9

    /** Base duration the iceberg stands before decaying, in ticks. */
    private const val DURATION_TICKS = 240L

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 60L

    /** Active iceberg sessions keyed by caster UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active Iceberg session: the spawned blocks and remaining life.
     */
    private class Session(
        val blocks: MutableList<Pair<Block, Material>>,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

        val eye = player.eyeLocation.clone()
        val dir =
            eye
                .direction
                .clone()
                .setY(0)
                .normalize()
        val world = player.world

        val centerLoc = eye.clone().add(dir.clone().multiply(FORWARD_OFFSET))
        val center = world.getHighestBlockAt(centerLoc).location.add(0.5, 1.0, 0.5)

        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL
        val height = HEIGHT + (level - 1) * HEIGHT_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL

        val placed = mutableListOf<Pair<Block, Material>>()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val dx = x.toDouble() / radius
                val dz = z.toDouble() / radius
                val dist = dx * dx + dz * dz
                if (dist > 1.0) continue

                val columnHeight = ((1.0 - dist) * height).toInt().coerceAtLeast(1)
                val spikeJitter = ((Math.random() - 0.5) * 2).toInt()
                val finalHeight = (columnHeight + spikeJitter).coerceAtLeast(1)

                for (y in 0 until finalHeight) {
                    val block =
                        world.getBlockAt(
                            center.blockX + x,
                            center.blockY + y,
                            center.blockZ + z,
                        )
                    if (!block.isPassable && !block.isLiquid && !block.type.isAir) break

                    val previous = block.type
                    placed.add(block to previous)
                    block.type =
                        when {
                            y == finalHeight - 1 -> Material.BLUE_ICE
                            Math.random() < 0.3 -> Material.ICE
                            else -> Material.PACKED_ICE
                        }
                }
            }
        }

        if (placed.isEmpty()) return

        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f)
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0.0, 0.0, 0.0, 0.0)

        world
            .getNearbyEntities(center, radius.toDouble() + 0.5, height.toDouble() + 0.5, radius.toDouble() + 0.5)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(damage, player)
                val vel = entity.velocity
                entity.velocity =
                    Vector(
                        vel.x * 0.3,
                        LAUNCH,
                        vel.z * 0.3,
                    )
            }

        val session = Session(placed)
        sessions[uuid] = session
        session.task =
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    decay(session)
                    sessions.remove(uuid)
                },
                DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL,
            )

        complete(player)
    }

    /** Removes the placed iceberg blocks and emits a shatter effect. */
    private fun decay(session: Session) {
        val anyBlock = session.blocks.firstOrNull() ?: return
        val world = anyBlock.first.world
        val center = anyBlock.first.location

        for ((block, original) in session.blocks) {
            if (
                block.type == Material.PACKED_ICE ||
                block.type == Material.ICE ||
                block.type == Material.BLUE_ICE
            ) {
                block.type = original
            }
        }

        world.spawnParticle(Particle.SNOWFLAKE, center, 80, 2.0, 3.0, 2.0, 0.05)
        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f)
    }
}
