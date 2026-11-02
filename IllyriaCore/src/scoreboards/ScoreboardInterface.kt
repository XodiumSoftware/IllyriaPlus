package org.xodium.illyriacore.scoreboards

import org.bukkit.event.Listener
import org.bukkit.scoreboard.Scoreboard
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import kotlin.time.measureTime

/** Represents a contract for custom scoreboards within the system. */
internal interface ScoreboardInterface : Listener {
    /** The underlying [Scoreboard] instance managed by this feature. */
    val scoreboard: Scoreboard

    /**
     * Registers this feature with the server.
     *
     * @return The time taken to register the feature in milliseconds.
     */
    fun register(): Long =
        measureTime {
            instance.server.pluginManager.registerEvents(this, instance)
        }.inWholeMilliseconds
}
