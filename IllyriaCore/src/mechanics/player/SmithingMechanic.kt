package org.xodium.illyriacore.mechanics.player

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.view.AnvilView
import org.xodium.illyriacore.mechanics.MechanicInterface

/**
 * Represents a mechanic handling tiered axe forging via an anvil.
 * Allows upgrading axes through metal tiers while preserving enchantments.
 *
 * Tiers: Stone → Copper → Iron → Golden → Diamond.
 */
internal object SmithingMechanic : MechanicInterface {
    private val UPGRADES =
        sortedMapOf(
            Material.STONE_AXE to Upgrade(Material.COPPER_AXE, Material.COPPER_INGOT, 5),
            Material.COPPER_AXE to Upgrade(Material.IRON_AXE, Material.IRON_INGOT, 10),
            Material.IRON_AXE to Upgrade(Material.GOLDEN_AXE, Material.GOLD_INGOT, 15),
            Material.GOLDEN_AXE to Upgrade(Material.DIAMOND_AXE, Material.DIAMOND, 20),
        )

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val firstItem = inventory.getItem(0) ?: return
        val secondItem = inventory.getItem(1) ?: return
        val upgrade = UPGRADES[firstItem.type] ?: return

        if (secondItem.type != upgrade.material) return

        event.result =
            ItemStack.of(upgrade.result).apply {
                firstItem.enchantments.forEach { addEnchantment(it.key, it.value) }
            }
        event.view.repairCost = upgrade.cost
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun on(event: InventoryClickEvent) {
        if (event.inventory !is AnvilInventory) return
        if (event.rawSlot != 2) return
        if (event.action !in setOf(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF)) return

        val inventory = event.inventory as AnvilInventory
        val result = inventory.getItem(2) ?: return
        val firstItem = inventory.getItem(0) ?: return
        val upgrade = UPGRADES[firstItem.type] ?: return

        if (result.type != upgrade.result) return

        val player = event.whoClicked as? Player ?: return
        val view = event.view as? AnvilView ?: return
        val cost = view.repairCost

        if (player.gameMode != GameMode.CREATIVE && player.level < cost) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        inventory.setItem(0, ItemStack.of(Material.AIR))
        inventory.setItem(1, consumeItem(inventory.getItem(1) ?: return))
        @Suppress("UsePropertyAccessSyntax")
        player.setItemOnCursor(result)

        if (player.gameMode != GameMode.CREATIVE) player.giveExpLevels(-cost)
    }

    /**
     * Consumes one item from the provided stack.
     *
     * @param item the item stack to consume from
     * @return the remaining stack, or air if fully consumed
     */
    private fun consumeItem(item: ItemStack): ItemStack =
        if (item.amount > 1) item.clone().apply { amount -= 1 } else ItemStack.of(Material.AIR)

    /**
     * Represents a single axe tier upgrade.
     *
     * @property result the resulting axe material
     * @property material the material consumed to perform the upgrade
     * @property cost the XP level cost for the upgrade
     */
    private data class Upgrade(
        val result: Material,
        val material: Material,
        val cost: Int,
    )
}
