package org.xodium.illyriamannequins

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
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
}
