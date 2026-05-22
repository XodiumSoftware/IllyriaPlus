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
                            .setName("<gray>Move to page <aqua>${gui.page}<gray>/<aqua>${gui.pageCount}")
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
                            .setName("<gray>Move to page <aqua>${gui.page + 2}<gray>/<aqua>${gui.pageCount}")
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
            .setTitle(MM.deserialize("<firewatch>FAQ</gradient>"))
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
            .setName("<mango>Rules Book</gradient>")
            .addLoreLines("", "<gray>cmd:</gray> <yellow>/Rules</yellow>")

    private val ITEM_NICKNAME =
        ItemBuilder(Material.NAME_TAG)
            .setName("<mango>Nickname</gradient>")
            .addLoreLines("", "<gray>cmd:</gray> <yellow>/nickname</yellow>")

    private val ITEM_LOCATOR =
        ItemBuilder(Material.COMPASS)
            .setName("<mango>Locator</gradient>")
            .addLoreLines("", "<gray>cmd:</gray> <yellow>/locator</yellow>")

    private val MECH_OPENABLE =
        ItemBuilder(Material.DARK_OAK_DOOR)
            .setName("<mango>Openable Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Double Doors</yellow> <firewatch>></gradient> <white>Sync open/close together</white>",
                "<yellow>Knocking</yellow> <firewatch>></gradient> <white>Sneak + left-click with empty hand</white>",
            )

    private val MECH_TAMEABLE =
        ItemBuilder(Material.WOLF_SPAWN_EGG)
            .setName("<mango>Tameable Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Transfer Pets</yellow> <firewatch>></gradient> <white>Hold lead + right-click player</white>",
            )

    private val MECH_ENDERCHEST =
        ItemBuilder(Material.ENDER_CHEST)
            .setName("<mango>Enderchest Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Portable Access</yellow> <firewatch>></gradient> <white>Right-click air with ender chest</white>",
            )

    private val MECH_XP =
        ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .setName("<mango>XP Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Bottle XP</yellow> <firewatch>></gradient> <white>Sneak + right-click enchanting table with bottle</white>",
            )

    private val MECH_HUSK =
        ItemBuilder(Material.SAND)
            .setName("<mango>Husk Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Sand Drops</yellow> <firewatch>></gradient> <white>Drop 0-2 sand (+Looting, bonus on camel)</white>",
            )

    private val MECH_HEAD =
        ItemBuilder(Material.PLAYER_HEAD)
            .setName("<mango>Head Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Player Heads</yellow> <firewatch>></gradient> <white>1% chance to drop on death</white>",
            )

    private val CHAT_PLACEHOLDERS =
        ItemBuilder(Material.TORCH)
            .setName("<mango>Chat Placeholders</gradient>")
            .addLoreLines(
                "",
                "<yellow>[item,i]</yellow> <firewatch>></gradient> <white>Shows your held item</white>",
                "<yellow>[pos]</yellow> <firewatch>></gradient> <white>Shows your position</white>",
                "<yellow>@player</yellow> <firewatch>></gradient> <white>Mentions a player</white>",
            )

    private val MECH_INVENTORY =
        ItemBuilder(Material.CHEST)
            .setName("<mango>Inventory Mechanics</gradient>")
            .addLoreLines(
                "",
                "<gray>cmd:</gray> <yellow>/search</yellow> <firewatch>></gradient> <white>Find items in nearby chests</white>",
                "<gray>cmd:</gray> <yellow>/unload</yellow> <firewatch>></gradient> <white>Dump inventory into nearby chests</white>",
            )

    private val MECH_SIT =
        ItemBuilder(Material.OAK_STAIRS)
            .setName("<mango>Sit Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Sit Anywhere</yellow> <firewatch>></gradient> <white>Right-click bottom stairs/slabs</white>",
                "<yellow>Stand Up</yellow> <firewatch>></gradient> <white>Take damage, break block, or dismount</white>",
            )

    private val MECH_BOOKSHELF =
        ItemBuilder(Material.BOOKSHELF)
            .setName("<mango>Bookshelf Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Peek Books</yellow> <firewatch>></gradient> <white>Left-click front face to inspect slot</white>",
            )

    private val MECH_DIMENSION =
        ItemBuilder(Material.OBSIDIAN)
            .setName("<mango>Dimension Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Portal Linking</yellow> <firewatch>></gradient> <white>Nether portals require Overworld link</white>",
            )

    private val MECH_BAT =
        ItemBuilder(Material.PHANTOM_MEMBRANE)
            .setName("<mango>Bat Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Membrane Drops</yellow> <firewatch>></gradient> <white>Drop 0-1 phantom membrane (+Looting)</white>",
            )

    private val MECH_SPAWN_EGG =
        ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
            .setName("<mango>Spawn Egg Mechanics</gradient>")
            .addLoreLines(
                "",
                "<yellow>Rare Drops</yellow> <firewatch>></gradient> <white>0.1% chance for mobs to drop their spawn egg</white>",
            )
}
