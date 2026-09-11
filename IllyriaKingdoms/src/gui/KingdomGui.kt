package org.xodium.illyriakingdoms.gui

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.xodium.illyriakingdoms.Utils.MM
import org.xodium.illyriakingdoms.data.KingdomData
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Builds and opens the kingdom overview GUI. */
@OptIn(ExperimentalDslApi::class)
internal object KingdomGui {
    private const val TITLE = "<gold>Kingdom Overview"
    private const val NO_KINGDOM_MSG = "<red>You are not in a kingdom."

    private val BORDER = Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true))

    private val openWindows = mutableSetOf<Window>()

    /**
     * Opens the kingdom overview GUI for the given player.
     *
     * @param player The player viewing the GUI.
     */
    fun open(player: Player) {
        val kingdom = KingdomData.getKingdom(player.uniqueId)
        if (kingdom == null) {
            player.sendActionBar(MM.deserialize(NO_KINGDOM_MSG))
            return
        }
        buildWindow(player, kingdom).open()
    }

    /** Closes every open kingdom window. */
    fun closeAll() {
        openWindows.toList().forEach { runCatching { it.close() } }
    }

    private fun buildWindow(
        player: Player,
        kingdom: KingdomData,
    ): Window {
        val window =
            window(player) {
                title by MM.deserialize(TITLE)
                upperGui by
                    gui(
                        "# # # # # # # # #",
                        "# . N . . . . . #",
                        "# . . . . . . . #",
                        "# # # # # # # # #",
                    ) {
                        '#' by BORDER
                        'N' by
                            item {
                                itemProvider by
                                    ItemBuilder(Material.BEACON)
                                        .setName(MM.deserialize("<gold>Name"))
                                        .addLoreLines(
                                            kingdom.name
                                                .color(null)
                                                .decoration(TextDecoration.ITALIC, false),
                                        )
                            }
                    }
            }
        window.addOpenHandler { openWindows += window }
        window.addCloseHandler { openWindows -= window }
        return window
    }
}
