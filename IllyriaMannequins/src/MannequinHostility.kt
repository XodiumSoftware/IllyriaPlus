package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.bukkit.entity.Monster

/** Makes monsters detect and attack mannequins. */
@Suppress("UnstableApiUsage")
internal object MannequinHostility {
    /** The radius in blocks within which monsters detect mannequins. */
    private const val DETECTION_RADIUS = 16.0

    /**
     * Makes all monsters near a mannequin target it.
     *
     * @param mannequins All mannequins across all worlds.
     */
    fun tick(mannequins: Collection<Mannequin>) {
        mannequins.forEach { mannequin ->
            mannequin
                .getNearbyEntities(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)
                .filterIsInstance<Monster>()
                .filter { it.target == null }
                .forEach { it.target = mannequin }
        }
    }
}
