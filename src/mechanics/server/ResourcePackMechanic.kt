package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Command.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.mechanics.MechanicInterface
import java.net.URI
import kotlin.time.measureTime

/** Represents a mechanic that sends the IllyriaPlus resource pack to joining players. */
internal object ResourcePackMechanic : MechanicInterface {
    private const val PACK_URL =
        "https://github.com/XodiumSoftware/IllyriaPlus/releases/download/nightly_resourcepack/irp.zip"
    private const val REQUIRED = true

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("reloadresourcepack")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ ->
                        player.sendActionBar(
                            MM.deserialize("<green>Reloading IllyriaPlus resource pack for all online players..."),
                        )
                        instance.server.onlinePlayers.forEach {
                            it.clearResourcePacks()
                            it.sendResourcePacks(createRequest())
                        }
                    },
                "Reloads the IllyriaPlus resource pack for all online players",
                listOf("rrp"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.reloadresourcepack".lowercase(),
                "Allows reloading the IllyriaPlus resource pack",
                PermissionDefault.OP,
            ),
        )

    override fun register(): Long = super.register() + measureTime { createRequest() }.inWholeMilliseconds

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.NORMAL)
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val connection = event.connection as? Audience ?: return

        connection.sendResourcePacks(createRequest())
    }

    private fun createRequest(): ResourcePackRequest =
        ResourcePackRequest
            .resourcePackRequest()
            .packs(ResourcePackInfo.resourcePackInfo().uri(URI.create(PACK_URL)).computeHashAndBuild())
            .required(REQUIRED)
            .build()
}
