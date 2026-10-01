package org.xodium.illyriaseasons

import org.bukkit.scheduler.BukkitTask

/** Advances the seasonal cycle based on Minecraft's day/night rhythm. */
internal object SeasonAdvancementMechanic {
    /** One Minecraft day = 24,000 ticks (20 minutes). */
    private const val TICKS_PER_DAY = 24000L

    private var task: BukkitTask? = null
    private var lastDay: Long = -1L

    /** Starts the seasonal advancement task. */
    fun start() {
        task =
            IllyriaSeasons.instance.server.scheduler.runTaskTimer(
                IllyriaSeasons.instance,
                Runnable {
                    val overworld =
                        IllyriaSeasons
                            .instance
                            .server
                            .worlds
                            .firstOrNull() ?: return@Runnable
                    val currentDay = overworld.fullTime / TICKS_PER_DAY
                    if (currentDay > lastDay && lastDay > 0) {
                        SeasonSystem.advance()
                    }
                    lastDay = currentDay
                },
                0L,
                1L,
            )
    }

    /** Stops the advancement task. */
    fun stop() {
        task?.cancel()
        task = null
    }
}
