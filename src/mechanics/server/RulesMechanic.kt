package org.xodium.illyriaplus.mechanics.server

import net.kyori.adventure.inventory.Book
import org.bukkit.Material
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling rules functionality within the system. */
internal object RulesMechanic : MechanicInterface {
    private val RULES: List<List<String>> =
        listOf(
            // Page 1: Player Rules (1-7)
            listOf(
                "<b><u><dark_aqua>Player Rules:<reset>",
                "",
                "<gold>▶ <dark_aqua>01 <dark_gray>| <red>No Griefing",
                "<gold>▶ <dark_aqua>02 <dark_gray>| <red>No Spamming",
                "<gold>▶ <dark_aqua>03 <dark_gray>| <red>No Advertising",
                "<gold>▶ <dark_aqua>04 <dark_gray>| <red>No Cursing/No Constant Cursing",
                "<gold>▶ <dark_aqua>05 <dark_gray>| <red>No Trolling/Flaming",
                "<gold>▶ <dark_aqua>06 <dark_gray>| <red>No Asking for OP, Ranks, or Items",
                "<gold>▶ <dark_aqua>07 <dark_gray>| <red>Respect all Players",
            ),
            // Page 2: Player Rules (8-13)
            listOf(
                "<gold>▶ <dark_aqua>08 <dark_gray>| <red>Obey Staff they are the Law Enforcers",
                "<gold>▶ <dark_aqua>09 <dark_gray>| <red>No Racist or Sexist Remarks",
                "<gold>▶ <dark_aqua>10 <dark_gray>| <red>No Mods/Hacks",
                "<gold>▶ <dark_aqua>12 <dark_gray>| <red>No 1x1 Towers",
                "<gold>▶ <dark_aqua>13 <dark_gray>| <red>Build in (Fantasy)Medieval style",
            ),
            // Page 3: Mod/Admin Rules
            listOf(
                "<b><u><dark_aqua>Mod/Admin Rules:<reset>",
                "",
                "<gold>▶ <dark_aqua>01 <dark_gray>| " +
                    "<red>Be Responsible with the power you are given as staff",
                "<gold>▶ <dark_aqua>02 <dark_gray>| " +
                    "<red>Do not spawn blocks or items for other players",
                "<gold>▶ <dark_aqua>03 <dark_gray>| <red>When Trading, only buy and sell legit items",
                "<gold>▶ <dark_aqua>05 <dark_gray>| <red>No Power Abuse",
            ),
        )

    override val faqItem =
        Item
            .builder()
            .setItemProvider(
                ItemBuilder(Material.WRITTEN_BOOK)
                    .setName(MM.deserialize("<mango>Rules Book</gradient>"))
                    .addLoreLines(
                        MM.deserialize(""),
                        MM.deserialize("<gray>Click to open</gray>"),
                    ),
            ).addClickHandler { _, click ->
                val player = click.player

                if (player.hasPermission(perms[0])) {
                    player.openBook(
                        Book.builder().pages(RULES.map { MM.deserialize(it.joinToString("\n")) }).build(),
                    )
                }
            }.build()

    override val faqCategory = FaqCategory.SERVER

    override val perms
        get() =
            listOf(
                Permission(
                    "${instance.javaClass.simpleName}.rules".lowercase(),
                    "Allows to access rules",
                    PermissionDefault.TRUE,
                ),
            )
}
