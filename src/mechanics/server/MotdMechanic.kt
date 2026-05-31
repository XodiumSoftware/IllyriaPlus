package org.xodium.illyriaplus.mechanics.server

import org.bukkit.Material
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.time.measureTime

/** Represents a mechanic handling server MOTD within the system. */
internal object MotdMechanic : MechanicInterface {
    private val MOTD: List<String> =
        listOf(
            "<firewatch><b>Ultimate Private SMP</b></gradient>",
            "<mango><b>➤ WELCOME BACK LADS!</b></gradient>",
        )

    override val faqTab = FaqTab.SERVER_MECHANIC

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

    override fun register(): Long =
        super.register() +
            measureTime {
                IllyriaPlus.instance.server.motd((MM.deserialize(MOTD.joinToString("\n"))))
            }.inWholeMilliseconds
}
