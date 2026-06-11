package org.xodium.illyriaplus.items

/** Represents the Chisel custom item. */
internal object Chisel : ItemInterface {
    override val lore: List<String> =
        listOf(
            "",
            "<gray>Right-click a stone block to</gray>",
            "<gray>cycle its variant.</gray>",
        )
}
