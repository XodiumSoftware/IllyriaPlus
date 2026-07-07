package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockState
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
import kotlin.math.hypot

/** Represents a mechanic handling dimension effects within the system. */
internal object DimensionMechanic : MechanicInterface {
    private const val NETHER_TO_OVERWORLD_RATIO = 8
    private const val PORTAL_SEARCH_RADIUS: Int = 128
    private const val CREATION_DENIED_MSG: String =
        "<firewatch>No corresponding active portal found in the Overworld!</gradient>"

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerPortalEvent) = playerPortal(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: EntityPortalEvent) = entityPortal(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PortalCreateEvent) = portalCreate(event)

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
     * Handles the PortalCreateEvent to prevent portal creation in the Nether if no corresponding Overworld portal exists.
     *
     * @param event The PortalCreateEvent to handle.
     */
    private fun portalCreate(event: PortalCreateEvent) {
        if (event.world.environment == World.Environment.NETHER &&
            event.reason == PortalCreateEvent.CreateReason.FIRE
        ) {
            val overworld = getOverworld()

            if (findCorrespondingPortal(calcPortalCentre(event.blocks), overworld) == null) {
                event.isCancelled = true

                val player = event.entity as? Player ?: return
                val destination = player.respawnLocation?.takeIf { it.world == overworld } ?: overworld.spawnLocation

                player.sendActionBar(MM.deserialize(CREATION_DENIED_MSG))
                player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
            }
        }
    }

    /**
     * Searches for an existing Nether portal in the target world near the corresponding scaled location.
     *
     * @param netherPortal The location of the portal in the Nether.
     * @param overworld The world to search in (should be the Overworld).
     * @param searchRadius The radius in blocks to search for portal blocks.
     * @return The closest portal block location if found, otherwise null.
     */
    private fun findCorrespondingPortal(
        netherPortal: Location,
        overworld: World,
        searchRadius: Int = PORTAL_SEARCH_RADIUS,
    ): Location? {
        val targetX = netherPortal.x * NETHER_TO_OVERWORLD_RATIO
        val targetZ = netherPortal.z * NETHER_TO_OVERWORLD_RATIO

        var closestPortal: Location? = null
        var closestDistance = Double.MAX_VALUE

        val startX = (targetX - searchRadius).toInt()
        val endX = (targetX + searchRadius).toInt()
        val startZ = (targetZ - searchRadius).toInt()
        val endZ = (targetZ + searchRadius).toInt()

        for (x in startX..endX) {
            for (z in startZ..endZ) {
                for (y in 0..overworld.maxHeight) {
                    val block = overworld.getBlockAt(x, y, z)

                    if (block.type == Material.NETHER_PORTAL) {
                        val dist = hypot(targetX - x.toDouble(), targetZ - z.toDouble())

                        if (dist < closestDistance) {
                            closestDistance = dist
                            closestPortal = block.location
                        }
                    }
                }
            }
        }
        return closestPortal
    }

    /**
     * Calculates the centre point of a portal structure by averaging the positions of its constituent blocks.
     *
     * @param blockStates The list of [BlockState]s representing the portal frame and portal blocks.
     * @return The [Location] representing the geometric centre of the portal.
     */
    private fun calcPortalCentre(blockStates: List<BlockState>): Location =
        blockStates
            .map { it.location }
            .reduce { location1, location2 -> location1.add(location2) }
            .multiply(1.0 / blockStates.size)

    /**
     * Retrieves the Overworld instance or throws if not found.
     *
     * @return The Overworld [World] object.
     * @throws IllegalStateException if the Overworld is not loaded.
     */
    private fun getOverworld(): World = instance.server.getWorld("world") ?: error("Overworld (world) is not loaded.")
}
