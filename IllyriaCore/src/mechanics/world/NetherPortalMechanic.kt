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

/** Prevents portals outside the spawn protection area from being created or entered, and links spawn portals to their counterparts. */
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
        when (event.cause) {
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL -> linkPortal(event)
            PlayerTeleportEvent.TeleportCause.END_PORTAL -> linkEndPortal(event)
            else -> return
        }
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
     * to the Overworld spawn. The destination Y coordinate is adjusted to the highest solid block
     * to account for flat worlds with non-standard spawn heights.
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
        event.to = targetWorld.spawnLocation.toSurface()
    }

    /**
     * Redirects End portal teleports from the End to the Overworld spawn instead of the player's respawn point.
     * The destination Y coordinate is adjusted to the highest solid block.
     *
     * @param event The [PlayerPortalEvent] to redirect.
     */
    private fun linkEndPortal(event: PlayerPortalEvent) {
        if (event.from.world?.environment != World.Environment.THE_END) return
        val overworld =
            instance
                .server
                .worlds
                .find { it.environment == World.Environment.NORMAL } ?: return
        event.to = overworld.spawnLocation.toSurface()
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

/**
 * Returns a copy of this location with the Y adjusted to the highest solid block's Y + 1 at the current X/Z.
 * If no solid block exists (e.g. flat world), falls back to the original Y.
 */
private fun Location.toSurface(): Location =
    clone().apply {
        val world = world ?: return@apply
        val surfaceY = world.getHighestBlockYAt(blockX, blockZ) + 1
        y = maxOf(surfaceY, blockY).toDouble()
    }
