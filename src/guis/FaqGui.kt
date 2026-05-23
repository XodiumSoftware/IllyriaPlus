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
        val categories =
            mechanics.groupBy {
                if (it.isOpInfo) {
                    "admin"
                } else {
                    when {
                        it.javaClass.`package`.name
                            .endsWith("player") -> "player"

                        it.javaClass.`package`.name
                            .endsWith("world") -> "world"

                        it.javaClass.`package`.name
                            .endsWith("entity") -> "entity"

                        it.javaClass.`package`.name
                            .endsWith("server") -> "server"

                        else -> "other"
                    }
                }
            }
        val tabs =
            mutableListOf(
                createTab(categories["player"] ?: emptyList()),
                createTab(categories["world"] ?: emptyList()),
                createTab(categories["entity"] ?: emptyList()),
                createTab(categories["server"] ?: emptyList()),
            )
        val tabButtons =
            mutableListOf(
                createTabButton(
                    0,
                    ItemBuilder(Material.PLAYER_HEAD).setName(MM.deserialize("<mango>Player</gradient>")),
                ),
                createTabButton(
                    1,
                    ItemBuilder(Material.GRASS_BLOCK).setName(MM.deserialize("<mango>World</gradient>")),
                ),
                createTabButton(
                    2,
                    ItemBuilder(Material.WOLF_SPAWN_EGG).setName(MM.deserialize("<mango>Entity</gradient>")),
                ),
                createTabButton(3, ItemBuilder(Material.COMPASS).setName(MM.deserialize("<mango>Server</gradient>"))),
            )
        val structure: Array<String>

        if (player.isOp) {
            tabs.add(createTab(categories["admin"] ?: emptyList()))
            tabButtons.add(
                createTabButton(
                    4,
                    ItemBuilder(Material.COMMAND_BLOCK).setName(MM.deserialize("<mango>Admin</gradient>")),
                ),
            )
            structure =
                arrayOf(
                    "# # P W E S A # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # # # # # # #",
                )
        } else {
            structure =
                arrayOf(
                    "# # P W E S # # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # # # # # # #",
                )
        }

        val tabGuiBuilder =
            TabGui
                .builder()
                .setStructure(*structure)
                .addIngredient('#', Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)))
                .addIngredient('P', tabButtons[0])
                .addIngredient('W', tabButtons[1])
                .addIngredient('E', tabButtons[2])
                .addIngredient('S', tabButtons[3])

        if (player.isOp) tabGuiBuilder.addIngredient('A', tabButtons[4])

        return Window
            .builder()
            .setTitle(MM.deserialize("<firewatch><b>FAQ</b></gradient>"))
            .setUpperGui(
                tabGuiBuilder
                    .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .setTabs(tabs)
                    .build(),
            ).setViewer(player)
            .build()
    }

    /**
     * Creates a tab button that switches the [TabGui] to the given index when clicked.
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
            .setItemProvider { _, _ -> itemBuilder }
            .addClickHandler { _, gui, _ -> gui.tab = index }
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
                    .map { Item.simple(it.infoItem) }
                    .forEachIndexed { index, item -> setItem(index, item) }
            }
}
