package org.xodium.illyriaplus.items

/** Represents the Trowel custom item. */
internal object Trowel : ItemInterface {
    override val lore: List<String> =
        listOf(
            "",
            "<gray>Right-click to place a random block</gray>",
            "<gray>from your hotbar.</gray>",
        )
}
