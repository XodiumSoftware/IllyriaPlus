package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.view.AnvilView
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling item disenchantment within the system. */
@Suppress("UnstableApiUsage")
internal object DisenchantmentMechanic : MechanicInterface {
    private const val BASE_COST: Double = 2.0
    private const val COST_MULTIPLIER: Double = 1.0

    @EventHandler(ignoreCancelled = true)
    fun on(event: PrepareAnvilEvent) = prepareDisenchant(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: InventoryClickEvent) = handleDisenchantClick(event)

    /**
     * Prepares the anvil result when valid disenchant inputs are present.
     *
     * @param event The PrepareAnvilEvent triggered by placing items in an anvil.
     */
    private fun prepareDisenchant(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val view = event.view

        if (inventory.viewers.isEmpty()) return

        val firstItem = inventory.getItem(0) ?: return
        val secondItem = inventory.getItem(1) ?: return
        val enchantments = collectDisenchantableEnchantments(firstItem, secondItem) ?: return

        event.result = createEnchantedBook(enchantments)
        view.repairCost = calculateCost(enchantments)

        instance.server.scheduler.runTask(
            instance,
            Runnable { view.repairCost = calculateCost(enchantments) },
        )
    }

    /**
     * Handles taking the disenchant result from an anvil.
     *
     * @param event The InventoryClickEvent triggered by clicking the anvil result slot.
     */
    private fun handleDisenchantClick(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.ANVIL) return
        if (event.rawSlot != 2) return
        if (event.action !in setOf(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF)) return

        val player = event.whoClicked as? Player ?: return

        if (!player.itemOnCursor.type.isAir) return

        val view = event.view as? AnvilView ?: return
        val inventory = event.inventory as AnvilInventory
        val result = inventory.getItem(2) ?: return

        if (result.type != Material.ENCHANTED_BOOK) return

        val firstItem = inventory.getItem(0) ?: return
        val secondItem = inventory.getItem(1) ?: return
        val enchantments = collectDisenchantableEnchantments(firstItem, secondItem) ?: return
        val cost = view.repairCost

        if (player.gameMode != GameMode.CREATIVE && player.level < cost) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        inventory.setItem(0, firstItem.clone().apply { enchantments.keys.forEach { removeEnchantment(it) } })
        inventory.setItem(1, consumeBook(secondItem))
        player.setItemOnCursor(result)

        if (player.gameMode != GameMode.CREATIVE) player.giveExpLevels(-cost)
    }

    /**
     * Creates an enchanted book containing the provided enchantments.
     *
     * @param enchantments The enchantments to store on the book.
     * @return The created enchanted book.
     */
    private fun createEnchantedBook(enchantments: Map<Enchantment, Int>): ItemStack =
        ItemStack.of(Material.ENCHANTED_BOOK).apply {
            setData(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantments.itemEnchantments(enchantments))
        }

    /**
     * Consumes one book from the provided stack.
     *
     * @param book The book stack to consume from.
     * @return The remaining book stack, or air if fully consumed.
     */
    private fun consumeBook(book: ItemStack): ItemStack =
        if (book.amount > 1) book.clone().apply { amount -= 1 } else ItemStack.of(Material.AIR)

    /**
     * Validates the anvil inputs and returns the enchantments that can be extracted.
     *
     * @param firstItem The enchanted item in slot 0.
     * @param secondItem The book in slot 1.
     * @return A map of enchantments and levels, or null if the inputs are invalid.
     */
    private fun collectDisenchantableEnchantments(
        firstItem: ItemStack,
        secondItem: ItemStack,
    ): Map<Enchantment, Int>? {
        if (firstItem.type.isAir ||
            secondItem.type.isAir ||
            firstItem.type == Material.ENCHANTED_BOOK ||
            secondItem.type != Material.BOOK ||
            secondItem.hasData(DataComponentTypes.STORED_ENCHANTMENTS)
        ) {
            return null
        }

        return firstItem.enchantments.filterKeys { !it.isCursed }.takeIf { it.isNotEmpty() }
    }

    /**
     * Calculates the XP cost for the disenchant operation.
     *
     * @param enchantments The enchantments being extracted.
     * @return The total XP cost in levels.
     */
    private fun calculateCost(enchantments: Map<Enchantment, Int>): Int =
        enchantments
            .values
            .sortedDescending()
            .foldIndexed(BASE_COST)
            { index, total, level -> total + level * (COST_MULTIPLIER + index * COST_MULTIPLIER) }
            .toInt()
}
