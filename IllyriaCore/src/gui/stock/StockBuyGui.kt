package org.xodium.illyriacore.gui.stock

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.data.WanderingTraderItemData
import org.xodium.illyriacore.gui.GuiInterface
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.pagedItemsGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Builds and opens the paged shop window for a single [MaterialCategory]. */
@OptIn(ExperimentalDslApi::class)
internal object StockBuyGui : GuiInterface {
    private const val TITLE = "<mango>Wandering Trader"
    private const val PREVIOUS_PAGE_NAME = "<gray>Previous page"
    private const val NEXT_PAGE_NAME = "<gray>Next page"
    private const val BACK_NAME = "<gray>Back to categories"
    private const val SELL_BUTTON_NAME = "<green><b>Sell items"
    private const val BULK_HINT = "<dark_gray>LMB: 1x | MMB: 10x | RMB: 100x"

    /** Open shop windows and their content rebuild callbacks; refreshed together on stock and price changes. */
    private val openRebuilds = mutableMapOf<Window, () -> Unit>()

    override val openWindows: MutableSet<Window> = mutableSetOf()

    private val previous =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page > 0) {
                    ItemBuilder(Material.ARROW).setName(PREVIOUS_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page-- }

    private val next =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page < gui.pageCount - 1) {
                    ItemBuilder(Material.ARROW).setName(NEXT_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page++ }

    /**
     * Builds and opens the shop window for [category] for the given player. The window is tracked
     * until closed, and its content is refreshed on every stock or price change.
     *
     * @param player The player viewing the shop.
     * @param category The category to filter displayed trades by.
     * @param handlers The trader's callbacks (items, stock of, purchase, deposit).
     */
    fun open(
        player: Player,
        category: MaterialCategory,
        handlers: StockHandlers,
    ) {
        buildWindow(player, category, handlers).open()
    }

    /** Rebuilds the content of every open shop window so all viewers see current stock and prices. */
    fun refreshAll() = openRebuilds.values.toList().forEach { it() }

    /**
     * Builds the shop window for [category], including paged content and navigation, and registers
     * it on open for live refreshes until it is closed.
     */
    private fun buildWindow(
        player: Player,
        category: MaterialCategory,
        handlers: StockHandlers,
    ): Window {
        lateinit var rebuild: () -> Unit
        val contentProvider = mutableProvider(emptyList<Item>())
        rebuild = {
            contentProvider.set(
                handlers
                    .items()
                    .filter { MaterialCategory.of(it.result.type) == category }
                    .map { it.toGuiItem(player, handlers) },
            )
        }
        rebuild()

        val shopWindow =
            window(player) {
                title by MM.deserialize("$TITLE <dark_gray>—</dark_gray> <mango>${category.displayName}")
                upperGui by
                    pagedItemsGui(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# < # b s # > # #",
                    ) {
                        '#' by border
                        'x' by Markers.CONTENT_LIST_SLOT_HORIZONTAL
                        '<' by previous
                        '>' by next
                        'b' by
                            item {
                                itemProvider by ItemBuilder(Material.ARROW).setName(BACK_NAME)
                                onClick { StockCategoriesGui.open(player, handlers) }
                            }
                        's' by
                            item {
                                itemProvider by ItemBuilder(Material.EMERALD).setName(SELL_BUTTON_NAME)
                                onClick {
                                    StockSellGui.open(player, handlers, ::refreshAll) {
                                        buildWindow(player, category, handlers).open()
                                    }
                                }
                            }
                        content by contentProvider
                    }
            }
        shopWindow.addOpenHandler {
            openWindows += shopWindow
            openRebuilds[shopWindow] = rebuild
        }
        shopWindow.addCloseHandler {
            openWindows -= shopWindow
            openRebuilds.remove(shopWindow)
        }
        return shopWindow
    }

    /**
     * Builds the button displaying this trade entry, describing price and current stock in its
     * lore. Left/middle/right clicks buy 1/10/100 units; every open shop window is refreshed
     * afterwards.
     */
    private fun WanderingTraderItemData.toGuiItem(
        player: Player,
        handlers: StockHandlers,
    ): Item =
        item {
            itemProvider by provider { ItemBuilder(icon(handlers.stockOf)) }
            onClick {
                val units =
                    when (clickType) {
                        ClickType.LEFT -> 1
                        ClickType.MIDDLE -> 10
                        ClickType.RIGHT -> 100
                        else -> return@onClick
                    }
                handlers.purchase(player, this@toGuiItem, units)
                refreshAll()
            }
        }

    /**
     * Builds the display icon for this trade entry, with lore lines describing the price
     * and the current stock.
     */
    private fun WanderingTraderItemData.icon(stockOf: (WanderingTraderItemData) -> Int): ItemStack =
        result.clone().apply {
            editMeta { meta ->
                val stock = stockOf(this@icon)
                val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                lore.add(
                    MM
                        .deserialize(
                            "<gray>Price: </gray><mango>${price.amount}x</gradient> " +
                                "<sprite:items:item/${price.type.key.key}>",
                        ).decoration(TextDecoration.ITALIC, false),
                )
                lore.add(
                    MM
                        .deserialize("<gray>In stock: </gray><skyline>$stock</gradient>")
                        .decoration(TextDecoration.ITALIC, false),
                )
                lore.add(MM.deserialize(BULK_HINT).decoration(TextDecoration.ITALIC, false))
                meta.lore(lore)
            }
        }
}
