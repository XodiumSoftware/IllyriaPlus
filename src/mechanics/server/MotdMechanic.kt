package org.xodium.illyriaplus.mechanics.server

import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.time.measureTime

/** Represents a mechanic handling server MOTD within the system. */
internal object MotdMechanic : MechanicInterface {
    private val MOTD: List<String> =
        listOf(
            "<firewatch><b>Ultimate Private SMP</b></gradient>",
            "<mango><b>➤ WELCOME BACK LADS!</b></gradient>",
        )

    override fun register(): Long =
        super.register() +
            measureTime {
                IllyriaPlus.instance.server.motd((MM.deserialize(MOTD.joinToString("\n"))))
            }.inWholeMilliseconds
}
