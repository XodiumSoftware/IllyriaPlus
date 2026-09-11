package org.xodium.illyriacore.gui

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.data.WanderingTraderItemData
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.pagedItemsGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Builds and opens the wandering trader's custom shop GUI. */
@OptIn(ExperimentalDslApi::class)
internal object WanderingTraderGui {
    private const val TITLE = "<mango>Wandering Trader"
    private const val SELL_TITLE = "<mango>Sell to the Trader"
    private const val PREVIOUS_PAGE_NAME = "<gray>Previous page"
    private const val NEXT_PAGE_NAME = "<gray>Next page"
    private const val SELL_BUTTON_NAME = "<green><b>Sell items"
    private const val BULK_HINT = "<dark_gray>LMB: 1x | MMB: 10x | RMB: 100x"
    private const val DEPOSIT_SIZE = 54

    private val BORDER = Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true))

    private val back =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page > 0) {
                    ItemBuilder(Material.ARROW).setName(PREVIOUS_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page-- }

    private val forward =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page < gui.pageCount - 1) {
                    ItemBuilder(Material.ARROW).setName(NEXT_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page++ }

    /** Open shop windows and their content rebuild callbacks; refreshed together on stock and price changes. */
    private val openShops = mutableMapOf<Window, () -> Unit>()

    /** Open sell windows, closed alongside shops so deposited items are still processed. */
    private val openSells = mutableSetOf<Window>()

    /**
     * Builds and opens the wandering trader shop window for the given player. The window is
     * tracked until closed, and its content is refreshed on every stock or price change.
     *
     * @param player The player viewing the shop.
     * @param items Supplies the currently stocked trade entries, re-evaluated on every refresh.
     * @param stockOf Returns the current stock (individual items) of an entry.
     * @param onPurchase Called when an entry is clicked, receiving the clicking player and the entry.
     * @param onDeposit Called with the contents of the sell window when it closes,
     * receiving the player and the deposited items.
     */
    fun openShop(
        player: Player,
        items: () -> List<WanderingTraderItemData>,
        stockOf: (WanderingTraderItemData) -> Int,
        onPurchase: (Player, WanderingTraderItemData, Int) -> Unit,
        onDeposit: (Player, List<ItemStack?>) -> Unit,
    ) {
        buildShopWindow(player, items, stockOf, onPurchase, onDeposit).open()
    }

    /**
     * Closes every open shop and sell window, e.g. when the plugin is disabled, isolating each
     * close so a failing window cannot skip the rest. Closing a sell window processes its
     * deposited contents via its close handler, so this must run before any state is persisted.
     */
    fun closeAll() {
        openShops.keys.toList().forEach { closeSafely(it) }
        openSells.toList().forEach { closeSafely(it) }
    }

    /**
     * Closes [window], logging failures without propagating them, so one failing close cannot
     * prevent the remaining windows from closing.
     */
    private fun closeSafely(window: Window) {
        runCatching { window.close() }
            .onFailure { instance.logger.warning("Failed to close a wandering trader window: ${it.message}") }
    }

    /**
     * Builds the shop window, including paged content and navigation, and registers it on open
     * for live refreshes until it is closed.
     */
    private fun buildShopWindow(
        player: Player,
        items: () -> List<WanderingTraderItemData>,
        stockOf: (WanderingTraderItemData) -> Int,
        onPurchase: (Player, WanderingTraderItemData, Int) -> Unit,
        onDeposit: (Player, List<ItemStack?>) -> Unit,
    ): Window {
        lateinit var rebuild: () -> Unit
        val contentProvider = mutableProvider(emptyList<Item>())
        rebuild = { contentProvider.set(items().map { it.toGuiItem(stockOf, onPurchase) }) }
        rebuild()

        val shopWindow =
            window(player) {
                title by MM.deserialize(TITLE)
                upperGui by
                    pagedItemsGui(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < s > # # #",
                    ) {
                        '#' by BORDER
                        'x' by Markers.CONTENT_LIST_SLOT_HORIZONTAL
                        '<' by back
                        's' by
                            item {
                                itemProvider by ItemBuilder(Material.EMERALD).setName(SELL_BUTTON_NAME)
                                onClick {
                                    openSell(player, onDeposit) {
                                        buildShopWindow(player, items, stockOf, onPurchase, onDeposit)
                                    }
                                }
                            }
                        '>' by forward
                        content by contentProvider
                    }
            }
        shopWindow.addOpenHandler { openShops[shopWindow] = rebuild }
        shopWindow.addCloseHandler { openShops.remove(shopWindow) }
        return shopWindow
    }

    /**
     * Builds and opens the sell window: a deposit inventory whose contents are processed on close.
     * When the player closes the window themselves, [shopWindow] is rebuilt and reopened (deferred
     * one tick, as opening a window during close handling is not allowed, and rebuilt after the
     * deposit is processed so new stock shows up immediately). All open shop windows are refreshed
     * afterwards so other viewers see the new stock and prices.
     */
    private fun openSell(
        player: Player,
        onDeposit: (Player, List<ItemStack?>) -> Unit,
        shopWindow: () -> Window,
    ) {
        val deposit = VirtualInventory(DEPOSIT_SIZE)
        val sellWindow =
            window(player) {
                title by MM.deserialize(SELL_TITLE)
                upperGui by deposit
                onClose {
                    onDeposit(player, deposit.items.toList())
                    refreshAll()
                    if (reason == InventoryCloseEvent.Reason.PLAYER) {
                        player.scheduler.runDelayed(instance, { shopWindow().open() }, null, 1L)
                    }
                }
            }
        openSells += sellWindow
        sellWindow.addCloseHandler { openSells -= sellWindow }
        sellWindow.open()
    }

    /**
     * Rebuilds the content of every open shop window so all viewers see current stock and prices.
     */
    private fun refreshAll() = openShops.values.toList().forEach { it() }

    /**
     * Builds the button displaying this trade entry, describing price and current stock in its
     * lore. Left/middle/right clicks buy 1/10/100 units; every open shop window is refreshed
     * afterwards.
     */
    private fun WanderingTraderItemData.toGuiItem(
        stockOf: (WanderingTraderItemData) -> Int,
        onPurchase: (Player, WanderingTraderItemData, Int) -> Unit,
    ): Item =
        item {
            itemProvider by provider { ItemBuilder(icon(stockOf)) }
            onClick {
                val units =
                    when (clickType) {
                        ClickType.LEFT -> 1
                        ClickType.MIDDLE -> 10
                        ClickType.RIGHT -> 100
                        else -> return@onClick
                    }
                onPurchase(player, this@toGuiItem, units)
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
