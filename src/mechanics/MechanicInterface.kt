package org.xodium.illyriaplus.mechanics

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.event.Listener
import org.bukkit.permissions.Permission
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqTab
import xyz.xenondevs.invui.item.Item
import kotlin.time.measureTime

/** Represents a contract for a mechanic within the system. */
internal interface MechanicInterface : Listener {
    /**
     * Retrieves a list of command data associated with the mechanic.
     *
     * @return A [Collection] of [org.xodium.illyriaplus.data.CommandData] objects representing the commands for the mechanic.
     */
    val cmds: Collection<CommandData> get() = emptyList()

    /**
     * Retrieves a list of permissions associated with this mechanic.
     *
     * @return A [List] of [org.bukkit.permissions.Permission] objects representing the permissions for this mechanic.
     */
    val perms: List<Permission> get() = emptyList()

    /**
     * Retrieves the FAQ tab.
     *
     * @return A [org.xodium.illyriaplus.data.FaqTab] instance.
     */
    val faqTab: FaqTab

    /**
     * Retrieves the FAQ display item.
     *
     * @return An [xyz.xenondevs.invui.item.Item] instance.
     */
    val faqItem: Item

    /**
     * Registers this feature with the server.
     *
     * @return The time taken to register the feature in milliseconds.
     */
    @Suppress("UnstableApiUsage")
    fun register(): Long =
        measureTime {
            IllyriaPlus.instance.server.pluginManager
                .addPermissions(perms)
            IllyriaPlus.instance.server.pluginManager
                .registerEvents(this, IllyriaPlus.instance)
            IllyriaPlus.instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
                cmds.forEach { cmd ->
                    it.registrar().register(
                        cmd.builder.build(),
                        cmd.description,
                        cmd.aliases,
                    )
                }
            }
        }.inWholeMilliseconds
}
