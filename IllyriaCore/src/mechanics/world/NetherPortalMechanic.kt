package org.xodium.illyriacore.mechanics.world

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.world.PortalCreateEvent
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.mechanics.MechanicInterface

/** Prevents nether portals outside the spawn protection area from being created or entered. */
internal object NetherPortalMechanic : MechanicInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: PortalCreateEvent) {
        val location = event.blocks.firstOrNull()?.location ?: return
        if (cancelPortal(location)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerPortalEvent) {
        if (cancelPortal(event.from)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityPortalEnterEvent) {
        if (cancelPortal(event.location)) event.isCancelled = true
    }

    /**
     * Determines if the given location is outside the spawn protection zone.
     *
     * @param location The location to check.
     * @return `true` if the portal should be cancelled (outside spawn protection), `false` otherwise.
     */
    private fun cancelPortal(location: Location): Boolean {
        val world = location.world ?: return true
        val spawn = world.spawnLocation
        val radius = IllyriaCore.instance.server.spawnRadius
        return radius > 0 && location.distanceSquared(spawn) > radius * radius
    }
}
