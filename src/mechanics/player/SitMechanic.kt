package org.xodium.illyriaplus.mechanics.player

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.Vector
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.mechanics.player.SitMechanic.occupiedBlocks
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/** Represents a mechanic handling player sitting within the system. */
@OptIn(ExperimentalUuidApi::class)
internal object SitMechanic : MechanicInterface {
    private val sittingPlayers = mutableMapOf<Uuid, ArmorStand>()
    private val occupiedBlocks = mutableMapOf<Location, Uuid>()
    private val blockCenterOffset = Vector(0.5, 0.5, 0.5)
    private val playerStandUpOffset = Vector(0.0, 0.5, 0.0)

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.OAK_STAIRS)
                .setName(MM.deserialize("<mango>Sit Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Sit Anywhere</yellow> <firewatch>></gradient> " +
                            "<white>Right-click bottom stairs/slabs</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Stand Up</yellow> <firewatch>></gradient> " +
                            "<white>Take damage, break block, or dismount</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.PLAYER

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) = playerInteract(event)

    @EventHandler
    fun on(event: EntityDismountEvent) = entityDismount(event)

    @EventHandler
    fun on(event: PlayerQuitEvent) = playerQuit(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) = entityDamage(event)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun on(event: BlockBreakEvent) = blockBreak(event)

    /**
     * Handles player interaction to initiate sitting.
     *
     * @param event The [PlayerInteractEvent] triggered by the player.
     */
    private fun playerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (player.gameMode != GameMode.SURVIVAL) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (player.isSneaking) return
        if (player.isInsideVehicle) return
        if (player.inventory.itemInMainHand.type != Material.AIR) return

        val block = event.clickedBlock ?: return
        val blockData = block.blockData
        val isSitTarget =
            (blockData is Stairs && blockData.half == Bisected.Half.BOTTOM) ||
                (blockData is Slab && blockData.type == Slab.Type.BOTTOM)

        if (!isSitTarget) return
        if (block.getRelative(BlockFace.UP).type.isCollidable) return
        if (block.location in occupiedBlocks) return

        event.isCancelled = true
        sit(player, block.location.clone().add(blockCenterOffset))
    }

    /**
     * Handles dismounting from the sitting ArmorStand.
     *
     * @param event The [EntityDismountEvent] triggered when the player dismounts.
     */
    private fun entityDismount(event: EntityDismountEvent) {
        val player = event.entity as? Player ?: return

        sittingPlayers.remove(player.uniqueId.toKotlinUuid())?.let { armorStand ->
            player.teleport(
                armorStand.location.clone().add(playerStandUpOffset).apply {
                    yaw = player.location.yaw
                    pitch = player.location.pitch
                },
            )

            instance.server.scheduler.runTask(instance, Runnable { armorStand.removeSeat() })
        }
    }

    /**
     * Handles clean-up when a player quits.
     *
     * @param event The [PlayerQuitEvent] triggered when the player leaves the server.
     */
    private fun playerQuit(event: PlayerQuitEvent) {
        sittingPlayers.remove(event.player.uniqueId.toKotlinUuid())?.removeSeat()
    }

    /**
     * Handles player damage while sitting.
     *
     * @param event The [EntityDamageEvent] triggered when the player takes damage.
     */
    private fun entityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        sittingPlayers.remove(player.uniqueId.toKotlinUuid())?.let { armorStand ->
            armorStand.removePassenger(player)
            armorStand.removeSeat()
        }
    }

    /**
     * Handles block break events to remove sitting ArmorStands on broken blocks.
     *
     * @param event The [BlockBreakEvent] triggered when a block is broken.
     */
    private fun blockBreak(event: BlockBreakEvent) {
        val brokenBlockLocation = event.block.location

        sittingPlayers.entries.removeIf { (_, armorStand) ->
            (armorStand.blockLocation() == brokenBlockLocation).also { matches ->
                if (matches) {
                    armorStand.passengers
                        .filterIsInstance<Player>()
                        .forEach(armorStand::removePassenger)

                    armorStand.removeSeat()
                }
            }
        }
    }

    /**
     * Spawns an invisible ArmorStand at the given location and makes the player sit on it.
     *
     * @param player The [Player] who will be made to sit.
     * @param location The [Location] where the player should sit.
     */
    private fun sit(
        player: Player,
        location: Location,
    ) {
        val world = location.world ?: return
        val armorStand =
            world.spawn(location, ArmorStand::class.java) {
                it.isVisible = false
                it.setGravity(false)
                it.isSmall = true
                it.isMarker = true
                it.isInvulnerable = true
            }

        instance.server.scheduler.runTask(instance, Runnable { armorStand.addPassenger(player) })

        val playerId = player.uniqueId.toKotlinUuid()

        sittingPlayers[playerId] = armorStand
        occupiedBlocks[location.block.location] = playerId
    }

    /** Returns the [Location] of the block this [ArmorStand] is sitting on. */
    private fun ArmorStand.blockLocation(): Location =
        location
            .clone()
            .subtract(blockCenterOffset)
            .block.location

    /** Removes this [ArmorStand] and clears it from [occupiedBlocks]. */
    private fun ArmorStand.removeSeat() {
        occupiedBlocks.remove(blockLocation())
        remove()
    }
}
