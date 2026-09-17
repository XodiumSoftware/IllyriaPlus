package org.xodium.illyriacore.gui.stock

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.data.WanderingTraderItemData

/**
 * Bundles the callbacks the wandering trader GUIs use to talk back to the mechanic's state:
 * stocked trades, per-entry stock, purchase execution, and sell-window deposit processing.
 *
 * @property items Supplies the currently stocked trade entries, re-evaluated on every refresh.
 * @property stockOf Returns the current stock (individual items) of an entry.
 * @property purchase Called when an entry is clicked, receiving the buying player, the entry,
 * and the requested unit count.
 * @property deposit Called with the contents of the sell window when it closes, receiving the
 * selling player and the deposited items.
 */
internal data class StockHandlers(
    val items: () -> List<WanderingTraderItemData>,
    val stockOf: (WanderingTraderItemData) -> Int,
    val purchase: (Player, WanderingTraderItemData, Int) -> Unit,
    val deposit: (Player, List<ItemStack?>) -> Unit,
)
