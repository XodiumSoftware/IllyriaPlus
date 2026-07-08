package org.xodium.illyriaplus.mechanics.world

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.PortalCreateEvent
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling dimension effects within the system. */
internal object DimensionMechanic : MechanicInterface {
    private const val CREATION_DENIED_MSG: String =
        "<firewatch>Portals can only be created in the Overworld!</gradient>"

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerPortalEvent) = playerPortal(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: EntityPortalEvent) = entityPortal(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PortalCreateEvent) = cancelPortalCreation(event)

    /**
     * Handles the PlayerPortalEvent to prevent portal creation in the Nether.
     *
     * @param event The PlayerPortalEvent to handle.
     */
    private fun playerPortal(event: PlayerPortalEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (event.player.world.environment == World.Environment.NETHER) event.canCreatePortal = false
        }
    }

    /**
     * Handles the EntityPortalEvent to prevent portal creation in the Nether.
     *
     * @param event The EntityPortalEvent to handle.
     */
    private fun entityPortal(event: EntityPortalEvent) {
        if (event.entity.world.environment == World.Environment.NETHER) event.canCreatePortal = false
    }

    /**
     * Always cancels Nether-side portal creation.
     * Players must create portals in the Overworld instead.
     *
     * @param event The PortalCreateEvent to handle.
     */
    private fun cancelPortalCreation(event: PortalCreateEvent) {
        if (event.world.environment != World.Environment.NETHER) return
        if (event.reason != PortalCreateEvent.CreateReason.FIRE) return

        event.isCancelled = true

        val player = event.entity as? Player ?: return
        val overworld = instance.server.getWorld("world") ?: return
        val destination = player.respawnLocation?.takeIf { it.world == overworld } ?: overworld.spawnLocation

        player.sendActionBar(MM.deserialize(CREATION_DENIED_MSG))
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
    }
}
