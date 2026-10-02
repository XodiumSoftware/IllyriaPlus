package org.xodium.illyrialib.interfaces

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
     * override fun invoke() = CommandData(
     *     Commands.literal("example").playerExecuted { _, _ -> },
     *     "Description.",
     * )
     * ```
     *
     * @return The [CommandData] describing this command.
     */
    operator fun invoke(): CommandData
}
