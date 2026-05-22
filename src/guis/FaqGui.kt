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
import xyz.xenondevs.invui.gui.Animation
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Represents a gui handling faq within the system. */
internal object FaqGui : GuiInterface {
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
        val animation =
            Animation
                .builder()
                .setSlotSelector(Animation::horizontalSnakeSlotSelector)
                .filterTaggedSlots('x')
                .build()
        val content =
            listOf(
                BOOK_RULES,
                ITEM_NICKNAME,
                ITEM_LOCATOR,
                MECH_OPENABLE,
                MECH_TAMEABLE,
                MECH_ENDERCHEST,
                MECH_XP,
                MECH_HUSK,
                MECH_HEAD,
                CHAT_PLACEHOLDERS,
                MECH_INVENTORY,
                MECH_SIT,
                MECH_BOOKSHELF,
                MECH_DIMENSION,
                MECH_BAT,
                MECH_SPAWN_EGG,
            ).map { Item.simple(it) }
        val previous =
            BoundItem
                .pagedBuilder()
                .setItemProvider { _, gui ->
                    if (gui.page > 0) {
                        ItemBuilder(Material.ARROW)
                            .setName(
                                MM.deserialize("<gray>Move to page <aqua>${gui.page}<gray>/<aqua>${gui.pageCount}"),
                            )
                    } else {
                        ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                    }
                }.addClickHandler { _, gui, _ ->
                    gui.cancelAnimation()
                    gui.page--
                    gui.playAnimation(animation)
                }.build()
        val next =
            BoundItem
                .pagedBuilder()
                .setItemProvider { _, gui ->
                    if (gui.page < gui.pageCount - 1) {
                        ItemBuilder(Material.ARROW)
                            .setName(
                                MM.deserialize("<gray>Move to page <aqua>${gui.page + 2}<gray>/<aqua>${gui.pageCount}"),
                            )
                    } else {
                        ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                    }
                }.addClickHandler { _, gui, _ ->
                    gui.cancelAnimation()
                    gui.page++
                    gui.playAnimation(animation)
                }.build()

        return Window
            .builder()
            .setTitle(MM.deserialize("<firewatch><b>FAQ</b></gradient>"))
            .setUpperGui(
                PagedGui
                    .itemsBuilder()
                    .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < # > # # #",
                    ).addIngredient('#', Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)))
                    .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .addIngredient('<', previous)
                    .addIngredient('>', next)
                    .setContent(content)
                    .addModifier { it.playAnimation(animation) }
                    .build(),
            ).setViewer(player)
            .build()
    }

    private val BOOK_RULES =
        ItemBuilder(Material.WRITTEN_BOOK)
            .setName(MM.deserialize("<mango>Rules Book</gradient>"))
            .addLoreLines(MM.deserialize(""), MM.deserialize("<gray>cmd:</gray> <yellow>/rules</yellow>"))

    private val ITEM_NICKNAME =
        ItemBuilder(Material.NAME_TAG)
            .setName(MM.deserialize("<mango>Nickname</gradient>"))
            .addLoreLines(MM.deserialize(""), MM.deserialize("<gray>cmd:</gray> <yellow>/nickname</yellow>"))

    private val ITEM_LOCATOR =
        ItemBuilder(Material.COMPASS)
            .setName(MM.deserialize("<mango>Locator</gradient>"))
            .addLoreLines(MM.deserialize(""), MM.deserialize("<gray>cmd:</gray> <yellow>/locator</yellow>"))

    private val MECH_OPENABLE =
        ItemBuilder(Material.DARK_OAK_DOOR)
            .setName(MM.deserialize("<mango>Openable Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Double Doors</yellow> <firewatch>></gradient> <white>Sync open/close together</white>",
                ),
                MM.deserialize(
                    "<yellow>Knocking</yellow> <firewatch>></gradient> <white>Sneak + " +
                        "left-click with empty hand</white>",
                ),
            )

    private val MECH_TAMEABLE =
        ItemBuilder(Material.WOLF_SPAWN_EGG)
            .setName(MM.deserialize("<mango>Tameable Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Transfer Pets</yellow> <firewatch>></gradient> <white>Hold lead + " +
                        "right-click player</white>",
                ),
            )

    private val MECH_ENDERCHEST =
        ItemBuilder(Material.ENDER_CHEST)
            .setName(MM.deserialize("<mango>Enderchest Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Portable Access</yellow> <firewatch>></gradient> " +
                        "<white>Right-click air with ender chest</white>",
                ),
            )

    private val MECH_XP =
        ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .setName(MM.deserialize("<mango>XP Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Bottle XP</yellow> <firewatch>></gradient> <white>Sneak + " +
                        "right-click enchanting table with bottle</white>",
                ),
            )

    private val MECH_HUSK =
        ItemBuilder(Material.SAND)
            .setName(MM.deserialize("<mango>Husk Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Sand Drops</yellow> <firewatch>></gradient> <white>Drop 0-2 sand " +
                        "(+Looting, bonus on camel)</white>",
                ),
            )

    private val MECH_HEAD =
        ItemBuilder(Material.PLAYER_HEAD)
            .setName(MM.deserialize("<mango>Head Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Player Heads</yellow> <firewatch>></gradient> <white>1% chance to drop on death</white>",
                ),
            )

    private val CHAT_PLACEHOLDERS =
        ItemBuilder(Material.TORCH)
            .setName(MM.deserialize("<mango>Chat Placeholders</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize("<yellow>[item,i]</yellow> <firewatch>></gradient> <white>Shows your held item</white>"),
                MM.deserialize("<yellow>[pos]</yellow> <firewatch>></gradient> <white>Shows your position</white>"),
                MM.deserialize("<yellow>@player</yellow> <firewatch>></gradient> <white>Mentions a player</white>"),
            )

    private val MECH_INVENTORY =
        ItemBuilder(Material.CHEST)
            .setName(MM.deserialize("<mango>Inventory Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<gray>cmd:</gray> <yellow>/search</yellow> <firewatch>></gradient> " +
                        "<white>Find items in nearby chests</white>",
                ),
                MM.deserialize(
                    "<gray>cmd:</gray> <yellow>/unload</yellow> <firewatch>></gradient> " +
                        "<white>Dump inventory into nearby chests</white>",
                ),
            )

    private val MECH_SIT =
        ItemBuilder(Material.OAK_STAIRS)
            .setName(MM.deserialize("<mango>Sit Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Sit Anywhere</yellow> <firewatch>></gradient> " +
                        "<white>Right-click bottom stairs/slabs</white>",
                ),
                MM.deserialize(
                    "<yellow>Stand Up</yellow> <firewatch>></gradient> " +
                        "<white>Take damage, break block, or dismount</white>",
                ),
            )

    private val MECH_BOOKSHELF =
        ItemBuilder(Material.BOOKSHELF)
            .setName(MM.deserialize("<mango>Bookshelf Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Peek Books</yellow> <firewatch>></gradient> " +
                        "<white>Left-click front face to inspect slot</white>",
                ),
            )

    private val MECH_DIMENSION =
        ItemBuilder(Material.OBSIDIAN)
            .setName(MM.deserialize("<mango>Dimension Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Portal Linking</yellow> <firewatch>></gradient> " +
                        "<white>Nether portals require Overworld link</white>",
                ),
            )

    private val MECH_BAT =
        ItemBuilder(Material.PHANTOM_MEMBRANE)
            .setName(MM.deserialize("<mango>Bat Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Membrane Drops</yellow> <firewatch>></gradient> " +
                        "<white>Drop 0-1 phantom membrane (+Looting)</white>",
                ),
            )

    private val MECH_SPAWN_EGG =
        ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
            .setName(MM.deserialize("<mango>Spawn Egg Mechanics</gradient>"))
            .addLoreLines(
                MM.deserialize(""),
                MM.deserialize(
                    "<yellow>Rare Drops</yellow> <firewatch>></gradient> " +
                        "<white>0.1% chance for mobs to drop their spawn egg</white>",
                ),
            )
}
