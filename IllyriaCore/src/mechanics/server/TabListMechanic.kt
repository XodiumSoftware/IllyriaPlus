package org.xodium.illyriacore.mechanics.server

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriacore.enums.GlyphEnum
import org.xodium.illyriacore.enums.SeasonStateEnum
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.MM

/** Represents a mechanic handling tab list updates within the system. */
internal object TabListMechanic : MechanicInterface {
    private val HEADER: List<String> =
        listOf(
            "<mango_r><st>───────────────</st></gradient> " +
                "<firewatch>" +
                "𝕴𝖑𝖑𝖞𝖗𝖎𝖆" +
                "</gradient> " +
                "<mango><st>───────────────</st></gradient>",
            "",
        )
    private val FOOTER: List<String> =
        listOf(
            "",
            "<mango>Season:</mango> <season_name> <season_icon>",
            "<mango_r><st>─────────────────</st></gradient><mango><st>─────────────────</st></gradient>",
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = tablist(event.player)

    @EventHandler
    fun on(event: PlayerClientLoadedWorldEvent) = updatePlayerListName(event)

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
     * Substitutes `<season_name>` and `<season_icon>` placeholders when the audience is a
     * [Player]; other audiences receive the unresolved template.
     *
     * @param audience The audience to update the tab list for.
     */
    fun tablist(audience: Audience) {
        val footer =
            if (audience is Player) {
                val season = SeasonStateEnum.currentSeason(audience.world)
                val name =
                    season.name.lowercase().replaceFirstChar(Char::uppercase)
                FOOTER
                    .joinToString("\n")
                    .replace("<season_name>", "<${season.color}>$name</${season.color}>")
                    .replace("<season_icon>", GlyphEnum.of(season).toString())
            } else {
                FOOTER.joinToString("\n")
            }
        audience.sendPlayerListHeaderAndFooter(
            MM.deserialize(HEADER.joinToString("\n")),
            MM.deserialize(footer),
        )
    }
}
