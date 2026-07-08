package org.xodium.illyriaplus.mechanics.player

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
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

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: InventoryClickEvent) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (player.gameMode == GameMode.CREATIVE) return
        if (event.inventory !is AnvilInventory) return

        setInstantBuild(player, false)
    }

    private fun configureAnvil(view: AnvilView) {
        view.maximumRepairCost = Int.MAX_VALUE
        view.bypassEnchantmentLevelRestriction(true)
    }

    private fun setInstantBuild(
        player: Player,
        instantBuild: Boolean,
    ) {
        val packet = PacketContainer(PacketType.Play.Server.ABILITIES)

        packet.booleans
            .write(0, player.isInvulnerable)
            .write(1, player.isFlying)
            .write(2, player.allowFlight)
            .write(3, instantBuild)
        packet.floats
            .write(0, player.flySpeed / 2)
            .write(1, player.walkSpeed / 2)

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
    }
}
