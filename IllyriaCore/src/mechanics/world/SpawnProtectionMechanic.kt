package org.xodium.illyriacore.mechanics.world

import org.bukkit.Location
import org.bukkit.entity.Monster
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.mechanics.MechanicInterface

/** Prevents hostile mobs from damaging entities inside the vanilla spawn protection area. */
internal object SpawnProtectionMechanic : MechanicInterface {
    @EventHandler
    fun on(event: EntityDamageByEntityEvent) = preventSpawnDamage(event)

    /**
     * Prevents hostile mobs from damaging entities inside the spawn protection area.
     *
     * @param event The EntityDamageByEntityEvent to handle.
     */
    private fun preventSpawnDamage(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val world = entity.world
        val radius = IllyriaCore.instance.server.spawnRadius
        if (radius <= 0) return

        val spawn = world.spawnLocation
        if (!isInSpawnProtection(entity.location, spawn, radius)) return

        val damager = event.damager
        if (damager is Monster || damager is Projectile && damager.shooter is Monster) {
            event.isCancelled = true
        }
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
        val dx = kotlin.math.abs(location.blockX - spawn.blockX)
        val dz = kotlin.math.abs(location.blockZ - spawn.blockZ)
        return dx <= radius && dz <= radius
    }
}
