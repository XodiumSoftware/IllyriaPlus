package org.xodium.illyriaplus.mechanics.server

import org.bukkit.Material
import org.bukkit.ServerLinks
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.net.URI
import kotlin.time.measureTime

/** Represents a mechanic handling server info display within the system. */
@Suppress("UnstableApiUsage")
internal object ServerInfoMechanic : MechanicInterface {
    private val SERVER_LINKS: Map<ServerLinks.Type, String> =
        mapOf(
            ServerLinks.Type.WEBSITE to "https://xodium.org/",
            ServerLinks.Type.REPORT_BUG to "https://discord.gg/jusYH9aYUh",
            ServerLinks.Type.STATUS to "https://modrinth.com/server/illyria",
            ServerLinks.Type.COMMUNITY to "https://discord.gg/jusYH9aYUh",
            ServerLinks.Type.COMMUNITY_GUIDELINES to "https://vanillaplus.xodium.org/",
        )

    override val faqTab = FaqTab.SERVER_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.MAP)
                .setName(MM.deserialize("<mango>Server Info</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Links</yellow> <firewatch>></gradient> <white>Website, Discord, Modrinth</white>",
                    ),
                ),
        )

    override fun register(): Long = super.register() + measureTime { serverLinks() }.inWholeMilliseconds

    /** Configures server links based on the module's configuration. */
    private fun serverLinks() =
        SERVER_LINKS.forEach { (type, url) ->
            instance.server.serverLinks.setLink(type, URI.create(url))
        }
}
