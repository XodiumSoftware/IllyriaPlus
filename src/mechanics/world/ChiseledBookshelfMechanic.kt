package org.xodium.illyriaplus.mechanics.world

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments
import io.papermc.paper.datacomponent.item.WrittenBookContent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.ChiseledBookshelf
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.bukkit.block.data.type.ChiseledBookshelf as ChiseledBookshelfData

/** Represents a mechanic handling chiseled bookshelf interactions within the system. */
@Suppress("UnstableApiUsage")
internal object ChiseledBookshelfMechanic : MechanicInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) = bookshelfInteraction(event)

    /**
     * Handles the interaction event where a player left-clicks a chiseled bookshelf
     * to display the item held in the targeted slot as an action bar message.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with the world.
     */
    private fun bookshelfInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (player.gameMode !in listOf(GameMode.SURVIVAL, GameMode.ADVENTURE)) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.LEFT_CLICK_BLOCK) return
        if (event.useInteractedBlock() == Event.Result.DENY) return

        val block = event.clickedBlock ?: return
        val bookshelf = block.state as? ChiseledBookshelf ?: return
        val facing = (block.blockData as? ChiseledBookshelfData)?.facing ?: return

        if (event.blockFace != facing) return

        val slot =
            player
                .rayTraceBlocks(6.0)
                ?.takeIf { it.hitBlock == block }
                ?.hitPosition
                ?.toSlot(block, facing) ?: return
        val item = bookshelf.inventory.getItem(slot)

        if (item == null || item.type.isAir) return

        player.sendActionBar(
            when {
                item.hasData(DataComponentTypes.WRITTEN_BOOK_CONTENT) -> {
                    renderBook(item.getData(DataComponentTypes.WRITTEN_BOOK_CONTENT)!!, item.type.key.key)
                }

                item.hasData(DataComponentTypes.STORED_ENCHANTMENTS) -> {
                    renderEnchantedBook(item.getData(DataComponentTypes.STORED_ENCHANTMENTS)!!)
                }

                else -> {
                    MM.deserialize("<white><sprite:items:item/${item.type.key.key}></white>")
                }
            },
        )
    }

    /**
     * Maps this hit [Vector] to a chiseled bookshelf slot index (0–5).
     * Slots are laid out in a 3×2 grid on the front face:
     * [0][1][2] (top row) / [3][4][5] (bottom row).
     *
     * @param block The clicked block.
     * @return The slot index, or null if the facing direction is unsupported.
     */
    private fun Vector.toSlot(
        block: Block,
        facing: BlockFace,
    ): Int? {
        val relX = x - block.x
        val relY = y - block.y
        val relZ = z - block.z
        val localX =
            when (facing) {
                BlockFace.SOUTH -> relX
                BlockFace.NORTH -> 1.0 - relX
                BlockFace.EAST -> 1.0 - relZ
                BlockFace.WEST -> relZ
                else -> return null
            }
        val col = (localX * 3).toInt().coerceIn(0, 2)
        val row = if (relY >= 0.5) 0 else 1

        return row * 3 + col
    }

    /**
     * Renders a written or writable book component.
     *
     * @param content The book content data.
     * @param itemKey The item key for the sprite.
     * @return The rendered component.
     */
    private fun renderBook(
        content: WrittenBookContent,
        itemKey: String,
    ): Component =
        MM
            .deserialize("<white><sprite:items:item/$itemKey></white>")
            .append(MM.deserialize(" <white>${content.title().raw()}</white>"))
            .append(MM.deserialize("<gray> by ${content.author()}</gray>"))

    /**
     * Renders an enchanted book component.
     *
     * @param enchantmentsData The stored enchantments' data.
     * @return The rendered component.
     */
    private fun renderEnchantedBook(enchantmentsData: ItemEnchantments): Component =
        MM
            .deserialize("<white><sprite:items:item/enchanted_book></white><gray> </gray>")
            .append(
                Component.join(
                    JoinConfiguration.separator(MM.deserialize("<gray>, </gray>")),
                    enchantmentsData.enchantments().entries.map { (enchantment, level) ->
                        enchantment.displayName(level)
                    },
                ),
            )
}
