package org.xodium.illyriaplus.enchantments.utility

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.EnchantmentUtils.displayName
import org.xodium.illyriaplus.interfaces.EnchantmentInterface

/** Represents an object handling verdance enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object VerdanceEnchantment : EnchantmentInterface {
    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(1)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(25, 0))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(75, 0))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    @EventHandler
    fun on(event: BlockBreakEvent) {
        val block = event.block
        val ageable = block.blockData as? Ageable ?: return
        val itemInHand = event.player.inventory.itemInMainHand

        if (ageable.age < ageable.maximumAge) return
        if (!itemInHand.containsEnchantment(get())) return

        instance.server.scheduler.runTaskLater(
            instance,
            Runnable { block.blockData = ageable.apply { age = 0 } },
            2,
        )
    }
}
