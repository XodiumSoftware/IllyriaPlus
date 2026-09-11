package org.xodium.illyriacore.mechanics.server

import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.mechanics.MechanicInterface
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
            measureTime { instance.server.motd((MM.deserialize(MOTD.joinToString("\n")))) }.inWholeMilliseconds
}
