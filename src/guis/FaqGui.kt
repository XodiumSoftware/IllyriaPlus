package org.xodium.illyriaplus.guis

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.GuiInterface
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Represents a gui handling faq within the system. */
internal object FaqGui : GuiInterface {
    lateinit var mechanics: List<MechanicInterface>

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("faq")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> gui(player).open() },
                "Opens the FAQ dialog",
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.faq".lowercase(),
                "Allows use of the /faq command",
                PermissionDefault.TRUE,
            ),
        )

    override fun gui(player: Player): Window {
        val categories = mechanics.groupBy { it.faqCategory }
        val tabConfigs =
            FaqCategory.entries.filter { it != FaqCategory.ADMIN || player.isOp }
        val tabs = tabConfigs.map { createTab(categories[it] ?: emptyList()) }
        val tabButtons =
            tabConfigs.mapIndexed { index, config ->
                createTabButton(
                    index,
                    ItemBuilder(config.material).setName(MM.deserialize(config.label)),
                )
            }
        val structure =
            if (player.isOp) {
                arrayOf(
                    "# # P W E S A # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # # # # # # #",
                )
            } else {
                arrayOf(
                    "# # P W # E S # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # # # # # # #",
                )
            }

        return Window
            .builder()
            .apply {
                setTitle(MM.deserialize("<firewatch><b>FAQ</b></gradient>"))
                setUpperGui(
                    TabGui
                        .builder()
                        .apply {
                            setStructure(*structure)
                            addIngredient(
                                '#',
                                Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)),
                            )
                            addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                            setTabs(tabs)
                            tabConfigs.zip(tabButtons).forEach { (config, button) ->
                                addIngredient(config.char, button)
                            }
                        }.build(),
                )
                setViewer(player)
            }.build()
    }

    /**
     * Creates a tab button that switches the [TabGui] to the given index when clicked.
     * The button shows an enchantment glint when its tab is active.
     *
     * @param index The tab index to switch to.
     * @param itemBuilder The display item for the tab button.
     * @return A [BoundItem] configured for tab navigation.
     */
    private fun createTabButton(
        index: Int,
        itemBuilder: ItemBuilder,
    ): BoundItem =
        BoundItem
            .tabBuilder()
            .setItemProvider { _, gui ->
                if (gui.tab == index) itemBuilder.clone().setGlint(true) else itemBuilder
            }.addClickHandler { _, gui, _ -> gui.tab = index }
            .build()

    /**
     * Creates a tab [Gui] populated with infoItems from the given mechanics.
     *
     * Items fill a 3×7 grid left-to-right, top-to-bottom.
     *
     * @param mechanics The list of mechanics to display in this tab.
     * @return A [Gui] containing the mechanic info items.
     */
    private fun createTab(mechanics: List<MechanicInterface>): Gui =
        Gui
            .builder()
            .setStructure(
                "x x x x x x x",
                "x x x x x x x",
                "x x x x x x x",
            ).build()
            .apply {
                mechanics
                    .take(21)
                    .forEachIndexed { index, mechanic -> setItem(index, mechanic.faqItem) }
            }
}
