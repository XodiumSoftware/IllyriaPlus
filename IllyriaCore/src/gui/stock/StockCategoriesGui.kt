package org.xodium.illyriacore.gui.stock

import org.bukkit.Material
import org.bukkit.entity.Player
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.gui.GuiInterface
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Builds and opens the wandering trader's category picker window: the shop's entry point. */
@OptIn(ExperimentalDslApi::class)
internal object StockCategoriesGui : GuiInterface {
    private const val TITLE = "<mango>Wandering Trader"
    private const val SELL_BUTTON_NAME = "<green><b>Sell items"

    override val openWindows: MutableSet<Window> = mutableSetOf()

    /**
     * Builds and opens the category picker for the given player. Clicking a category opens the
     * paged shop filtered to that category.
     *
     * @param player The player browsing the trader's stock.
     * @param handlers The trader's callbacks (items, stock of, purchase, deposit).
     */
    fun open(
        player: Player,
        handlers: StockHandlers,
    ) {
        val pickerWindow =
            window(player) {
                title by MM.deserialize(TITLE)
                upperGui by
                    gui(
                        "# # # # # # # # #",
                        "# B T F M R X . #",
                        "# # # # s # # # #",
                    ) {
                        '#' by border
                        '.' by border
                        'B' by categoryIcon(MaterialCategory.BUILDING_BLOCKS, player, handlers)
                        'T' by categoryIcon(MaterialCategory.TOOLS_AND_WEAPONS, player, handlers)
                        'F' by categoryIcon(MaterialCategory.FOOD_AND_FARMING, player, handlers)
                        'M' by categoryIcon(MaterialCategory.MATERIALS, player, handlers)
                        'R' by categoryIcon(MaterialCategory.REDSTONE_AND_UTILITY, player, handlers)
                        'X' by categoryIcon(MaterialCategory.MISC, player, handlers)
                        's' by
                            item {
                                itemProvider by ItemBuilder(Material.EMERALD).setName(SELL_BUTTON_NAME)
                                onClick {
                                    StockSellGui.open(player, handlers, StockBuyGui::refreshAll) {
                                        open(player, handlers)
                                    }
                                }
                            }
                    }
            }
        pickerWindow.addOpenHandler { openWindows += pickerWindow }
        pickerWindow.addCloseHandler { openWindows -= pickerWindow }
        pickerWindow.open()
    }

    /**
     * Builds a picker button for [category]: a static icon showing the category's display name.
     * Clicking opens the paged shop filtered to that category.
     */
    private fun categoryIcon(
        category: MaterialCategory,
        player: Player,
        handlers: StockHandlers,
    ): Item =
        item {
            itemProvider by
                ItemBuilder(category.icon)
                    .setName("<mango><b>${category.displayName}")
            onClick { StockBuyGui.open(player, category, handlers) }
        }
}
