package org.xodium.illyriaplus.guis

import org.bukkit.entity.Player
import xyz.xenondevs.invui.window.Window

/** Represents a contract for guis within the system. */
internal interface GuiInterface {
    /**
     * Builds the GUI for the specified player with an optional selection callback.
     *
     * @param player The player to show the GUI to.
     * @return The [xyz.xenondevs.invui.window.Window] instance that was built.
     */
    fun gui(player: Player): Window

    /**
     * Builds and immediately opens the GUI for the specified player with a selection callback.
     *
     * @param player The player to open the GUI for.
     */
    fun open(player: Player) {
        gui(player).open()
    }
}
