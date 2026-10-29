package org.xodium.illyriamannequins

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/** Utility functions shared across mannequin behaviors. */
@Suppress("UnstableApiUsage")
internal object Utils {
    /**
     * Checks if a player is visible to a mannequin.
     *
     * @param mannequin The mannequin doing the tracking.
     * @return `true` if the player is visible to the mannequin.
     */
    fun Player.visibleTo(mannequin: Entity): Boolean =
        !isDead &&
            isOnline &&
            uniqueId != mannequin.uniqueId &&
            !isInvisible

    /**
     * Finds the nearest solid ground at or below this location.
     *
     * @return The [Location] on top of the highest motion-blocking block.
     */
    fun Location.groundLocation(): Location =
        world
            .getHighestBlockAt(this, HeightMap.MOTION_BLOCKING_NO_LEAVES)
            .location
            .add(0.5, 1.0, 0.5)

    /**
     * Computes a horizontal step vector from a source location toward a destination.
     *
     * @param from The source location.
     * @param speed The step magnitude in blocks per tick.
     * @return The horizontal movement step, or `null` if the locations overlap horizontally.
     */
    fun Location.stepToward(
        from: Location,
        speed: Double,
    ): Vector? {
        val step = toVector().subtract(from.toVector()).setY(0)
        if (step.lengthSquared() == 0.0) return null
        return step.normalize().multiply(speed)
    }

    /**
     * Computes the melee attack damage of a weapon from its default mainhand attributes.
     *
     * @return The total attack damage, at minimum 1.0 (fist damage).
     */
    fun ItemStack.meleeDamage(): Double {
        if (isEmpty) return FIST_DAMAGE
        val amount =
            type
                .asItemType()
                ?.getDefaultAttributeModifiers(EquipmentSlot.HAND)
                ?.get(Attribute.ATTACK_DAMAGE)
                ?.sumOf { it.amount } ?: 0.0
        return FIST_DAMAGE + amount
    }

    /** The unarmed melee damage dealt without a weapon. */
    private const val FIST_DAMAGE = 1.0
}
