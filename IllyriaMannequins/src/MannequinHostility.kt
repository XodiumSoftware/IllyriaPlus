package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.bukkit.entity.Monster
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance

/** Makes monsters detect and attack mannequins. */
@Suppress("UnstableApiUsage")
internal object MannequinHostility {
    /** The radius in blocks within which monsters detect mannequins. */
    private const val DETECTION_RADIUS = 16.0

    /** The interval in ticks at which monster targeting updates. */
    private const val TARGETING_INTERVAL_TICKS = 10L

    /** Registers the hostility task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinHostility::updateTargeting,
            TARGETING_INTERVAL_TICKS,
            TARGETING_INTERVAL_TICKS,
        )
    }

    /** Makes all monsters near a mannequin target it. */
    private fun updateTargeting() {
        instance.server.worlds.forEach { world ->
            world
                .entities
                .filterIsInstance<Mannequin>()
                .forEach { mannequin ->
                    mannequin
                        .getNearbyEntities(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)
                        .filterIsInstance<Monster>()
                        .filter { it.target == null }
                        .forEach { it.target = mannequin }
                }
        }
    }
}
