package org.xodium.illyriacore.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData
import java.net.URI

/** Represents a mechanic that sends the IllyriaCore resource pack to joining players. */
internal object ResourcePackMechanic : MechanicInterface {
    private const val PACK_URL =
        "https://github.com/XodiumSoftware/IllyriaPlus/releases/download/nightly/pack.zip"

    @Volatile
    private var request: ResourcePackRequest = buildRequest()

    private fun buildRequest(): ResourcePackRequest =
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

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("reloadresourcepack")
                    .requiresPlayer { isOp }
                    .playerExecuted { player, _ ->
                        player.sendActionBar(
                            MM.deserialize("<green>Reloading IllyriaCore resource pack for all online players..."),
                        )
                        instance.server.asyncScheduler.runNow(instance) {
                            val freshRequest = buildRequest()
                            instance.server.globalRegionScheduler.run(instance) { _ ->
                                request = freshRequest
                                instance.server.onlinePlayers.forEach {
                                    it.clearResourcePacks()
                                    it.sendResourcePacks(freshRequest)
                                }
                            }
                        }
                    },
                "Reloads the IllyriaCore resource pack for all online players",
                listOf("rrp"),
            ),
        )

    override fun register(): Long = super.register().also { request }

    @EventHandler(priority = EventPriority.NORMAL)
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val connection = event.connection as? Audience ?: return

        connection.sendResourcePacks(request)
    }
}
