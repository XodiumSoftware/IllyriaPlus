package org.xodium.illyriaplus.enchantments.vanilla

import org.bukkit.Tag
import org.bukkit.block.data.Ageable
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.enchantments.vanilla.FortuneEnchantment.MIN_REPLANT_LEVEL

/** Represents an object handling fortune enchantment implementation within the system. */
internal object FortuneEnchantment : EnchantmentInterface {
    /** Minimum Fortune level required to trigger auto-replanting of crops. */
    private const val MIN_REPLANT_LEVEL = 2

    @EventHandler
    fun on(event: BlockBreakEvent) {
        if (!isValidItem(event.player.inventory.itemInMainHand)) return

        val block = event.block
        val ageable = block.blockData as? Ageable ?: return

        if (ageable.age < ageable.maximumAge) return

        instance.server.scheduler.runTaskLater(
            instance,
            Runnable { block.blockData = ageable.apply { age = 0 } },
            2,
        )
    }

    /**
     * Checks if the item is a hoe with at least [MIN_REPLANT_LEVEL] Fortune.
     *
     * @param item The item to check.
     * @return `true` if the item is a hoe with sufficient Fortune, otherwise `false`.
     */
    private fun isValidItem(item: ItemStack): Boolean =
        Tag.ITEMS_HOES.isTagged(item.type) && item.getEnchantmentLevel(Enchantment.FORTUNE) >= MIN_REPLANT_LEVEL
}
