package org.xodium.illyriaplus.enchantments.vanilla

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.CreatureSpawner
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.Item.spawnEgg
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling silk touch enchantment implementation within the system. */
internal object SilkTouchEnchantment : EnchantmentInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: BlockBreakEvent) {
        if (!isValidItem(event.player.inventory.itemInMainHand)) return

        when (event.block.type) {
            Material.SPAWNER -> handleSpawnerBreak(event)
            Material.BUDDING_AMETHYST -> handleBuddingAmethystBreak(event)
            else -> return
        }
    }

    /**
     * Handles breaking a spawner with Silk Touch.
     *
     * @param event The block break event.
     */
    private fun handleSpawnerBreak(event: BlockBreakEvent) {
        event.isDropItems = false
        event.expToDrop = 0

        val state = event.block.state

        if (state is CreatureSpawner) {
            event.block.world.dropItemNaturally(event.block.location, ItemStack.of(Material.SPAWNER))
            event.block.world.dropItemNaturally(event.block.location, state.spawnedType?.spawnEgg() ?: return)
        }
    }

    /**
     * Handles breaking a budding amethyst with Silk Touch.
     *
     * @param event The block break event.
     */
    private fun handleBuddingAmethystBreak(event: BlockBreakEvent) {
        event.isDropItems = false
        event.block.world.dropItemNaturally(event.block.location, ItemStack.of(Material.BUDDING_AMETHYST))
    }

    /**
     * Checks if the item is a pickaxe with Silk Touch.
     *
     * @param item The item to check.
     * @return `true` if the item is a pickaxe with Silk Touch, otherwise `false`.
     */
    private fun isValidItem(item: ItemStack): Boolean =
        Tag.ITEMS_PICKAXES.isTagged(item.type) && item.containsEnchantment(Enchantment.SILK_TOUCH)
}
