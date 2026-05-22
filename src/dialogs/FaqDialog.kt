package org.xodium.illyriaplus.dialogs

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.DialogInterface

/** Represents a dialog handling faq within the system. */
@Suppress("UnstableApiUsage")
internal object FaqDialog : DialogInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("faq")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.showDialog(dialog) },
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

    override val dialog by lazy {
        Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(Utils.MM.deserialize("<firewatch>FAQ</gradient>"))
                        .body(
                            listOf(
                                DialogBody.item(BOOK_RULES).build(),
                                DialogBody.item(ITEM_NICKNAME).build(),
                                DialogBody.item(ITEM_LOCATOR).build(),
                                DialogBody.item(MECH_OPENABLE).build(),
                                DialogBody.item(MECH_TAMEABLE).build(),
                                DialogBody.item(MECH_ENDERCHEST).build(),
                                DialogBody.item(MECH_XP).build(),
                                DialogBody.item(MECH_HUSK).build(),
                                DialogBody.item(MECH_HEAD).build(),
                                DialogBody.item(CHAT_PLACEHOLDERS).build(),
                                DialogBody.item(MECH_INVENTORY).build(),
                                DialogBody.item(MECH_SIT).build(),
                                DialogBody.item(MECH_BOOKSHELF).build(),
                                DialogBody.item(MECH_DIMENSION).build(),
                                DialogBody.item(MECH_BAT).build(),
                                DialogBody.item(MECH_SPAWN_EGG).build(),
                            ),
                        ).build(),
                ).type(DialogType.notice())
        }
    }

    /**
     * Creates an [ItemStack] with the specified [material], display name, and lore lines.
     *
     * The name is wrapped in a mango gradient, and each lore line is deserialized with MiniMessage.
     * An empty line is prepended to the lore for visual spacing.
     *
     * @param material The item material.
     * @param name The display name rendered with `<mango>...</gradient>`.
     * @param loreLines MiniMessage-formatted lore lines.
     * @return The configured [ItemStack].
     */
    private fun itemStackOf(
        material: Material,
        name: String,
        vararg loreLines: String,
    ): ItemStack =
        ItemStack.of(material).apply {
            setData(DataComponentTypes.ITEM_NAME, Utils.MM.deserialize("<mango>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(Component.empty()) + loreLines.map { Utils.MM.deserialize(it) }),
            )
        }

    private val BOOK_RULES =
        itemStackOf(
            Material.WRITTEN_BOOK,
            "Rules Book",
            "<gray>cmd:</gray> <yellow>/Rules</yellow>",
        )

    private val ITEM_NICKNAME =
        itemStackOf(
            Material.NAME_TAG,
            "Nickname",
            "<gray>cmd:</gray> <yellow>/nickname</yellow>",
        )

    private val ITEM_LOCATOR =
        itemStackOf(
            Material.COMPASS,
            "Locator",
            "<gray>cmd:</gray> <yellow>/locator</yellow>",
        )

    private val MECH_OPENABLE =
        itemStackOf(
            Material.DARK_OAK_DOOR,
            "Openable Mechanics",
            "<yellow>Double Doors</yellow> <firewatch>></gradient> <white>Sync open/close together</white>",
            "<yellow>Knocking</yellow> <firewatch>></gradient> <white>Sneak + left-click with empty hand</white>",
        )

    private val MECH_TAMEABLE =
        itemStackOf(
            Material.WOLF_SPAWN_EGG,
            "Tameable Mechanics",
            "<yellow>Transfer Pets</yellow> <firewatch>></gradient> <white>Hold lead + right-click player</white>",
        )

    private val MECH_ENDERCHEST =
        itemStackOf(
            Material.ENDER_CHEST,
            "Enderchest Mechanics",
            "<yellow>Portable Access</yellow> <firewatch>></gradient> <white>Right-click air with ender chest</white>",
        )

    private val MECH_XP =
        itemStackOf(
            Material.EXPERIENCE_BOTTLE,
            "XP Mechanics",
            "<yellow>Bottle XP</yellow> <firewatch>></gradient> <white>Sneak + right-click enchanting table with bottle</white>",
        )

    private val MECH_HUSK =
        itemStackOf(
            Material.SAND,
            "Husk Mechanics",
            "<yellow>Sand Drops</yellow> <firewatch>></gradient> <white>Drop 0-2 sand (+Looting, bonus on camel)</white>",
        )

    private val MECH_HEAD =
        itemStackOf(
            Material.PLAYER_HEAD,
            "Head Mechanics",
            "<yellow>Player Heads</yellow> <firewatch>></gradient> <white>1% chance to drop on death</white>",
        )

    private val CHAT_PLACEHOLDERS =
        itemStackOf(
            Material.TORCH,
            "Chat Placeholders",
            "<yellow>[item,i]</yellow> <firewatch>></gradient> <white>Shows your held item</white>",
            "<yellow>[pos]</yellow> <firewatch>></gradient> <white>Shows your position</white>",
            "<yellow>@player</yellow> <firewatch>></gradient> <white>Mentions a player</white>",
        )

    private val MECH_INVENTORY =
        itemStackOf(
            Material.CHEST,
            "Inventory Mechanics",
            "<gray>cmd:</gray> <yellow>/search</yellow> <firewatch>></gradient> <white>Find items in nearby chests</white>",
            "<gray>cmd:</gray> <yellow>/unload</yellow> <firewatch>></gradient> <white>Dump inventory into nearby chests</white>",
        )

    private val MECH_SIT =
        itemStackOf(
            Material.OAK_STAIRS,
            "Sit Mechanics",
            "<yellow>Sit Anywhere</yellow> <firewatch>></gradient> <white>Right-click bottom stairs/slabs</white>",
            "<yellow>Stand Up</yellow> <firewatch>></gradient> <white>Take damage, break block, or dismount</white>",
        )

    private val MECH_BOOKSHELF =
        itemStackOf(
            Material.BOOKSHELF,
            "Bookshelf Mechanics",
            "<yellow>Peek Books</yellow> <firewatch>></gradient> <white>Left-click front face to inspect slot</white>",
        )

    private val MECH_DIMENSION =
        itemStackOf(
            Material.OBSIDIAN,
            "Dimension Mechanics",
            "<yellow>Portal Linking</yellow> <firewatch>></gradient> <white>Nether portals require Overworld link</white>",
        )

    private val MECH_BAT =
        itemStackOf(
            Material.PHANTOM_MEMBRANE,
            "Bat Mechanics",
            "<yellow>Membrane Drops</yellow> <firewatch>></gradient> <white>Drop 0-1 phantom membrane (+Looting)</white>",
        )

    private val MECH_SPAWN_EGG =
        itemStackOf(
            Material.ZOMBIE_SPAWN_EGG,
            "Spawn Egg Mechanics",
            "<yellow>Rare Drops</yellow> <firewatch>></gradient> <white>0.1% chance for mobs to drop their spawn egg</white>",
        )
}
