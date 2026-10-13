package org.xodium.illyriamannequins

import org.bukkit.entity.Mannequin
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.combat.MannequinCoreCombat

/** Collects all mannequins once per tick and dispatches them to each mannequin subsystem. */
@Suppress("UnstableApiUsage")
internal object MannequinTicker {
    /** The interval in ticks of the fastest mannequin subsystem. */
    private const val BASE_INTERVAL_TICKS = 2L

    /** How many base ticks make up one [MannequinHostility] tick (10 ticks). */
    private const val HOSTILITY_EVERY = 5

    /** How many base ticks make up one threat-scan tick for [MannequinCoreCombat] (20 ticks). */
    private const val THREAT_SCAN_EVERY = 10

    /** The base tick counter, incremented once per [BASE_INTERVAL_TICKS]. */
    private var tick = 0

    /** Registers the shared tick task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinTicker::update,
            BASE_INTERVAL_TICKS,
            BASE_INTERVAL_TICKS,
        )
    }

    /** Collects all mannequins once and dispatches them to every ticking subsystem. */
    private fun update() {
        val mannequins =
            instance.server.worlds.flatMap { world ->
                world.entities.filterIsInstance<Mannequin>()
            }
        MannequinFollowing.tick(mannequins)
        MannequinHeadTracking.tick(mannequins)
        if (tick % HOSTILITY_EVERY == 0) MannequinHostility.tick(mannequins)
        if (tick % THREAT_SCAN_EVERY == 0) MannequinCoreCombat.scanThreats(mannequins)
        tick++
    }
}
