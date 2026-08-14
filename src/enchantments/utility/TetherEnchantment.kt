package org.xodium.illyriaplus.enchantments.utility

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling tether enchantment implementation within the system. */
internal object TetherEnchantment : EnchantmentInterface {
    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(1)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(25, 0))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(75, 0))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    @EventHandler(ignoreCancelled = true)
    fun on(event: BlockDropItemEvent) {
        val player = event.player

        if (!player.inventory.itemInMainHand.containsEnchantment(get())) return

        transferItemEntitiesToInventory(player, event.items)
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        val itemInHand = player.inventory.itemInMainHand

        if (!itemInHand.containsEnchantment(get())) return

        val xp = event.droppedExp

        if (xp > 0) {
            event.droppedExp = 0
            player.giveExp(xp)
        }
    }

    /**
     * Transfers item entities to a player's inventory using the same removeIf pattern.
     *
     * @param player The player whose inventory to transfer to
     * @param items The mutable collection of item entities to transfer
     */
    private fun transferItemEntitiesToInventory(
        player: Player,
        items: MutableCollection<Item>,
    ) {
        items.removeIf { item ->
            val remaining = player.inventory.addItem(item.itemStack)
            val remainingItem = remaining[0] ?: return@removeIf true

            remainingItem.takeIf { it.amount > 0 }?.let { item.itemStack = it } == null
        }
    }
}
