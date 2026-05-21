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
            "<mango_r><st>───────────────</st></gradient> " +
                "<firewatch>" +
                "𝕴𝖑𝖑𝖞𝖗𝖎𝖆" +
                "</gradient> " +
                "<mango><st>───────────────</st></gradient>",
            "",
        )
    val FOOTER: List<String> =
        listOf(
            "",
            "<mango_r><st>─────────────────</st></gradient><mango><st>─────────────────</st></gradient>",
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        updateTablist(event)
    }

    @EventHandler
    fun on(event: PlayerClientLoadedWorldEvent) {
        updatePlayerListName(event)
    }

    /**
     * Updates the tab list for the player on join.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun updateTablist(event: PlayerJoinEvent) {
        tablist(event.player)
    }

    /**
     * Updates the player's list name when their client finishes loading the world.
     *
     * @param event The PlayerClientLoadedWorldEvent triggered by the player.
     */
    private fun updatePlayerListName(event: PlayerClientLoadedWorldEvent) {
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
