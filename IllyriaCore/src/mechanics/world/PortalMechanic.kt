package org.xodium.illyriacore.mechanics.world

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.PortalCreateEvent
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.World.toSurface
import org.xodium.illyriacore.data.PortalData
import org.xodium.illyriacore.items.IncendiumKeyItem
import org.xodium.illyriacore.items.ItemInterface
import org.xodium.illyriacore.items.NullscapeKeyItem
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.lastNetherPortal
import org.xodium.illyrialib.Utils.MM

/**
 * Restricts Overworld portals to the spawn protection area, while portals in the Nether can be
 * created anywhere but always teleport players back to the Overworld spawn.
 * Requires Incendium Key for Nether travel and Nullscape Key for End travel.
 */
internal object PortalMechanic : MechanicInterface {
    /** Velocity applied when a player is rejected from a portal (pushed back). */
    private const val PUSHBACK_STRENGTH = 1.5

    /** Upward velocity component of the portal rejection pushback. */
    private const val PUSHBACK_UPWARD = 0.5

    /** Number of portal particles spawned on rejection. */
    private const val REJECT_PARTICLE_COUNT = 40

    /** Spread (offset radius) of the rejection particles around the player. */
    private const val REJECT_PARTICLE_SPREAD = 0.6

    /**
     * Portal cooldown (in ticks) applied after a rejection, so the player is not spammed with the
     * rejection effect every tick while standing in a portal.
     */
    private const val REJECT_COOLDOWN_TICKS = 40

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
        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (!requireKey(
                    event,
                    World.Environment.NETHER,
                    IncendiumKeyItem,
                    "<mango>You need the Incendium Key!</gradient>",
                    "<red>Find one to unlock the Nether</red>",
                )
            ) {
                return
            }
            returnToLastLocation(event)
            returnToSpawn(event)
        }
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            requireKey(
                event,
                World.Environment.THE_END,
                NullscapeKeyItem,
                "<mango>You need the Nullscape Key!</gradient>",
                "<red>Find one to unlock the End</red>",
            )
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityPortalEnterEvent) {
        val player =
            event.entity as? Player ?: run {
                if (cancelPortal(event.location)) event.isCancelled = true
                return
            }
        if (cancelPortal(event.location)) {
            event.isCancelled = true
            portalCancelledMessage(player)
            return
        }

        val blockType = event.location.block.type
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return
        if (player.portalCooldown > 0) return

        val portalInfo =
            when (blockType) {
                Material.NETHER_PORTAL ->
                    PortalData(
                        World.Environment.NETHER,
                        IncendiumKeyItem,
                        "<mango>You need the Incendium Key!</gradient>",
                        "<red>Find one to unlock the Nether</red>",
                    )

                Material.END_PORTAL ->
                    PortalData(
                        World.Environment.THE_END,
                        NullscapeKeyItem,
                        "<mango>You need the Nullscape Key!</gradient>",
                        "<red>Find one to unlock the End</red>",
                    )

                else -> return
            }

        val fromEnvironment = event.location.world?.environment ?: return
        if (fromEnvironment == portalInfo.targetEnvironment) return

        if (player.inventory.contains(portalInfo.keyItem())) return

        player.portalCooldown = REJECT_COOLDOWN_TICKS
        rejectPlayer(player, event.location, portalInfo.title, portalInfo.subtitle)
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.TRIAL_SPAWNER) return

        val item = event.item ?: return
        val itemModel = item.getData(DataComponentTypes.ITEM_MODEL) ?: return

        if (itemModel == IncendiumKeyItem.key || itemModel == NullscapeKeyItem.key) {
            event.isCancelled = true
        }
    }

    /**
     * Determines if the given location is an Overworld location outside the spawn protection zone.
     * Portals in the Nether (or any other dimension) are never cancelled.
     *
     * @param location The location to check.
     * @return `true` if the portal should be cancelled, `false` otherwise.
     */
    private fun cancelPortal(location: Location): Boolean {
        val world = location.world ?: return true
        if (world.environment != World.Environment.NORMAL) return false
        val radius = instance.server.spawnRadius
        return radius > 0 && location.distanceSquared(world.spawnLocation) > radius * radius
    }

    /**
     * Checks if a player traveling to a specific dimension has the required key item.
     * If the player doesn't have the key, the portal travel is cancelled.
     * If the player has the key, one key is consumed upon successful teleportation.
     * Players in creative or spectator mode are exempt from the key requirement.
     *
     * @param event The [PlayerPortalEvent] to check.
     * @param targetEnvironment The target world environment to check for.
     * @param keyItem The required key item.
     * @param title The MiniMessage formatted title to display.
     * @param subtitle The MiniMessage formatted subtitle to display.
     * @return `true` if the player can travel (has key, is in creative/spectator mode, or is not traveling to the target dimension), `false` otherwise.
     */
    private fun requireKey(
        event: PlayerPortalEvent,
        targetEnvironment: World.Environment,
        keyItem: ItemInterface,
        title: String,
        subtitle: String,
    ): Boolean {
        if (event.player.gameMode == GameMode.CREATIVE || event.player.gameMode == GameMode.SPECTATOR) {
            return true
        }

        val toWorld = event.to.world ?: return false

        if (toWorld.environment != targetEnvironment) {
            return true
        }

        if (!event.player.inventory.contains(keyItem())) {
            event.isCancelled = true
            rejectPlayer(event.player, event.from, title, subtitle)
            return false
        }

        val keyItemStack = keyItem()
        val itemSlot = event.player.inventory.first(keyItemStack)
        if (itemSlot != -1) {
            val item = event.player.inventory.getItem(itemSlot) ?: return false
            if (item.amount > 1) {
                item.amount--
            } else {
                event.player.inventory.setItem(itemSlot, null)
            }
        }

        return true
    }

    /**
     * Rejects a player from a portal: shows the key requirement as a title with subtitle, pushes
     * them away from the portal, and bursts reverse-portal particles around them.
     *
     * The pushback direction is computed from the portal block toward the player (falling back to
     * the inverse of the player's look direction when the player is centered on the block), so the
     * player is always pushed out of the portal regardless of facing.
     *
     * @param player The player being rejected.
     * @param portalLocation The location of the portal block the player entered.
     * @param title The MiniMessage formatted title to display.
     * @param subtitle The MiniMessage formatted subtitle to display.
     */
    private fun rejectPlayer(
        player: Player,
        portalLocation: Location,
        title: String,
        subtitle: String,
    ) {
        player.showTitle(
            Title.title(
                MM.deserialize(title),
                MM.deserialize(subtitle),
            ),
        )
        val away =
            player
                .location
                .toVector()
                .subtract(portalLocation.toCenterLocation().toVector())
                .setY(0.0)
        val direction =
            if (away.lengthSquared() < 1.0e-6) {
                player
                    .location
                    .direction
                    .setY(0)
                    .multiply(-1.0)
            } else {
                away.normalize()
            }
        player.velocity =
            direction
                .multiply(PUSHBACK_STRENGTH)
                .setY(PUSHBACK_UPWARD)
        player.world.spawnParticle(
            Particle.REVERSE_PORTAL,
            player.location.add(0.0, 1.0, 0.0),
            REJECT_PARTICLE_COUNT,
            REJECT_PARTICLE_SPREAD,
            REJECT_PARTICLE_SPREAD,
            REJECT_PARTICLE_SPREAD,
        )
    }

    /**
     * Cancels Nether portal teleports originating in the Nether and teleports the player to the
     * Overworld spawn instead, preventing vanilla portal search/creation at the destination.
     * Portals in the Overworld (spawn-protected) teleport normally. The teleport is scheduled one
     * tick later, as teleporting a player while they are still inside a portal in the same tick is
     * unreliable. The destination Y coordinate is adjusted to the highest solid block to account
     * for flat worlds with non-standard spawn heights.
     *
     * @param event The [PlayerPortalEvent] to cancel.
     */
    private fun returnToSpawn(event: PlayerPortalEvent) {
        if (event.from.world?.environment != World.Environment.NETHER) return
        val overworld =
            instance
                .server
                .worlds
                .find { it.environment == World.Environment.NORMAL } ?: return
        event.isCancelled = true
        event.player.lastNetherPortal = findPortalBlock(event.from)
        val destination = overworld.spawnLocation.toSurface()
        instance.server.scheduler.runTask(
            instance,
            Runnable { event.player.teleport(destination) },
        )
    }

    /**
     * Finds the NETHER_PORTAL block at or adjacent to the given location. When a player teleports
     * through a portal, their position is inside the portal column, but may be one block below or
     * above the nearest portal block depending on frame shape, so a small vertical scan is used.
     *
     * @param location The location to search around.
     * @return The portal block's location, or the given location if no portal block is found.
     */
    private fun findPortalBlock(location: Location): Location {
        val block = location.block
        if (block.type == Material.NETHER_PORTAL) return block.location
        for (dy in -1..1) {
            val candidate = block.getRelative(0, dy, 0)
            if (candidate.type == Material.NETHER_PORTAL) return candidate.location
        }
        return location
    }

    /**
     * Redirects an Overworld to Nether portal teleport to the player's last used Nether-side
     * portal, if one is stored. This effectively links the player's trips so they resume at their
     * own portal in the Nether instead of generating a new vanilla portal exit.
     *
     * @param event The [PlayerPortalEvent] to redirect.
     */
    private fun returnToLastLocation(event: PlayerPortalEvent) {
        if (event.from.world?.environment != World.Environment.NORMAL) return
        val lastPortal = event.player.lastNetherPortal ?: return
        if (lastPortal.world.environment != World.Environment.NETHER) return
        if (lastPortal.block.type != Material.NETHER_PORTAL) {
            event.player.lastNetherPortal = null
            return
        }
        event.to = lastPortal.toCenterLocation()
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
