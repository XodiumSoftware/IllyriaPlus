package org.xodium.illyriaplus.mechanics.player

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.view.AnvilView
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic that removes the vanilla anvil "Too Expensive!" limit. */
@Suppress("UnstableApiUsage")
internal object AnvilMechanic : MechanicInterface {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: InventoryOpenEvent) {
        configureAnvil(event.view as? AnvilView ?: return)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PrepareAnvilEvent) {
        prepareAnvil(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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

        event.result?.takeIf { !it.type.isAir }?.let { result ->
            view.renameText?.takeIf { it.isNotEmpty() }?.let { renameText ->
                result.editMeta { it.displayName(Utils.MM.deserialize(renameText)) }
                event.result = result
            }
        }

        instance.server.scheduler.runTask(instance) { _ ->
            if (player.gameMode == GameMode.CREATIVE) return@runTask
            if (view.topInventory != anvil) return@runTask

            val input2 = anvil.getItem(1)
            val instantBuild =
                input2 == null || input2.type == Material.AIR || view.repairCost < view.maximumRepairCost

            setInstantBuild(player, instantBuild)
        }
    }

    /**
     * Handles clicking the anvil result slot, applying the repair cost as level
     * deductions when the vanilla cost threshold (40) is exceeded.
     *
     * @param event the inventory click event
     */
    private fun handleInventoryClick(event: InventoryClickEvent) {
        if (event.inventory !is AnvilInventory) return
        if (event.rawSlot != 2) return

        val player = event.whoClicked as? Player ?: return

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
     * Resets the player's instant-build ability when an anvil inventory closes.
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
            else -> setInstantBuild(player, false)
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
     * Sends a Player Abilities packet to toggle the creative-mode (instant-build) bit,
     * allowing anvil interactions above the vanilla cost limit.
     *
     * @param player the player whose abilities should be updated
     * @param creativeMode whether instant-build should be enabled
     */
    private fun setInstantBuild(
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
}
