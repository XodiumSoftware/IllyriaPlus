package org.xodium.illyriamannequins.combat

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance

/** Handles melee weapon combat for mannequins: swinging and weapon-based damage. */
@Suppress("UnstableApiUsage")
internal object MannequinSwordCombat {
    /** The delay in ticks before a mannequin raises its shield again after swinging. */
    private const val SHIELD_RAISE_DELAY_TICKS = 5L

    /** The unarmed melee damage dealt by a mannequin. */
    private const val FIST_DAMAGE = 1.0

    /**
     * Makes a mannequin swing its arm and deal melee damage to its target.
     * Lowers the shield for the swing and raises it again after a short delay.
     *
     * @param mannequin The mannequin performing the attack.
     * @param target The entity to damage.
     */
    fun dealDamage(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        val shieldUp = MannequinCoreCombat.hasShield(mannequin) && mannequin.isHandRaised
        if (shieldUp) {
            mannequin.clearActiveItem()
        }
        mannequin.swingMainHand()
        target.damage(weaponDamage(mannequin.equipment.getItem(EquipmentSlot.HAND)), mannequin)
        if (shieldUp) {
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    if (mannequin.isValid && MannequinCoreCombat.hasShield(mannequin)) {
                        mannequin.startUsingItem(EquipmentSlot.OFF_HAND)
                    }
                },
                SHIELD_RAISE_DELAY_TICKS,
            )
        }
    }

    /**
     * Computes the attack damage of an item stack from its default mainhand attributes.
     *
     * @param weapon The held item.
     * @return The total attack damage, at minimum [FIST_DAMAGE].
     */
    private fun weaponDamage(weapon: ItemStack): Double {
        if (weapon.isEmpty) return FIST_DAMAGE
        val amount =
            weapon
                .type
                .asItemType()
                ?.getDefaultAttributeModifiers(EquipmentSlot.HAND)
                ?.get(Attribute.ATTACK_DAMAGE)
                ?.sumOf { it.amount } ?: 0.0
        return FIST_DAMAGE + amount
    }
}
