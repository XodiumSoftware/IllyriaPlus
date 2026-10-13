package org.xodium.illyriamannequins

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.combatMode
import org.xodium.illyriamannequins.MannequinPDC.movementMode
import org.xodium.illyriamannequins.MannequinPDC.owner
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.pagedItemsGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemWrapper
import java.util.Locale

/** Inventory GUI listing every mannequin a player owns. */
@OptIn(ExperimentalDslApi::class)
@Suppress("UnstableApiUsage")
internal object MannequinListUI {
    private const val TITLE = "<firewatch>Your Mannequins</gradient>"
    private const val OTHER_TITLE_PREFIX = "<firewatch>Mannequins of </gradient><white>"
    private const val UNNAMED_PREFIX = "NPC "
    private const val LOCATION_LABEL = "<gray>Location: <white>%s %d %d %d"
    private const val MOVEMENT_LABEL = "<gray>Movement: "
    private const val COMBAT_LABEL = "<gray>Combat: "
    private const val MAIN_HAND_LABEL = "<gray>Main Hand: "
    private const val DESCRIPTION_LABEL = "<gray>Desc: <white>"
    private const val EDIT_HINT = "<yellow>Left-click to edit"
    private const val TELEPORT_HINT_OP = "<yellow>Shift-right-click to teleport"
    private const val PREVIOUS_TITLE = "<red>Previous Page"
    private const val NEXT_TITLE = "<green>Next Page"
    private const val SPAWN_TITLE = "<green>Spawn Mannequin"
    private const val EMPTY_TEXT = "<dark_gray>No mannequins"

    /**
     * Opens the mannequin list GUI.
     *
     * Lists all mannequins owned by [target] across all worlds, shown to [viewer].
     * Left-clicking a mannequin opens the edit dialog; operators can shift-right-click
     * to teleport.
     *
     * @param viewer The player to show the GUI to.
     * @param target The player whose mannequins to list.
     */
    fun open(
        viewer: Player,
        target: Player,
    ) {
        val mannequins =
            instance
                .server
                .worlds
                .flatMap { it.entities.filterIsInstance<Mannequin>() }
                .filter { it.owner == target.uniqueId }
                .sortedBy { it.customName()?.let(MM::serialize) ?: it.uniqueId.toString() }

        window(viewer) {
            title by MM.deserialize(if (viewer == target) TITLE else "$OTHER_TITLE_PREFIX${target.name}")
            upperGui by
                pagedItemsGui(
                    "# # # # # # # # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # < + > # # #",
                ) {
                    '#' by ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                    'x' by Markers.CONTENT_LIST_SLOT_HORIZONTAL
                    '<' by pageButton(PREVIOUS_TITLE) { it - 1 }
                    '>' by pageButton(NEXT_TITLE) { it + 1 }
                    '+' by spawnButton(viewer, target)

                    val items =
                        mannequins
                            .mapIndexed { index, mannequin -> mannequinItem(mannequin, index + 1, viewer.isOp) }
                            .toMutableList()
                    if (items.isEmpty()) {
                        items +=
                            Item.simple(
                                ItemBuilder(Material.ARMOR_STAND)
                                    .setName(MM.deserialize(EMPTY_TEXT)),
                            )
                    }
                    content by items
                }
        }.open()
    }

    /**
     * Builds a clickable head [Item] representing a mannequin.
     *
     * The head shows the mannequin's owner-facing summary: name, world and coordinate
     * position, movement mode, combat mode, and the click actions available.
     *
     * Dynamic [ResolvableProfile]s (name-only lookups) are resolved asynchronously via
     * [ResolvableProfile.resolve] so the rendered head shows the real skin instead of
     * the client's "Dynamic" tooltip line.
     *
     * @param mannequin The mannequin to create the [Item] for.
     * @param fallbackIndex The 1-based index used to name an unnamed mannequin (`NPC 1`, `NPC 2`, ...).
     * @param viewerIsOp Whether the GUI viewer has operator permissions.
     * @return An invui [Item] rendering this mannequin in the list.
     */
    private fun mannequinItem(
        mannequin: Mannequin,
        fallbackIndex: Int,
        viewerIsOp: Boolean,
    ): Item {
        val displayName =
            (mannequin.customName() ?: Component.text(UNNAMED_PREFIX + fallbackIndex))
                .color(NamedTextColor.WHITE)
        val lore = buildLore(mannequin, viewerIsOp)

        val profile = mannequin.profile
        val placeholder =
            ItemWrapper(buildHead(profile, displayName, lore))
        return Item
            .builder()
            .also { builder ->
                if (profile.dynamic()) {
                    builder.async(
                        placeholder,
                        profile.resolve().thenApply { resolved ->
                            ItemWrapper(buildHead(ResolvableProfile.resolvableProfile(resolved), displayName, lore))
                        },
                    )
                } else {
                    builder.setItemProvider(placeholder)
                }
            }.addClickHandler { _, click ->
                when (click.clickType) {
                    ClickType.LEFT -> {
                        click.player.closeInventory()
                        MannequinEditDialog.show(click.player, mannequin)
                    }
                    ClickType.SHIFT_RIGHT -> {
                        if (click.player.isOp) {
                            click.player.closeInventory()
                            click.player.teleportAsync(mannequin.location.clone().add(0.0, 1.0, 0.0))
                        }
                    }
                    else -> {}
                }
            }.build()
    }

    /**
     * Builds the head [ItemStack] for a mannequin list entry, applying the given profile,
     * display name, and lore.
     *
     * @param profile The [ResolvableProfile] to set on the head.
     * @param displayName The tooltip title for the head.
     * @param lore The tooltip lore lines.
     * @return The built head [ItemStack].
     */
    private fun buildHead(
        profile: ResolvableProfile,
        displayName: Component,
        lore: List<Component>,
    ): ItemStack {
        val head = ItemStack.of(Material.PLAYER_HEAD)
        head.setData(DataComponentTypes.PROFILE, profile)
        return ItemBuilder(head)
            .setCustomName(displayName.decoration(TextDecoration.ITALIC, false))
            .addLoreLines(lore)
            .build()
    }

    /**
     * Builds the lore lines summarizing a mannequin.
     *
     * @param mannequin The mannequin to summarize.
     * @param viewerIsOp Whether the GUI viewer has operator permissions.
     * @return The ordered lore [Component] lines.
     */
    private fun buildLore(
        mannequin: Mannequin,
        viewerIsOp: Boolean,
    ): List<Component> {
        val lore = mutableListOf<Component>()
        lore += Component.empty()
        lore +=
            MM
                .deserialize(DESCRIPTION_LABEL)
                .append(
                    mannequin.description?.colorIfAbsent(NamedTextColor.WHITE)
                        ?: Component.empty(),
                ).decoration(TextDecoration.ITALIC, false)
        lore +=
            MM
                .deserialize(
                    String.format(
                        Locale.ROOT,
                        LOCATION_LABEL,
                        mannequin.world.name,
                        mannequin.location.blockX,
                        mannequin.location.blockY,
                        mannequin.location.blockZ,
                    ),
                ).decoration(TextDecoration.ITALIC, false)
        lore +=
            MM
                .deserialize(MOVEMENT_LABEL)
                .append(mannequin.movementMode.display)
                .decoration(TextDecoration.ITALIC, false)
        lore +=
            MM
                .deserialize(COMBAT_LABEL)
                .append(mannequin.combatMode.display)
                .decoration(TextDecoration.ITALIC, false)
        lore +=
            MM
                .deserialize(MAIN_HAND_LABEL)
                .append(
                    Component.text(
                        mannequin
                            .mainHand
                            .name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        NamedTextColor.WHITE,
                    ),
                ).decoration(TextDecoration.ITALIC, false)
        lore += Component.empty()
        lore +=
            MM
                .deserialize(EDIT_HINT)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
        if (viewerIsOp) {
            lore +=
                MM
                    .deserialize(TELEPORT_HINT_OP)
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
        }
        return lore
    }

    /**
     * Builds the spawn button [Item]. Non-op viewers see plain black glass instead.
     *
     * @param viewer The player viewing the GUI (checked for op status).
     * @param target The player the spawned mannequin will belong to.
     * @return The spawn [Item].
     */
    private fun spawnButton(
        viewer: Player,
        target: Player,
    ): Item =
        if (viewer.isOp) {
            Item
                .builder()
                .setItemProvider(
                    ItemWrapper(
                        ItemBuilder(Material.NETHER_STAR)
                            .setName(MM.deserialize(SPAWN_TITLE))
                            .build(),
                    ),
                ).addClickHandler { _, click ->
                    click.player.closeInventory()
                    MannequinMechanic.spawnMannequin(click.player.location, target)
                }.build()
        } else {
            Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true))
        }

    /**
     * Builds a [BoundItem] that turns the bound [PagedGui] to a new page.
     *
     * @param title The MiniMessage title shown on the button.
     * @param target Computes the target page from the current one.
     * @return The navigation [BoundItem].
     */
    private fun pageButton(
        title: String,
        target: (Int) -> Int,
    ): BoundItem =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (isValidPage(gui, target(gui.page))) {
                    ItemBuilder(Material.ARROW)
                        .setName(MM.deserialize(title))
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ ->
                val next = target(gui.page)
                if (isValidPage(gui, next)) {
                    gui.page = next
                }
            }.build()

    /**
     * Checks if a page index is within the bound [PagedGui]'s range.
     *
     * @param gui The [PagedGui] to check against.
     * @param page The page index to validate.
     * @return `true` if the page exists.
     */
    private fun isValidPage(
        gui: PagedGui<*>,
        page: Int,
    ): Boolean = page in 0 until gui.pageCount
}
