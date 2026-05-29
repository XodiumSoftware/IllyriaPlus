package org.xodium.illyriaplus.mechanics.server

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerListPingEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling server MOTD within the system. */
internal object MotdMechanic : MechanicInterface {
    private val MOTD: List<String> =
        listOf(
            "<firewatch><b>Ultimate Private SMP</b></gradient>",
            "<mango><b>➤ WELCOME BACK LADS!</b></gradient>",
        )

    override val faqCategory = FaqCategory.ADMIN

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.OAK_SIGN)
                .setName(MM.deserialize("<mango>MOTD</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Server List</yellow> <firewatch>></gradient> <white>Custom ping message</white>",
                    ),
                ),
        )

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: ServerListPingEvent) {
        event.motd(MM.deserialize(MOTD.joinToString("\n")))
    }
}
