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
import xyz.xenondevs.invui.gui.Animation
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
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
        val animation =
            Animation
                .builder()
                .setSlotSelector(Animation::horizontalSnakeSlotSelector)
                .filterTaggedSlots('x')
                .build()
        val content =
            mechanics
                .filter { !it.isOpInfo || player.isOp }
                .map { Item.simple(it.infoItem) }
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
                    if (gui.page > 0) {
                        gui.cancelAnimation()
                        gui.page--
                        gui.playAnimation(animation)
                    }
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
                    if (gui.page < gui.pageCount - 1) {
                        gui.cancelAnimation()
                        gui.page++
                        gui.playAnimation(animation)
                    }
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
}
