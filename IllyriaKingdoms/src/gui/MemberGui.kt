package org.xodium.illyriakingdoms.gui

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.Utils.MM
import org.xodium.illyriakingdoms.data.KingdomData
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
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
    private const val PLAYERS_TAB = "<green>Players"
    private const val NPCS_TAB = "<aqua>NPCs"
    private const val NO_NPCS_MSG = "<red>There are no NPCs."

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
        val memberItems = buildMemberItems(kingdom, player)
        val npcItems = buildNpcItems(kingdom, player)
        val contentProvider = mutableProvider(memberItems)

        val playersTab =
            item {
                itemProvider by provider { ItemBuilder(Material.PLAYER_HEAD).setName(MM.deserialize(PLAYERS_TAB)) }
<<<<<<< HEAD
                onClick {
                    contentProvider.set(memberItems)
                }
=======
                onClick { contentProvider.set(memberItems) }
>>>>>>> eb5f20ac (Populate NPC tab in member GUI and rename Members tab to Players)
            }

        val npcsTab =
            item {
                itemProvider by provider { ItemBuilder(Material.VILLAGER_SPAWN_EGG).setName(MM.deserialize(NPCS_TAB)) }
                onClick {
                    if (npcItems.isNotEmpty()) {
                        contentProvider.set(npcItems)
                    } else {
                        player.sendActionBar(MM.deserialize(NO_NPCS_MSG))
                    }
                }
            }

        return window(player) {
            title by kingdom.name.append(MM.deserialize(" <gray>Members"))
            upperGui by
                pagedItemsGui(
                    "# # # m # n # # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # < # > # # #",
                ) {
                    '#' by BORDER
                    'x' by Markers.CONTENT_LIST_SLOT_HORIZONTAL
                    'm' by playersTab
                    'n' by npcsTab
                    '<' by back
                    '>' by forward
                    content by contentProvider
                }
        }
    }

    /**
     * Builds member head items with the owner first and the rest sorted alphabetically.
     * Owner is permanently shown with mango gradient lore. Owner-viewing players get right-click kick on members.
     *
     * @param kingdom The kingdom to list members for.
     * @param viewer The player viewing the GUI.
     */
    private fun buildMemberItems(kingdom: KingdomData, viewer: Player): List<Item> {
        val ownerUuid = kingdom.owner
        val ownerName = instance.server.getOfflinePlayer(ownerUuid).name ?: ownerUuid.toString().substring(0, 8)
        val ownerStack = playerHead(
            instance.server.getOfflinePlayer(ownerUuid).playerProfile,
            ownerName,
            "<gradient:#FFE259:#FFA751>Owner"
        )
        val memberItems = kingdom.members
            .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
            .map { memberUuid ->
                val memberName =
                    instance.server.getOfflinePlayer(memberUuid).name ?: memberUuid.toString().substring(0, 8)
                val stack = playerHead(instance.server.getOfflinePlayer(memberUuid).playerProfile, memberName, null)
                if (viewer.uniqueId == ownerUuid) {
                    item {
                        itemProvider by provider { ItemBuilder(stack) }
                        onClick {
                            if (clickType == ClickType.RIGHT) {
                                KingdomData.kickMember(kingdom.owner, memberUuid)
                                viewer.sendActionBar(MM.deserialize("<red>$memberName has been kicked."))
                                viewer.closeInventory()
                                this@MemberGui.open(viewer, kingdom)
                            }
                        }
                    }
                } else {
                    Item.simple(stack)
                }
            }
        return listOf(Item.simple(ownerStack)) + memberItems
    }

    /**
<<<<<<< HEAD
     * Builds NPC head items. Shows players from npcs set (excluding the owner).
=======
     * Builds NPC head items. Owner is excluded from the NPC list. Owner-viewing players can right-click to kick NPCs.
>>>>>>> eb5f20ac (Populate NPC tab in member GUI and rename Members tab to Players)
     *
     * @param kingdom The kingdom to list NPCs for.
     * @param viewer The player viewing the GUI.
     */
<<<<<<< HEAD
    private fun buildNpcItems(kingdom: KingdomData): List<Item> =
        kingdom.npcs
            .filter { it != kingdom.owner }
            .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
            .map {
                val name = instance.server.getOfflinePlayer(it).name ?: it.toString().substring(0, 8)
                playerHead(instance.server.getOfflinePlayer(it).playerProfile, name, "<gradient:#FFE259:#FFA751>NPC")
            }
            .map { Item.simple(it) }
=======
    private fun buildNpcItems(kingdom: KingdomData, viewer: Player): List<Item> =
        kingdom.npcs
            .filter { it != kingdom.owner }
            .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
            .map { npcUuid ->
                val name = instance.server.getOfflinePlayer(npcUuid).name ?: npcUuid.toString().substring(0, 8)
                val stack =
                    playerHead(instance.server.getOfflinePlayer(npcUuid).playerProfile, name, "<gray>NPC")
                if (viewer.uniqueId == kingdom.owner) {
                    item {
                        itemProvider by provider { ItemBuilder(stack) }
                        onClick {
                            if (clickType == ClickType.RIGHT) {
                                KingdomData.kickNpc(kingdom.owner, npcUuid)
                                viewer.sendActionBar(MM.deserialize("<red>$name has been kicked."))
                                viewer.closeInventory()
                                this@MemberGui.open(viewer, kingdom)
                            }
                        }
                    }
                } else {
                    Item.simple(stack)
                }
            }
>>>>>>> eb5f20ac (Populate NPC tab in member GUI and rename Members tab to Players)

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
