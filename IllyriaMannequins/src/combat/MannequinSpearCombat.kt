package org.xodium.illyriamannequins.combat

import org.bukkit.Tag
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriamannequins.Utils.meleeDamage
import org.xodium.illyriamannequins.Utils.stepToward

/** Handles spear combat for mannequins: charging lunges with extended reach. */
@Suppress("UnstableApiUsage")
internal object MannequinSpearCombat {
    /** The squared distance within which a mannequin performs a spear lunge. */
    const val ATTACK_RANGE_SQUARED = 16.0

    /** The interval in ticks between a mannequin's spear lunges. */
    private const val LUNGE_COOLDOWN_TICKS = 20

    /** The horizontal velocity magnitude of a spear lunge. */
    private const val LUNGE_VELOCITY = 0.9

    /** The upward velocity component of a spear lunge. */
    private const val LUNGE_LIFT = 0.25

    /**
     * Makes a mannequin with a spear lunge toward its target and deal damage on impact.
     *
     * @param mannequin The mannequin performing the lunge.
     * @param target The entity to lunge at.
     */
    fun lunge(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        val direction = target.location.stepToward(mannequin.location, LUNGE_VELOCITY) ?: return
        mannequin.swingMainHand()
        target.damage(
            mannequin.equipment.getItem(EquipmentSlot.HAND).meleeDamage(),
            mannequin,
        )
        mannequin.velocity = direction.apply { y = LUNGE_LIFT }
    }

    /**
     * Determines the attack cooldown in ticks for spear combat.
     *
     * @return The cooldown in ticks between spear lunges.
     */
    fun cooldownTicks(): Int = LUNGE_COOLDOWN_TICKS

    /**
     * Checks if a mannequin is holding a spear in its main hand.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if a spear is held.
     */
    fun hasSpear(mannequin: Mannequin): Boolean =
        Tag.ITEMS_SPEARS.isTagged(mannequin.equipment.getItem(EquipmentSlot.HAND).type)
}
