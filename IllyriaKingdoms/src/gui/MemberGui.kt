package org.xodium.illyriakingdoms.gui

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.Utils.MM
import org.xodium.illyriakingdoms.data.DeadNpcData
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
import java.util.UUID

/** Builds and opens the kingdom members GUI. */
@OptIn(ExperimentalDslApi::class)
internal object MemberGui {
    private const val PREVIOUS_PAGE_NAME = "<gray>Previous page"
    private const val NEXT_PAGE_NAME = "<gray>Next page"
    private const val PLAYERS_TAB = "<green>Players"
    private const val NPCS_TAB = "<aqua>NPCs"
    private const val NO_NPCS_MSG = "<red>There are no NPCs."
    private const val CALL_HINT = "<gray>Left click to call"
    private const val KICK_HINT = "<gray>Right click to kick"
    private const val RESURRECT_HINT = "<yellow>Left click to resurrect <gray>(32 emeralds)"
    private const val OWNER_LORE = "<mango>Owner"
    private const val RESURRECT_COST = 32
    private const val CALL_INTERVAL = 20L
    private const val CALL_MAX_TICKS = 300L
    private const val CALL_ARRIVE_RANGE = 2.0

    private val BORDER = Item.simple(ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).hideTooltip(true))

    private val callTasks = mutableMapOf<UUID, BukkitTask>()

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
    fun open(
        player: Player,
        kingdom: KingdomData,
    ) {
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
                onClick { contentProvider.set(memberItems) }
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
    private fun buildMemberItems(
        kingdom: KingdomData,
        viewer: Player,
    ): List<Item> {
        val ownerUuid = kingdom.owner
        val ownerName = instance.server.getOfflinePlayer(ownerUuid).name ?: ownerUuid.toString().substring(0, 8)
        val ownerStack =
            playerHead(
                instance.server.getOfflinePlayer(ownerUuid).playerProfile,
                ownerName,
                listOf(OWNER_LORE),
            )
        val isOwner = viewer.uniqueId == ownerUuid
        val memberItems =
            kingdom
                .members
                .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
                .map { memberUuid ->
                    val memberName =
                        instance.server.getOfflinePlayer(memberUuid).name ?: memberUuid.toString().substring(0, 8)
                    val stack =
                        playerHead(
                            instance.server.getOfflinePlayer(memberUuid).playerProfile,
                            memberName,
                            if (isOwner) listOf(CALL_HINT, KICK_HINT) else null,
                        )
                    if (isOwner) {
                        item {
                            itemProvider by provider { ItemBuilder(stack) }
                            onClick {
                                val kingdomName = MM.serialize(kingdom.name)
                                if (clickType == ClickType.RIGHT) {
                                    KingdomData.kickMember(kingdom.owner, memberUuid)
                                    instance.server.broadcast(
                                        MM.deserialize(
                                            "<firewatch>[$kingdomName]</gradient> <red>$memberName has been kicked.",
                                        ),
                                    )
                                    this@MemberGui.open(viewer, kingdom)
                                } else if (clickType == ClickType.LEFT) {
                                    val target = instance.server.getPlayer(memberUuid)
                                    if (target != null) {
                                        target.showTitle(
                                            Title.title(
                                                kingdom.name,
                                                MM
                                                    .deserialize("<firewatch>The King has called upon you")
                                                    .decoration(TextDecoration.ITALIC, false),
                                            ),
                                        )
                                        instance.server.broadcast(
                                            MM.deserialize(
                                                "<firewatch>[$kingdomName]</gradient> " +
                                                    "<green>$memberName has been notified.",
                                            ),
                                        )
                                    }
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
     * Builds NPC head items. Owner is excluded from the NPC list. Owner-viewing players can right-click to kick NPCs.
     *
     * @param kingdom The kingdom to list NPCs for.
     * @param viewer The player viewing the GUI.
     */
    private fun buildNpcItems(
        kingdom: KingdomData,
        viewer: Player,
    ): List<Item> {
        val isOwner = viewer.uniqueId == kingdom.owner
        return kingdom
            .npcs
            .filter { it != kingdom.owner }
            .sortedBy { instance.server.getOfflinePlayer(it).name ?: "" }
            .map { npcUuid ->
                val isDead = npcUuid in DeadNpcData.registry
                val entity = instance.server.getEntity(npcUuid)
                val name = entity?.name ?: npcUuid.toString().substring(0, 8)
                val stack =
                    if (isDead) {
                        npcSkull(name, if (isOwner) listOf(RESURRECT_HINT) else null)
                    } else {
                        npcHead(name, if (isOwner) listOf(CALL_HINT, KICK_HINT) else null)
                    }
                if (isOwner) {
                    item {
                        itemProvider by provider { ItemBuilder(stack) }
                        onClick {
                            when (clickType) {
                                ClickType.LEFT ->
                                    if (isDead) {
                                        resurrectNpc(kingdom, npcUuid, name, viewer)
                                    } else {
                                        callNpc(kingdom, npcUuid, name, viewer)
                                    }

                                ClickType.RIGHT -> kickNpc(kingdom, npcUuid, name, viewer)
                                else -> {}
                            }
                        }
                    }
                } else {
                    Item.simple(stack)
                }
            }
    }

    /**
     * Commands a villager NPC to pathfind toward the kingdom owner.
     * The villager re-pathfinds every [CALL_INTERVAL] ticks to track a moving
     * player until it arrives within [CALL_ARRIVE_RANGE] blocks or [CALL_MAX_TICKS] expires.
     *
     * @param kingdom the kingdom the NPC belongs to.
     * @param npcUuid the UUID of the NPC villager.
     * @param name the display name of the NPC.
     * @param viewer the player who called the NPC.
     */
    private fun callNpc(
        kingdom: KingdomData,
        npcUuid: UUID,
        name: String,
        viewer: Player,
    ) {
        val owner = instance.server.getPlayer(kingdom.owner)
        val entity = instance.server.getEntity(npcUuid)

        if (owner == null || !owner.isOnline) return

        val villager = entity as? Mob ?: return

        callTasks.remove(npcUuid)?.cancel()

        villager.pathfinder.moveTo(owner)
        var ticksRemaining = CALL_MAX_TICKS

        viewer.sendActionBar(
            MM.deserialize("<green>$name is on its way."),
        )

        callTasks[npcUuid] =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (!villager.isValid || !owner.isOnline || ticksRemaining <= 0) {
                        callTasks.remove(npcUuid)?.cancel()
                        return@Runnable
                    }
                    if (villager.location.distanceSquared(owner.location) <= CALL_ARRIVE_RANGE * CALL_ARRIVE_RANGE) {
                        callTasks.remove(npcUuid)?.cancel()
                        return@Runnable
                    }
                    villager.pathfinder.moveTo(owner)
                    ticksRemaining -= CALL_INTERVAL
                },
                0L,
                CALL_INTERVAL,
            )
    }

    /** Cancels all active call tasks. */
    fun cancelCallTasks() {
        callTasks.values.forEach { it.cancel() }
        callTasks.clear()
    }

    /**
     * Kicks an NPC from a kingdom.
     *
     * @param kingdom the kingdom to kick the NPC from.
     * @param npcUuid the UUID of the NPC to kick.
     * @param name the display name of the NPC.
     * @param viewer the player viewing the GUI (used to close/reopen).
     */
    private fun kickNpc(
        kingdom: KingdomData,
        npcUuid: UUID,
        name: String,
        viewer: Player,
    ) {
        KingdomData.kickNpc(kingdom.owner, npcUuid)
        DeadNpcData.registry.remove(npcUuid)
        DeadNpcData.delete(npcUuid)
        instance.server.broadcast(
            MM.deserialize(
                "<firewatch>[${
                    MM.serialize(kingdom.name)
                }]</gradient> <red>$name has been kicked.",
            ),
        )
        this@MemberGui.open(viewer, kingdom)
    }

    /**
     * Resurrects a dead villager NPC at its death location, consuming emeralds from the owner's inventory.
     *
     * @param kingdom the kingdom the NPC belongs to.
     * @param npcUuid the UUID of the dead NPC.
     * @param name the display name of the NPC.
     * @param viewer the player viewing the GUI (used to consume emeralds and reopen the GUI).
     */
    private fun resurrectNpc(
        kingdom: KingdomData,
        npcUuid: UUID,
        name: String,
        viewer: Player,
    ) {
        val deadData = DeadNpcData.registry[npcUuid]
        val kingdomName = MM.serialize(kingdom.name)
        val emerald = ItemStack.of(Material.EMERALD, RESURRECT_COST)

        if (deadData == null) return
        if (!viewer.inventory.containsAtLeast(emerald, RESURRECT_COST)) {
            viewer.sendActionBar(
                MM.deserialize("<red>You need $RESURRECT_COST emeralds to resurrect $name."),
            )
            return
        }

        viewer.inventory.removeItem(emerald)
        DeadNpcData.registry.remove(npcUuid)
        DeadNpcData.delete(npcUuid)

        deadData.location.world.spawnEntity(deadData.location, EntityType.VILLAGER).let { entity ->
            val villager = entity as Villager
            villager.profession = deadData.profession
            villager.villagerType = deadData.type
            villager.villagerLevel = deadData.level
            villager.customName(MM.deserialize(name))
            KingdomData.kickNpc(kingdom.owner, npcUuid)
            KingdomData.addNpc(kingdom.owner, villager.uniqueId)
        }

        instance.server.broadcast(
            MM.deserialize("<firewatch>[$kingdomName]</gradient> <green>$name has been resurrected!"),
        )
        this@MemberGui.open(viewer, kingdom)
    }

    /**
     * Creates a villager-themed [ItemStack] for dead NPCs.
     *
     * @param name the display name for the NPC.
     * @param lore optional list of MiniMessage lore lines to display under the name.
     * @return the configured ItemStack.
     */
    private fun npcSkull(
        name: String,
        lore: List<String>?,
    ): ItemStack =
        ItemStack.of(Material.SKELETON_SKULL).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<reset><red>$name"))
            if (lore != null) {
                setData(
                    DataComponentTypes.LORE,
                    ItemLore.lore(lore.map { MM.deserialize(it).decoration(TextDecoration.ITALIC, false) }),
                )
            }
        }

    /**
     * Creates a villager-themed [ItemStack] for NPCs.
     *
     * @param name the display name for the NPC.
     * @param lore optional list of MiniMessage lore lines to display under the name.
     * @return the configured ItemStack.
     */
    private fun npcHead(
        name: String,
        lore: List<String>?,
    ): ItemStack =
        ItemStack.of(Material.VILLAGER_SPAWN_EGG).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<reset>$name"))
            if (lore != null) {
                setData(
                    DataComponentTypes.LORE,
                    ItemLore.lore(lore.map { MM.deserialize(it).decoration(TextDecoration.ITALIC, false) }),
                )
            }
        }

    /**
     * Creates a player head item stack with the given profile, display name, and optional lore lines.
     *
     * @param profile the player profile to use for the skin.
     * @param name the display name for the head.
     * @param lore optional list of MiniMessage lore lines to display under the name.
     * @return the configured player head ItemStack.
     */
    private fun playerHead(
        profile: PlayerProfile,
        name: String,
        lore: List<String>?,
    ): ItemStack =
        ItemStack.of(Material.PLAYER_HEAD).apply {
            setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile))
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<reset>$name"))
            if (lore != null) {
                setData(
                    DataComponentTypes.LORE,
                    ItemLore.lore(lore.map { MM.deserialize(it).decoration(TextDecoration.ITALIC, false) }),
                )
            }
        }
}
