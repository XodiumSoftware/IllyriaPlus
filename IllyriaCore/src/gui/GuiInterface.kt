package org.xodium.illyriacore.gui

import org.bukkit.Material
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Represents a contract for an InvUI-based GUI window within the system. */
internal interface GuiInterface {
    /** Windows currently open for this GUI, closed on [closeAll]. */
    val openWindows: MutableSet<Window>

    /** A reusable black stained glass pane used as a border/filler across GUIs. */
    val border: Item
        get() = Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true))

    /**
     * Closes every currently tracked open window for this GUI, isolating each close so a failing
     * window cannot skip the rest. Called when the plugin is disabled; any pending state should
     * land in the underlying storage before returning. Implementations that open secondary
     * windows (e.g., a sell window from a shop window) are responsible for tracking and closing
     * those as well.
     */
    fun closeAll() {
        openWindows.toList().forEach { window ->
            runCatching { window.close() }
                .onFailure { instance.logger.warning("Failed to close a GUI window: ${it.message}") }
        }
        openWindows.clear()
    }
}
