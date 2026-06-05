package org.xodium.illyriaplus.guis

import org.bukkit.entity.Player
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Represents a gui handling faq within the system. */
internal object FaqGui : GuiInterface {
    override fun gui(player: Player): Window {
        val categories = instance.mechanics.groupBy { it.faqTab }
        val tabConfigs = FaqTab.entries
        val tabs =
            tabConfigs.map { config ->
                createTab(
                    when (config) {
                        FaqTab.RECIPES -> instance.recipes.map { it.faqItem }
                        else -> categories[config]?.map { it.faqItem } ?: emptyList()
                    },
                )
            }
        val tabButtons =
            tabConfigs.mapIndexed { index, config ->
                Utils.Gui.createTabButton(index, ItemBuilder(config.material).setName(MM.deserialize(config.label)))
            }

        return Window
            .builder()
            .apply {
                setTitle(MM.deserialize("<firewatch><b>FAQ</b></gradient>"))
                setUpperGui(
                    TabGui
                        .builder()
                        .apply {
                            setStructure(
                                "# # P W E S R # #",
                                "# x x x x x x x #",
                                "# x x x x x x x #",
                                "# x x x x x x x #",
                                "# # # # # # # # #",
                            )
                            addIngredient('#', Utils.Gui.filler())
                            addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                            setTabs(tabs)
                            tabConfigs
                                .zip(tabButtons)
                                .forEach { (config, button) -> addIngredient(config.char, button) }
                        }.build(),
                )
                setViewer(player)
            }.build()
    }

    /**
     * Creates a tab [Gui] populated with the given items.
     *
     * Items fill a 3×7 grid left-to-right, top-to-bottom.
     *
     * @param items The list of items to display in this tab.
     * @return A [Gui] containing the items.
     */
    private fun createTab(items: List<Item>): Gui =
        Gui
            .builder()
            .setStructure(
                "x x x x x x x",
                "x x x x x x x",
                "x x x x x x x",
            ).build()
            .apply {
                items
                    .take(21)
                    .forEachIndexed { index, item -> setItem(index, item) }
            }
}
