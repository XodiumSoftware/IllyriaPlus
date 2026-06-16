package org.xodium.illyriaplus.enchantments.utility

import com.destroystokyo.paper.MaterialTags
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling vinemine enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object VinemineEnchantment : EnchantmentInterface {
    private val DIRECTIONS =
        arrayOf(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.NORTH,
        )
    private val LEVEL_TO_MAX_BLOCKS = mapOf(1 to 16, 2 to 32, 3 to 48)

    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(3)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(25, 5))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(75, 10))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    @EventHandler
    fun on(event: BlockBreakEvent) {
        val player = event.player

        if (player.gameMode == GameMode.CREATIVE) return

        val itemInHand = player.inventory.itemInMainHand

        if (!itemInHand.containsEnchantment(get())) return

        val block = event.block

        if (!MaterialTags.ORES.isTagged(block.type)) return

        val blockType = block.type
        val enchantLevel = itemInHand.getEnchantmentLevel(get())
        val connectedBlocks = findConnectedBlocks(block, blockType, LEVEL_TO_MAX_BLOCKS[enchantLevel] ?: return)

        if (connectedBlocks.size <= 1) return

        val hasTetherEnchant = itemInHand.containsEnchantment(TetherEnchantment.get())
        var currentDamage = itemInHand.getData(DataComponentTypes.DAMAGE) ?: 0

        for (connectedBlock in connectedBlocks) {
            if (connectedBlock == block) continue
            if (hasTetherEnchant) {
                val drops = connectedBlock.getDrops(itemInHand, player)

                connectedBlock.type = Material.AIR

                if (drops.isNotEmpty()) {
                    val inventory = player.inventory

                    for (drop in drops) {
                        val remaining = inventory.addItem(drop)

                        for (remainingItem in remaining.values) {
                            connectedBlock.world.dropItem(connectedBlock.location, remainingItem)
                        }
                    }
                }
            } else {
                connectedBlock.breakNaturally(itemInHand)
            }

            currentDamage++

            if (currentDamage >= itemInHand.type.maxDurability.toInt()) break
        }

        if (currentDamage > (itemInHand.getData(DataComponentTypes.DAMAGE) ?: 0)) {
            itemInHand.setData(DataComponentTypes.DAMAGE, currentDamage)
        }
    }

    /**
     * Finds all connected [Block]s of the same type starting from the given [Block].
     *
     * @param startBlock The starting [Block].
     * @param targetType The [Material] type of block to search for.
     * @param maxBlocks The maximum number of blocks to find.
     * @return A [List] of connected [Block]s of the same type.
     */
    private fun findConnectedBlocks(
        startBlock: Block,
        targetType: Material,
        maxBlocks: Int,
    ): List<Block> {
        val visited = LinkedHashSet<Block>(maxBlocks)
        val queue = ArrayDeque<Block>()

        queue.add(startBlock)
        visited.add(startBlock)

        while (queue.isNotEmpty() && visited.size < maxBlocks) {
            val current = queue.removeFirst()

            for (direction in DIRECTIONS) {
                val neighbor = current.getRelative(direction)

                if (visited.size >= maxBlocks) break
                if (neighbor.type == targetType && visited.add(neighbor)) queue.add(neighbor)
            }
        }

        return visited.toList()
    }
}
