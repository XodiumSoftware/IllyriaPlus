package org.xodium.illyrialib

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import org.xodium.illyrialib.data.CommandData

/** Represents a contract for a command within the system. */
interface CommandInterface {
    /**
     * Builds this command's definition.
     *
     * Implementations typically return a [CommandData] bundling the command's
     * literal argument builder, description and aliases:
     * ```kotlin
     * override fun onRegister() = CommandData(
     *     Commands.literal("example").playerExecuted { _, _ -> },
     *     "Description.",
     * )
     * ```
     *
     * @return The [CommandData] describing this command.
     */
    fun onRegister(): CommandData

    /**
     * Registers this command with the server.
     *
     * @param plugin The plugin performing registration.
     */
    operator fun invoke(plugin: Plugin) {
        val (builder, description, aliases) = onRegister()
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(builder.build(), description, aliases)
        }
    }
}
