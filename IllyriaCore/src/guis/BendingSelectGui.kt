package org.xodium.illyriacore.guis

import org.bukkit.Material
import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.BendingTypeEnum
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/**
 * GUI for selecting a bending type.
 *
 * Black-stained-glass-pane border, 2x2 grid of bending-type buttons in the center.
 */
internal object BendingSelectGui {
    /** Maps each [BendingTypeEnum] to its display item material. */
    private val TYPE_ITEMS =
        mapOf(
            BendingTypeEnum.AIR to Material.WHITE_CONCRETE,
            BendingTypeEnum.EARTH to Material.BROWN_CONCRETE,
            BendingTypeEnum.FIRE to Material.RED_CONCRETE,
            BendingTypeEnum.WATER to Material.BLUE_CONCRETE,
        )

    /**
     * Opens the bending selection GUI for the given [player].
     *
     * @param player The player viewing the GUI.
     * @param onSelect Callback invoked with the chosen [BendingTypeEnum].
     */
    fun open(
        player: Player,
        onSelect: (BendingTypeEnum) -> Unit,
    ) {
        Window
            .builder()
            .setTitle("<firewatch>Choose Your Bending</gradient>")
            .setUpperGui(
                Gui
                    .builder()
                    .setStructure("A E # F W")
                    .addIngredient('#', Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)))
                    .addIngredient('A', button(BendingTypeEnum.AIR, onSelect))
                    .addIngredient('E', button(BendingTypeEnum.EARTH, onSelect))
                    .addIngredient('F', button(BendingTypeEnum.FIRE, onSelect))
                    .addIngredient('W', button(BendingTypeEnum.WATER, onSelect)),
            ).open(player)
    }

    /** Builds the selection button for the given [type]. */
    private fun button(
        type: BendingTypeEnum,
        onSelect: (BendingTypeEnum) -> Unit,
    ): Item {
        val label = type.name.lowercase().replaceFirstChar { it.uppercase() }
        return Item
            .builder()
            .setItemProvider(
                ItemBuilder(TYPE_ITEMS.getValue(type))
                    .setName("<${type.color}>$label</${type.color}>")
                    .addMiniMessageLoreLines(
                        listOf(
                            "<gray>Click to become a <${type.color}>${label.lowercase()}</${type.color}> bender</gray>",
                        ),
                    ),
            ).addClickHandler { _, click ->
                onSelect(type)
                click.player.closeInventory()
            }.build()
    }
}
