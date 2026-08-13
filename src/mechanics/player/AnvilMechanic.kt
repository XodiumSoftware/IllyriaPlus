package org.xodium.illyriaplus.mechanics.player

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.*
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.view.AnvilView
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling custom anvil operations, including disenchantment and cost-limit bypass. */
@Suppress("UnstableApiUsage")
internal object AnvilMechanic : MechanicInterface {
    private const val BASE_COST: Double = 2.0
    private const val COST_MULTIPLIER: Double = 1.0

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: InventoryOpenEvent) {
        configureAnvil(event.view as? AnvilView ?: return)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PrepareAnvilEvent) {
        prepareAnvil(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun on(event: InventoryClickEvent) {
        handleInventoryClick(event)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: InventoryCloseEvent) {
        handleInventoryClose(event.player as? Player ?: return, event.inventory)
    }

    /**
     * Configures the anvil view and applies any rename text using MiniMessage.
     * Also updates the player's instant-build ability to bypass the "Too Expensive!" limit.
     *
     * @param event the prepare anvil event
     */
    private fun prepareAnvil(event: PrepareAnvilEvent) {
        val view = event.view
        val anvil = event.inventory
        val player = view.player as? Player ?: return

        configureAnvil(view)

        val disenchantHandled = prepareDisenchant(event)

        if (!disenchantHandled) applyAnvilRename(event)

        instance.server.scheduler.runTask(instance) { _ ->
            if (player.gameMode == GameMode.CREATIVE) return@runTask
            if (view.topInventory != anvil) return@runTask

            val input2 = anvil.getItem(1)
            val instantBuild =
                input2 == null || input2.type == Material.AIR || view.repairCost < view.maximumRepairCost

            setCreativeMode(player, instantBuild)
        }
    }

    /**
     * Prepares the anvil result when valid disenchant inputs are present.
     *
     * @param event the PrepareAnvilEvent triggered by placing items in an anvil
     * @return true if the event was handled as a disenchant operation
     */
    private fun prepareDisenchant(event: PrepareAnvilEvent): Boolean {
        val inventory = event.inventory
        val view = event.view

        if (inventory.viewers.isEmpty()) return false

        val firstItem = inventory.getItem(0) ?: return false
        val secondItem = inventory.getItem(1) ?: return false
        val enchantments = collectDisenchantableEnchantments(firstItem, secondItem) ?: return false
        val transferred = resolveTransferredEnchantments(firstItem, enchantments)

        event.result = createEnchantedBook(transferred)
        view.repairCost = calculateCost(transferred)

        return true
    }

    /**
     * Handles clicking the anvil result slot, applying custom disenchant logic or
     * the repair cost as level deductions when the vanilla cost threshold (40) is exceeded.
     *
     * @param event the inventory click event
     */
    private fun handleInventoryClick(event: InventoryClickEvent) {
        if (event.inventory !is AnvilInventory) return
        if (event.rawSlot != 2) return

        val player = event.whoClicked as? Player ?: return

        if (handleDisenchantClick(event, player)) return

        if (player.gameMode == GameMode.CREATIVE) return

        val view = event.view as? AnvilView ?: return
        val cost = view.repairCost

        if (player.level < cost) {
            event.isCancelled = true
            return
        }

        if (cost >= 40) {
            view.repairCost = 0
            instance.server.scheduler.runTask(instance) { _ -> player.giveExpLevels(-cost) }
        }
    }

    /**
     * Handles taking the disenchant result from an anvil.
     *
     * @param event the InventoryClickEvent triggered by clicking the anvil result slot
     * @param player the player interacting with the anvil
     * @return true if the event was handled as a disenchant operation
     */
    private fun handleDisenchantClick(
        event: InventoryClickEvent,
        player: Player,
    ): Boolean {
        if (event.action !in setOf(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF)) return false
        if (!player.itemOnCursor.type.isAir) return false

        val view = event.view as? AnvilView ?: return false
        val inventory = event.inventory as? AnvilInventory ?: return false
        val result = inventory.getItem(2) ?: return false

        if (result.type != Material.ENCHANTED_BOOK) return false

        val firstItem = inventory.getItem(0) ?: return false
        val secondItem = inventory.getItem(1) ?: return false
        val enchantments = collectDisenchantableEnchantments(firstItem, secondItem) ?: return false
        val transferred = resolveTransferredEnchantments(firstItem, enchantments)
        val cost = view.repairCost

        if (player.gameMode != GameMode.CREATIVE && player.level < cost) {
            event.isCancelled = true
            return true
        }

        event.isCancelled = true
        inventory.setItem(0, removeEnchantments(firstItem, transferred.keys))
        inventory.setItem(1, consumeBook(secondItem))
        player.setItemOnCursor(result)

        if (player.gameMode != GameMode.CREATIVE) player.giveExpLevels(-cost)
        return true
    }

    /**
     * Resets the player's creative mode ability when an anvil inventory closes.
     *
     * @param player the player closing the inventory
     * @param inventory the inventory being closed
     */
    private fun handleInventoryClose(
        player: Player,
        inventory: Inventory,
    ) {
        when {
            player.gameMode == GameMode.CREATIVE -> return
            inventory !is AnvilInventory -> return
            else -> setCreativeMode(player, false)
        }
    }

    /**
     * Removes the vanilla anvil cost limit and allows exceeding enchantment level caps.
     *
     * @param view the anvil view to configure
     */
    private fun configureAnvil(view: AnvilView) {
        view.maximumRepairCost = Int.MAX_VALUE
        view.bypassEnchantmentLevelRestriction(true)
    }

    /**
     * Applies the player's rename text to the anvil result using MiniMessage.
     *
     * @param event the prepare anvil event
     */
    private fun applyAnvilRename(event: PrepareAnvilEvent) {
        val result = event.result?.takeIf { !it.type.isAir } ?: return
        val renameText = event.view.renameText?.takeIf { it.isNotEmpty() } ?: return

        result.setData(DataComponentTypes.CUSTOM_NAME, Utils.MM.deserialize(renameText))
        event.result = result
    }

    /**
     * Sends a Player Abilities packet to toggle the creative-mode bit,
     * allowing anvil interactions above the vanilla cost limit.
     *
     * @param player the player whose abilities should be updated
     * @param creativeMode whether instant-build should be enabled
     */
    private fun setCreativeMode(
        player: Player,
        creativeMode: Boolean,
    ) {
        PacketEvents.getAPI().playerManager.sendPacket(
            player,
            WrapperPlayServerPlayerAbilities(
                player.isInvulnerable,
                player.isFlying,
                player.allowFlight,
                creativeMode,
                player.flySpeed,
                player.walkSpeed,
            ),
        )
    }

    /**
     * Creates an enchanted book containing the provided enchantments.
     *
     * @param enchantments the enchantments to store on the book
     * @return the created enchanted book
     */
    private fun createEnchantedBook(enchantments: Map<Enchantment, Int>): ItemStack =
        ItemStack.of(Material.ENCHANTED_BOOK).apply {
            setData(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantments.itemEnchantments(enchantments))
        }

    /**
     * Consumes one book from the provided stack.
     *
     * @param book the book stack to consume from
     * @return the remaining book stack, or air if fully consumed
     */
    private fun consumeBook(book: ItemStack): ItemStack =
        if (book.amount > 1) book.clone().apply { amount -= 1 } else ItemStack.of(Material.AIR)

    /**
     * Removes the specified enchantments from the given item or enchanted book.
     *
     * @param item the item to remove enchantments from
     * @param enchantments the enchantments to remove
     * @return the modified item, or air if no enchantments remain on an enchanted book
     */
    private fun removeEnchantments(
        item: ItemStack,
        enchantments: Set<Enchantment>,
    ): ItemStack {
        val clone = item.clone()

        when (clone.type) {
            Material.ENCHANTED_BOOK -> {
                val stored =
                    clone
                        .getData(DataComponentTypes.STORED_ENCHANTMENTS)
                        ?.enchantments()
                        ?.toMutableMap()
                        ?: return clone

                enchantments.forEach { stored.remove(it) }

                if (stored.isEmpty()) return ItemStack.of(Material.AIR)

                clone.setData(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantments.itemEnchantments(stored))
            }

            else -> {
                enchantments.forEach { clone.removeEnchantment(it) }
            }
        }

        return clone
    }

    /**
     * Validates the anvil inputs and returns the enchantments that can be extracted.
     *
     * @param firstItem the enchanted item or book in slot 0
     * @param secondItem the book in slot 1
     * @return a map of enchantments and levels, or null if the inputs are invalid
     */
    private fun collectDisenchantableEnchantments(
        firstItem: ItemStack,
        secondItem: ItemStack,
    ): Map<Enchantment, Int>? {
        if (firstItem.type.isAir ||
            secondItem.type.isAir ||
            secondItem.type != Material.BOOK ||
            secondItem.hasData(DataComponentTypes.STORED_ENCHANTMENTS)
        ) {
            return null
        }

        val enchantments =
            when (firstItem.type) {
                Material.ENCHANTED_BOOK -> firstItem.getData(DataComponentTypes.STORED_ENCHANTMENTS)?.enchantments()
                else -> firstItem.enchantments
            } ?: return null

        val filtered = enchantments.filterKeys { !it.isCursed }

        if (filtered.isEmpty() || filtered.size == 1) return null

        return filtered
    }

    /**
     * Resolves which enchantments should be transferred based on the source item type.
     *
     * @param firstItem the source item in slot 0
     * @param enchantments the enchantments available on the source item
     * @return the enchantments to transfer to the result book
     */
    private fun resolveTransferredEnchantments(
        firstItem: ItemStack,
        enchantments: Map<Enchantment, Int>,
    ): Map<Enchantment, Int> =
        if (firstItem.type == Material.ENCHANTED_BOOK) selectTransferredEnchantment(enchantments) else enchantments

    /**
     * Selects the enchantment to transfer when splitting an enchanted book.
     *
     * @param enchantments the enchantments available on the source item/book
     * @return a map containing exactly one enchantment
     */
    private fun selectTransferredEnchantment(enchantments: Map<Enchantment, Int>): Map<Enchantment, Int> =
        enchantments.entries.first().let { mapOf(it.toPair()) }

    /**
     * Calculates the XP cost for the disenchant operation.
     *
     * @param enchantments the enchantments being extracted
     * @return the total XP cost in levels
     */
    private fun calculateCost(enchantments: Map<Enchantment, Int>): Int =
        enchantments
            .values
            .sortedDescending()
            .foldIndexed(BASE_COST.toInt()) { index, total, level ->
                total + (level * (COST_MULTIPLIER + index * COST_MULTIPLIER)).toInt()
            }
}
