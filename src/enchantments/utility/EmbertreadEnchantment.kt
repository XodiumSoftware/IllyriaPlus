package org.xodium.illyriaplus.enchantments.utility

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.EnchantmentUtils.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling embertread enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object EmbertreadEnchantment : EnchantmentInterface {
    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(1)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(20, 0))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(60, 0))
            .activeSlots(EquipmentSlotGroup.FEET)

    @EventHandler
    fun on(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        if (!isValidBoots(player.inventory.boots)) return

        when (event.cause) {
            EntityDamageEvent.DamageCause.HOT_FLOOR -> event.isCancelled = true
            EntityDamageEvent.DamageCause.FIRE -> event.isCancelled = true
            else -> return
        }
    }

    /**
     * Checks if the item is foot armor with the Embertread enchantment.
     *
     * @param item The item to check.
     * @return `true` if the item is foot armor with Embertread, otherwise `false`.
     */
    private fun isValidBoots(item: ItemStack?): Boolean =
        item?.let { Tag.ITEMS_FOOT_ARMOR.isTagged(it.type) && it.containsEnchantment(get()) } ?: false
}
