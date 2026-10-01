package org.xodium.illyriamannequins.combat

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.Utils.meleeDamage

/** Handles melee weapon combat for mannequins: swinging and weapon-based damage. */
@Suppress("UnstableApiUsage")
internal object MannequinSwordCombat {
    /** The delay in ticks before a mannequin raises its shield again after swinging. */
    private const val SHIELD_RAISE_DELAY_TICKS = 5L

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
        target.damage(mannequin.equipment.getItem(EquipmentSlot.HAND).meleeDamage(), mannequin)
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
}
