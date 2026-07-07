package org.xodium.illyriaplus.enchantments.vanilla

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling feather falling enchantment implementation within the system. */
internal object FeatherFallingEnchantment : EnchantmentInterface {
    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) {
        when {
            event.action != Action.PHYSICAL -> return
            event.clickedBlock?.type != Material.FARMLAND -> return
            !isValidItem(event.player.inventory.boots) -> return
            else -> event.isCancelled = true
        }
    }

    /**
     * Checks if the item is foot armor with the Feather Falling enchantment.
     *
     * @param item The item to check.
     * @return `true` if the item is foot armor with Feather Falling, otherwise `false`.
     */
    private fun isValidItem(item: ItemStack): Boolean =
        Tag.ITEMS_FOOT_ARMOR.isTagged(item.type) && item.containsEnchantment(Enchantment.FEATHER_FALLING)
}
