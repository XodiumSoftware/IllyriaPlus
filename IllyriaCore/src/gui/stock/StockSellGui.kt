package org.xodium.illyriacore.gui.stock

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.gui.GuiInterface
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.window.Window

/** Builds and opens the trader's sell window: a deposit inventory whose contents are processed on close. */
@OptIn(ExperimentalDslApi::class)
internal object StockSellGui : GuiInterface {
    private const val TITLE = "<mango>Sell to the Trader"
    private const val DEPOSIT_SIZE = 54

    override val openWindows: MutableSet<Window> = mutableSetOf()

    /**
     * Builds and opens the sell window. When the player closes the window themselves, its contents
     * are processed and the player is returned to the shop (picker or category window) supplied by
     * [reopen]. All open buy windows are refreshed afterwards so viewers see new stock and prices.
     *
     * @param player The player selling to the trader.
     * @param handlers The trader's callbacks (deposit processing, items, stock of, purchase).
     * @param onRefresh Called after the deposit is processed to refresh all open buy windows.
     * @param reopen Supplies the shop window to reopen one tick after a player-initiated close.
     */
    fun open(
        player: Player,
        handlers: StockHandlers,
        onRefresh: () -> Unit,
        reopen: () -> Unit,
    ) {
        val deposit = VirtualInventory(DEPOSIT_SIZE)
        val sellWindow =
            window(player) {
                title by MM.deserialize(TITLE)
                upperGui by deposit
                onClose {
                    handlers.deposit(player, deposit.items.toList())
                    onRefresh()
                    if (reason == InventoryCloseEvent.Reason.PLAYER) {
                        player.scheduler.runDelayed(instance, { reopen() }, null, 1L)
                    }
                }
            }
        openWindows += sellWindow
        sellWindow.addCloseHandler { openWindows -= sellWindow }
        sellWindow.open()
    }
}
