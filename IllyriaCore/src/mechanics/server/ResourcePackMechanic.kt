package org.xodium.illyriacore.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Command.playerExecuted
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.data.CommandData
import org.xodium.illyriacore.mechanics.MechanicInterface
import java.net.URI

/** Represents a mechanic that sends the IllyriaCore resource pack to joining players. */
internal object ResourcePackMechanic : MechanicInterface {
    private const val PACK_URL =
        "https://github.com/XodiumSoftware/IllyriaCore/releases/download/nightly_resourcepack/irp.zip"

    private val request: ResourcePackRequest by lazy {
        ResourcePackRequest
            .resourcePackRequest()
            .packs(
                ResourcePackInfo
                    .resourcePackInfo()
                    .uri(URI.create(PACK_URL))
                    .computeHashAndBuild()
                    .join(),
            ).required(true)
            .build()
    }

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("reloadresourcepack")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ ->
                        player.sendActionBar(
                            MM.deserialize("<green>Reloading IllyriaCore resource pack for all online players..."),
                        )
                        instance.server.onlinePlayers.forEach {
                            it.clearResourcePacks()
                            it.sendResourcePacks(request)
                        }
                    },
                "Reloads the IllyriaCore resource pack for all online players",
                listOf("rrp"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.reloadresourcepack".lowercase(),
                "Allows reloading the IllyriaCore resource pack",
                PermissionDefault.OP,
            ),
        )

    override fun register(): Long = super.register().also { request }

    @EventHandler(priority = EventPriority.NORMAL)
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val connection = event.connection as? Audience ?: return

        connection.sendResourcePacks(request)
    }
}
