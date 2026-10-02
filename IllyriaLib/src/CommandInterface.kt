package org.xodium.illyrialib

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import org.xodium.illyrialib.data.CommandData
import kotlin.time.measureTime

/** Represents a contract for a command within the system. */
interface CommandInterface {
    /**
     * Builds this command's definition bundle.
     *
     * @return A [CommandData] containing the builder, description and aliases.
     */
    fun build(): CommandData

    /**
     * Registers this command with the server via Paper's lifecycle API.
     *
     * @param plugin The plugin performing registration.
     * @return The time taken to register the command in milliseconds.
     */
    @Suppress("UnstableApiUsage")
    fun register(plugin: Plugin): Long =
        measureTime {
            val (builder, description, aliases) = build()
            plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
                it.registrar().register(builder.build(), description, aliases)
            }
        }.inWholeMilliseconds
}
