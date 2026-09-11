package org.xodium.illyriakingdoms.mechanics

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.event.Listener
import org.bukkit.permissions.Permission
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.data.CommandData
import kotlin.time.measureTime

/** Represents a contract for a mechanic within the system. */
internal interface MechanicInterface : Listener {
    /**
     * Retrieves a list of command data associated with the mechanic.
     *
     * @return A [Collection] of [CommandData] objects representing the commands for the mechanic.
     */
    val cmds: Collection<CommandData> get() = emptyList()

    /**
     * Retrieves a list of permissions associated with this mechanic.
     *
     * @return A [List] of [Permission] objects representing the permissions for this mechanic.
     */
    val perms: List<Permission> get() = emptyList()

    /**
     * Registers this feature with the server.
     *
     * @return The time taken to register the feature in milliseconds.
     */
    @Suppress("UnstableApiUsage")
    fun register(): Long =
        measureTime {
            instance.server.pluginManager.addPermissions(perms)
            instance.server.pluginManager.registerEvents(this, instance)
            instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
                cmds.forEach { cmd ->
                    it.registrar().register(
                        cmd.builder.build(),
                        cmd.description,
                        cmd.aliases,
                    )
                }
            }
        }.inWholeMilliseconds

    /**
     * Called when the plugin is disabled. Override to flush pending state; the default
     * implementation does nothing.
     */
    fun onDisable() {}
}
