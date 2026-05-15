@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent
import net.kyori.adventure.audience.Audience
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.interfaces.MechanicInterface

/** Represents a mechanic handling tab list updates within the system. */
internal object TabListMechanic : MechanicInterface {
    val HEADER: List<String> =
        listOf(
            "<gradient:#FFA751:#FFE259><st>───────────────</st></gradient> " +
                "<gradient:#CB2D3E:#EF473A>" +
                "𝕴𝖑𝖑𝖞𝖗𝖎𝖆" +
                "</gradient> " +
                "<gradient:#FFE259:#FFA751><st>───────────────</st></gradient>",
            "",
        )
    val FOOTER: List<String> =
        listOf(
            "",
            "<gradient:#FFA751:#FFE259><st>─────────────────</st></gradient>" +
                "<gradient:#FFE259:#FFA751><st>─────────────────</st></gradient>",
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        tablist(event.player)
    }

    @EventHandler
    fun on(event: PlayerClientLoadedWorldEvent) {
        event.player.playerListName(event.player.displayName())
    }

    /**
     * Updates the tab list header and footer for the given audience.
     *
     * @param audience The audience to update the tab list for.
     */
    fun tablist(audience: Audience) {
        audience.sendPlayerListHeaderAndFooter(
            MM.deserialize(HEADER.joinToString("\n")),
            MM.deserialize(FOOTER.joinToString("\n")),
        )
    }
}
