package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.bukkit.entity.Monster
import org.xodium.illyriamannequins.MannequinPDC.combatMode

/** Makes monsters detect and attack mannequins. */
@Suppress("UnstableApiUsage")
internal object MannequinHostility {
    /** The radius in blocks within which monsters detect mannequins. */
    private const val DETECTION_RADIUS = 16.0

    /**
     * Makes all monsters near a mannequin target it.
     *
     * Mannequins in [CombatMode.FLEEING] are skipped, so a horde leaves passive mannequins alone.
     *
     * @param mannequins All mannequins across all worlds.
     */
    fun tick(mannequins: Collection<Mannequin>) {
        mannequins
            .filterNot { it.combatMode == CombatMode.FLEEING }
            .forEach { mannequin ->
                mannequin
                    .getNearbyEntities(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)
                    .filterIsInstance<Monster>()
                    .filter { it.target == null }
                    .forEach { it.target = mannequin }
            }
    }
}
