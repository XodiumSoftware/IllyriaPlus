package org.xodium.illyriabridge.bridges

import org.bukkit.event.Listener
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import kotlin.time.measureTime

/** Represents a contract for a bridge within the system. */
internal interface BridgeInterface : Listener {
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
