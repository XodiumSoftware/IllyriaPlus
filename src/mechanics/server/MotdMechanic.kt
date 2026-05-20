package org.xodium.illyriaplus.mechanics.server

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerListPingEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.interfaces.MechanicInterface

/** Represents a mechanic handling server MOTD within the system. */
internal object MotdMechanic : MechanicInterface {
    private val MOTD: List<String> =
        listOf(
            "<firewatch><b>Ultimate Private SMP</b></gradient>",
            "<mango><b>➤ WELCOME BACK LADS!</b></gradient>",
        )

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: ServerListPingEvent) {
        event.motd(Utils.MM.deserialize(MOTD.joinToString("\n")))
    }
}
