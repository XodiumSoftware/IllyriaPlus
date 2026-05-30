package org.xodium.illyriaplus.mechanics.world

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Openable
import org.bukkit.block.data.type.Door
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.AdjacentBlockData
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling openable blocks within the system. */
internal object OpenableMechanic : MechanicInterface {
    private const val KNOCK_SOUND_RADIUS: Double = 10.0
    private const val DOUBLE_DOOR_DELAY_TICKS: Long = 1

    private val possibleNeighbours: Set<AdjacentBlockData> =
        setOf(
            AdjacentBlockData(0, -1, Door.Hinge.RIGHT, BlockFace.EAST),
            AdjacentBlockData(0, 1, Door.Hinge.LEFT, BlockFace.EAST),
            AdjacentBlockData(1, 0, Door.Hinge.RIGHT, BlockFace.SOUTH),
            AdjacentBlockData(-1, 0, Door.Hinge.LEFT, BlockFace.SOUTH),
            AdjacentBlockData(0, 1, Door.Hinge.RIGHT, BlockFace.WEST),
            AdjacentBlockData(0, -1, Door.Hinge.LEFT, BlockFace.WEST),
            AdjacentBlockData(-1, 0, Door.Hinge.RIGHT, BlockFace.NORTH),
            AdjacentBlockData(1, 0, Door.Hinge.LEFT, BlockFace.NORTH),
        )
    private val ALLOWED_KNOCKING_GAME_MODES: Set<GameMode> = setOf(GameMode.SURVIVAL, GameMode.ADVENTURE)
    private val KNOCK_SOUND: Sound =
        Sound.sound(
            Key.key("entity.zombie.attack_wooden_door"),
            Sound.Source.HOSTILE,
            1.0f,
            1.0f,
        )

    override val faqTab = FaqTab.WORLD_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.DARK_OAK_DOOR)
                .setName(MM.deserialize("<mango>Openable Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Double Doors</yellow> <firewatch>></gradient> <white>Sync open/close together</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Knocking</yellow> <firewatch>></gradient> <white>Sneak + " +
                            "left-click with empty hand</white>",
                    ),
                ),
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) = handlePlayerInteract(event)

    /**
     * Handles block interactions and delegates to the correct click handler.
     *
     * @param event The [PlayerInteractEvent] triggered by the player.
     */
    private fun handlePlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        if (!isValidInteraction(event)) return

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> handleLeftClick(event, clickedBlock)
            Action.RIGHT_CLICK_BLOCK -> handleRightClick(event, clickedBlock)
            else -> return
        }
    }

    /**
     * Checks if the interaction event is valid for processing.
     *
     * @param event The [PlayerInteractEvent] to validate.
     * @return `true` if the interaction is valid, `false` otherwise.
     */
    private fun isValidInteraction(event: PlayerInteractEvent): Boolean =
        event.hand == EquipmentSlot.HAND &&
            event.useInteractedBlock() != Event.Result.DENY &&
            event.useItemInHand() != Event.Result.DENY

    /**
     * Handles the left-click interaction with doors and gates, playing a knock sound if applicable.
     *
     * @param event The [PlayerInteractEvent] triggering the interaction.
     * @param block The [Block] representing the door or gate being interacted with.
     */
    private fun handleLeftClick(
        event: PlayerInteractEvent,
        block: Block,
    ) {
        if (canKnock(event.player) && isKnockableBlock(block.blockData)) playKnockSound(block)
    }

    /**
     * Handles the right-click interaction with doors, toggling double door state.
     *
     * @param event The [PlayerInteractEvent] triggering the interaction.
     * @param block The [Block] representing the door being interacted with.
     */
    private fun handleRightClick(
        event: PlayerInteractEvent,
        block: Block,
    ) {
        if (block.blockData !is Door) return
        if (event.player.isSneaking) return

        processDoubleDoorInteraction(block)
    }

    /**
     * Checks if the [Player] can knock on the block based on their game mode and interaction conditions.
     *
     * @param player The [Player] attempting to knock.
     * @return `true` if the player can knock, `false` otherwise.
     */
    private fun canKnock(player: Player): Boolean {
        if (player.gameMode !in ALLOWED_KNOCKING_GAME_MODES) return false
        if (!player.isSneaking) return false
        if (player.inventory.itemInMainHand.type != Material.AIR) return false

        return true
    }

    /**
     * Checks if the [BlockData] is of a type that can be knocked on.
     *
     * @param data The [BlockData] to check.
     * @return `true` if the block can be knocked on, `false` otherwise.
     */
    private fun isKnockableBlock(data: BlockData): Boolean = data is Openable

    /**
     * Plays the knocking sound to all nearby players around the specified [Block].
     *
     * @param block The [Block] where the knock sound originates.
     */
    private fun playKnockSound(block: Block) {
        block.world
            .getNearbyPlayers(block.location, KNOCK_SOUND_RADIUS)
            .forEach { it.playSound(KNOCK_SOUND) }
    }

    /**
     * Processes the interaction with double doors, syncing their open/closed state.
     *
     * @param block The [Block] representing the door being interacted with.
     */
    private fun processDoubleDoorInteraction(block: Block) {
        val doorBottomBlock = getDoorBottomBlock(block) ?: return
        val otherDoorBlock = findAdjacentDoor(doorBottomBlock, block) ?: return
        val otherDoor = otherDoorBlock.blockData as? Door ?: return

        syncDoorState(block, otherDoorBlock, !otherDoor.isOpen)
    }

    /**
     * Synchronizes the open/closed state of two adjacent doors.
     *
     * @param sourceBlock The [Block] representing the source door.
     * @param targetBlock The [Block] representing the target door to sync.
     * @param open The desired open state for the target door.
     * @param delay The delay in ticks before syncing (defaults to DOUBLE_DOOR_DELAY_TICKS).
     */
    private fun syncDoorState(
        sourceBlock: Block,
        targetBlock: Block,
        open: Boolean,
        delay: Long = DOUBLE_DOOR_DELAY_TICKS,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                val currentSource = sourceBlock.blockData as? Door ?: return@Runnable
                val currentTarget = targetBlock.blockData as? Door ?: return@Runnable

                if (currentSource.isOpen != currentTarget.isOpen) {
                    currentTarget.isOpen = open
                    targetBlock.blockData = currentTarget
                }
            },
            delay,
        )
    }

    /**
     * Gets the bottom block of a door.
     *
     * @param block The [Block] to get the bottom half from.
     * @return The bottom [Block] of the door, or `null` if not a door.
     */
    private fun getDoorBottomBlock(block: Block): Block? {
        val door = block.blockData as? Door ?: return null

        return if (door.half == Bisected.Half.BOTTOM) {
            block
        } else {
            block.getRelative(BlockFace.DOWN)
        }
    }

    /**
     * Finds an adjacent door that forms a double door pair with the given door.
     *
     * @param doorBlock The [Block] containing the door to check from.
     * @param originalBlock The original [Block] clicked (for material check).
     * @return The adjacent [Block] containing the paired door, or `null` if none found.
     */
    private fun findAdjacentDoor(
        doorBlock: Block,
        originalBlock: Block,
    ): Block? {
        val door = doorBlock.blockData as? Door ?: return null

        return possibleNeighbours
            .map { it to doorBlock.getRelative(it.offsetX, 0, it.offsetZ).blockData as? Door }
            .firstOrNull { (neighbour, otherDoor) ->
                otherDoor?.let { neighbour.matchesDoorPair(it, door, originalBlock.type) } == true
            }?.let { (neighbour, _) -> doorBlock.getRelative(neighbour.offsetX, 0, neighbour.offsetZ) }
    }
}
