package org.xodium.illyriacore.mechanics.world

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Schedule.schedule
import org.xodium.illyriacore.mechanics.MechanicInterface
import kotlin.math.abs

/**
 * Makes players, villagers, and tamed animals invulnerable while inside spawn protection, and
 * keeps monsters out of the protected area by cancelling their spawns and despawning any that
 * wander in or load with chunks.
 */
internal object SpawnProtectionMechanic : MechanicInterface {
    /** Interval in ticks between monster sweeps of the spawn protection area (5 seconds). */
    private const val SWEEP_INTERVAL_TICKS = 100L

    private var sweepTask: BukkitTask? = null

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player && entity !is Villager && entity !is Tameable) return
        if (isProtected(entity.location)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        if (event.entity !is Monster) return
        if (isProtected(event.location)) event.isCancelled = true
    }

    @EventHandler
    fun on(event: ChunkLoadEvent) {
        val spawn = event.world.spawnLocation
        val radius = instance.server.spawnRadius
        if (radius <= 0) return
        event
            .chunk
            .entities
            .filterIsInstance<Monster>()
            .filter { isInSpawnProtection(it.location, spawn, radius) }
            .forEach { it.remove() }
    }

    override fun register(): Long =
        super.register().also {
            sweepTask = schedule(period = SWEEP_INTERVAL_TICKS) { instance.server.worlds.forEach(::sweep) }
        }

    override fun onDisable() {
        sweepTask?.cancel()
    }

    /**
     * Removes all monsters inside the given world's spawn protection area.
     *
     * @param world The world whose spawn area should be swept.
     */
    private fun sweep(world: World) {
        val radius = instance.server.spawnRadius
        if (radius <= 0) return
        val spawn = world.spawnLocation
        world
            .entities
            .filterIsInstance<Monster>()
            .filter { isInSpawnProtection(it.location, spawn, radius) }
            .forEach { it.remove() }
    }

    /**
     * Checks if a location is inside the world's spawn protection area.
     *
     * @param location The location to check.
     * @return `true` if the location is protected, `false` otherwise.
     */
    private fun isProtected(location: Location): Boolean {
        val radius = instance.server.spawnRadius
        return radius > 0 && isInSpawnProtection(location, location.world.spawnLocation, radius)
    }

    /**
     * Checks if a location is within the vanilla spawn protection area.
     *
     * Spawn protection is a square centered on the world spawn point with a side length of
     * `(2 * radius + 1)` blocks, matching vanilla Minecraft's Chebyshev distance calculation.
     *
     * @param location The location to check.
     * @param spawn The world spawn location (center of the protected area).
     * @param radius The spawn protection radius in blocks (from `server.properties`).
     * @return `true` if the location is within the protected square, `false` otherwise.
     */
    private fun isInSpawnProtection(
        location: Location,
        spawn: Location,
        radius: Int,
    ): Boolean {
        if (location.world != spawn.world) return false
        val dx = abs(location.blockX - spawn.blockX)
        val dz = abs(location.blockZ - spawn.blockZ)
        return dx <= radius && dz <= radius
    }
}
