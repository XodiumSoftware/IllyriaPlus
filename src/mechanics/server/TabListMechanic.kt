@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.weather.ThunderChangeEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
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
            "<gradient:#FFA751:#FFE259><st>─────────────</st></gradient>  " +
                "<gradient:#CB2D3E:#EF473A>Weather:</gradient> <weather> " +
                " <gradient:#FFE259:#FFA751><st>─────────────</st></gradient>",
        )
    const val WEATHER_THUNDERING: String = "<red>🌩<reset>"
    const val WEATHER_STORM: String = "<yellow>🌦<reset>"
    const val WEATHER_CLEAR: String = "<green>🌤<reset>"

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        tablist(event.player)
    }

    @EventHandler
    fun on(event: PlayerClientLoadedWorldEvent) {
        event.player.playerListName(event.player.displayName())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun on(event: WeatherChangeEvent) = event.world.players.forEach { tablist(it) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun on(event: ThunderChangeEvent) = event.world.players.forEach { tablist(it) }

    /**
     * Updates the tab list header and footer for the given audience.
     *
     * @param audience The audience to update the tab list for.
     */
    fun tablist(audience: Audience) {
        audience.sendPlayerListHeaderAndFooter(
            MM.deserialize(HEADER.joinToString("\n")),
            MM.deserialize(
                FOOTER.joinToString("\n"),
                Placeholder.component(
                    "weather",
                    MM.deserialize(
                        instance.server.worlds[0].weather(
                            WEATHER_THUNDERING,
                            WEATHER_STORM,
                            WEATHER_CLEAR,
                        ),
                    ),
                ),
            ),
        )
    }

    /**
     * Gets a string representation of the world's weather.
     *
     * @param thundering Value for thunder.
     * @param storm Value for storm.
     * @param clear Value for clear weather.
     * @return The matching weather string.
     */
    private fun World.weather(
        thundering: String,
        storm: String,
        clear: String,
    ): String =
        when {
            isThundering -> thundering
            hasStorm() -> storm
            else -> clear
        }
}
