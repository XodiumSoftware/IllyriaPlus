package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.GameRules
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Bat
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.random.Random

/** Represents a mechanic handling bat drops within the system. */
internal object BatMechanic : MechanicInterface {
    private const val BAT_MEMBRANE_DROP_CHANCE: Double = 1.0
    private const val BAT_MEMBRANE_BASE_MIN: Int = 0
    private const val BAT_MEMBRANE_BASE_MAX: Int = 1
    private const val BAT_MEMBRANE_LOOTING_BONUS: Int = 1

    @EventHandler
    fun on(event: EntityDeathEvent) = batDrop(event)

    /**
     * Handles bat death drops.
     *
     * @param event The EntityDeathEvent triggered when an entity dies.
     */
    private fun batDrop(event: EntityDeathEvent) {
        if (event.entity.world.getGameRuleValue(GameRules.SPAWN_PHANTOMS)) return
        if (event.entity !is Bat) return

        val killer = event.entity.killer ?: return

        if (Random.nextDouble() > BAT_MEMBRANE_DROP_CHANCE) return

        val lootingLevel = killer.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.LOOTING)
        val minAmount = BAT_MEMBRANE_BASE_MIN
        val maxAmount = BAT_MEMBRANE_BASE_MAX + (lootingLevel * BAT_MEMBRANE_LOOTING_BONUS)
        val amount = Random.nextInt(minAmount, maxAmount + 1)

        if (amount > 0) event.drops.add(ItemStack.of(Material.PHANTOM_MEMBRANE, amount))
    }
}
