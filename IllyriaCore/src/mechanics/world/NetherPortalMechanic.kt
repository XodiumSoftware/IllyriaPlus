package org.xodium.illyriacore.mechanics.world

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.PortalCreateEvent
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.MM

/** Prevents nether portals outside the spawn protection area from being created or entered. */
internal object NetherPortalMechanic : MechanicInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: PortalCreateEvent) {
        val location = event.blocks.firstOrNull()?.location ?: return
        if (cancelPortal(location)) {
            event.isCancelled = true
            portalCancelledMessage(event.entity as? Player ?: return)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerPortalEvent) {
        if (cancelPortal(event.from)) {
            event.isCancelled = true
            portalCancelledMessage(event.player)
            return
        }
        if (event.cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return
        linkPortal(event)
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityPortalEnterEvent) {
        if (cancelPortal(event.location)) {
            event.isCancelled = true
            portalCancelledMessage(event.entity as? Player ?: return)
        }
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
        val radius = instance.server.spawnRadius
        return radius > 0 && location.distanceSquared(spawn) > radius * radius
    }

    /**
     * Links the portal teleport to the counterpart world's spawn location.
     *
     * Overworld spawn portals teleport to the Nether world spawn, and Nether spawn portals teleport
     * to the Overworld spawn. This bypasses vanilla portal searching/creation to guarantee the pair
     * always link to each other regardless of the overworld spawn offset.
     *
     * @param event The [PlayerPortalEvent] to redirect.
     */
    private fun linkPortal(event: PlayerPortalEvent) {
        val targetEnvironment =
            when (event.from.world?.environment) {
                World.Environment.NORMAL -> World.Environment.NETHER
                World.Environment.NETHER -> World.Environment.NORMAL
                else -> return
            }
        val targetWorld =
            instance
                .server
                .worlds
                .find { it.environment == targetEnvironment } ?: return
        event.canCreatePortal = false
        event.searchRadius = 0
        event.to = targetWorld.spawnLocation
    }

    /**
     * Sends an action bar message to the player indicating that portals cannot be created outside spawn protection.
     *
     * @param player The player to notify.
     */
    private fun portalCancelledMessage(player: Player) =
        player.sendActionBar(
            MM.deserialize("<firewatch>Portals cannot be created, use the one at spawn instead!</gradient>"),
        )
}
