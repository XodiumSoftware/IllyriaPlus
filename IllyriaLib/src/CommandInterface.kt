package org.xodium.illyrialib

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin

/** Represents a contract for a command within the system. */
interface CommandInterface {
    /**
     * Builds and submits this command against the active [Commands] registrar.
     *
     * Implementations typically write the full registration inline, e.g.:
     * ```kotlin
     * override fun ReloadableRegistrarEvent<Commands>.onRegister() {
     *     registrar().register(
     *         Commands.literal("example").playerExecuted { _, _ -> }.build(),
     *         "Description.",
     *     )
     * }
     * ```
     */
    fun ReloadableRegistrarEvent<Commands>.onRegister()

    /**
     * Registers this command with the server.
     *
     * @param plugin The plugin performing registration.
     */
    operator fun invoke(plugin: Plugin) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.onRegister()
        }
    }
}
