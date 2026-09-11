package org.xodium.illyriaplus.data

import org.bukkit.inventory.ItemStack

/**
 * Represents an item a wandering trader trades and its value.
 *
 * @property result The [ItemStack] defining one trade unit (type and stack size).
 * @property price The [ItemStack] players pay per unit when buying; the trader pays half of it when buying from players.
 */
internal data class WanderingTraderItemData(
    val result: ItemStack,
    val price: ItemStack,
)
