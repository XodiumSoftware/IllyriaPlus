package org.xodium.illyriakingdoms.gui

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.Utils.MM
import org.xodium.illyriakingdoms.data.KingdomData
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.pagedItemsGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

/** Builds and opens the kingdom members GUI. */
@OptIn(ExperimentalDslApi::class)
internal object MemberGui {
    private const val PREVIOUS_PAGE_NAME = "<gray>Previous page"
    private const val NEXT_PAGE_NAME = "<gray>Next page"

    private val BORDER = Item.simple(ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).hideTooltip(true))

    private val back =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page > 0) {
                    ItemBuilder(Material.ARROW).setName(PREVIOUS_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page-- }

    private val forward =
        BoundItem
            .pagedBuilder()
            .setItemProvider { _, gui ->
                if (gui.page < gui.pageCount - 1) {
                    ItemBuilder(Material.ARROW).setName(NEXT_PAGE_NAME)
                } else {
                    ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)
                }
            }.addClickHandler { _, gui, _ -> gui.page++ }

    /**
     * Opens the kingdom members GUI for the given player.
     *
     * @param player The player viewing the GUI.
     * @param kingdom The kingdom to list members for.
     */
    fun open(player: Player, kingdom: KingdomData) {
        buildWindow(player, kingdom).open()
    }

    private fun buildWindow(
        player: Player,
        kingdom: KingdomData,
    ): Window {
        val allMembers = sortedMembers(kingdom)
        val contentProvider = mutableProvider(allMembers)

        return window(player) {
            title by kingdom.name.append(MM.deserialize(" <gray>Members"))
            upperGui by
                pagedItemsGui(
                    "# # # # # # # # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # < # > # # #",
                ) {
                    '#' by BORDER
                    'x' by Markers.CONTENT_LIST_SLOT_HORIZONTAL
                    '<' by back
                    '>' by forward
                    content by contentProvider
                }
        }
    }

    /**
     * Sorts members alphabetically by display name, with the owner first.
     */
    private fun sortedMembers(kingdom: KingdomData): List<Item> {
        val ownerUuid = kingdom.owner
        val ownerName = instance.server.getOfflinePlayer(ownerUuid).name ?: ownerUuid.toString().substring(0, 8)
        val ownerStack =
            playerHead(
                instance.server.getOfflinePlayer(ownerUuid).playerProfile,
                ownerName,
                "<gradient:#FFE259:#FFA751>Owner"
            )
        val memberStacks = kingdom.members
            .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
            .map { memberUuid ->
                val memberName =
                    instance.server.getOfflinePlayer(memberUuid).name ?: memberUuid.toString().substring(0, 8)
                playerHead(instance.server.getOfflinePlayer(memberUuid).playerProfile, memberName, null)
            }

        return (listOf(ownerStack) + memberStacks).map { Item.simple(it) }
    }

    /**
     * Creates a [ItemStack] player head with the given profile, display name, and optional lore.
     *
     * @param profile the player profile to use for the skin.
     * @param name the display name for the head.
     * @param lore optional MiniMessage lore line to display under the name.
     * @return the configured player head ItemStack.
     */
    private fun playerHead(
        profile: PlayerProfile,
        name: String,
        lore: String?,
    ): ItemStack =
        ItemStack.of(Material.PLAYER_HEAD).apply {
            setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile))
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<reset>$name"))
            if (lore != null) {
                setData(
                    DataComponentTypes.LORE,
                    ItemLore.lore(listOf(MM.deserialize(lore).decoration(TextDecoration.ITALIC, false)))
                )
            }
        }
}
