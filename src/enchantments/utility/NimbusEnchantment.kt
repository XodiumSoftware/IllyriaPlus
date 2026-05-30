package org.xodium.illyriaplus.enchantments.utility

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.attribute.Attribute
import org.bukkit.entity.HappyGhast
import org.bukkit.event.EventHandler
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.EnchantmentUtils.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling nimbus enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object NimbusEnchantment : EnchantmentInterface {
    private const val DEFAULT_FLY_SPEED = 0.05

    private val SPEED_MODIFIER = mapOf(1 to 1.5, 2 to 2.0, 3 to 2.5, 4 to 3.0, 5 to 3.5)

    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(5)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(25, 5))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(75, 10))
            .activeSlots(EquipmentSlotGroup.SADDLE)

    @EventHandler
    fun on(event: EntityEquipmentChangedEvent) {
        val entity = event.entity as? HappyGhast ?: return
        val harness = entity.equipment.getItem(EquipmentSlot.BODY)
        val attribute = entity.getAttribute(Attribute.FLYING_SPEED)

        if (harness.containsEnchantment(get())) {
            attribute?.baseValue = DEFAULT_FLY_SPEED * (SPEED_MODIFIER[harness.getEnchantmentLevel(get())] ?: return)
        } else {
            attribute?.baseValue = DEFAULT_FLY_SPEED
        }
    }
}
