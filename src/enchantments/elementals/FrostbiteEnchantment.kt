package org.xodium.illyriaplus.enchantments.elementals

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling frostbite enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object FrostbiteEnchantment : EnchantmentInterface {
    private val FREEZE_TICKS = mapOf(1 to 60, 2 to 100)
    private val SLOWNESS_LEVEL = mapOf(1 to 1, 2 to 2)

    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(2)
            .maxLevel(2)
            .weight(2)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(20, 5))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(50, 10))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val targetPlayer = event.entity as? Player ?: return
        val weapon = attacker.inventory.itemInMainHand
        val level = weapon.getEnchantmentLevel(get())

        if (level <= 0) return

        targetPlayer.freezeTicks = FREEZE_TICKS[level] ?: return
        targetPlayer.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                (FREEZE_TICKS[level] ?: return) + 20,
                (SLOWNESS_LEVEL[level] ?: return) - 1,
                false,
                true,
                true,
            ),
        )
    }
}
