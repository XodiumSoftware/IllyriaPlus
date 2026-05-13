@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.world

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriaplus.Utils.BlockUtils.DANGEROUS
import org.xodium.illyriaplus.Utils.BlockUtils.center
import org.xodium.illyriaplus.Utils.ItemUtils.getCustomName
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.ScheduleUtils.schedule
import org.xodium.illyriaplus.data.AnchorData
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.managers.XpManager
import org.xodium.illyriaplus.tables.AnchorTable
import xyz.xenondevs.invui.gui.Animation
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window
import kotlin.math.cos
import kotlin.math.sin
import kotlin.uuid.ExperimentalUuidApi

/** Represents a mechanic handling teleportation within the system. */
@OptIn(ExperimentalUuidApi::class)
internal object TeleportMechanic : MechanicInterface {
    /** Holds user-facing MiniMessage strings for teleport anchor interactions. */
    private object Messages {
        const val ANCHOR_CREATION =
            "<gradient:#43A047:#66BB6A><b>You have created a Teleportation Anchor!</b></gradient>"
        const val ANCHOR_REMOVAL =
            "<gradient:#CB2D3E:#EF473A><b>You have removed a Teleportation Anchor!</b></gradient>"
        const val ANCHOR_NAME_UPDATE =
            "<gradient:#FFE259:#FFA751><b>You have successfully changed the Anchor's name!</b></gradient>"
        const val TELEPORT_TITLE =
            "<gradient:#FFE259:#FFA751><b>Teleporting...</b></gradient>"
        const val TELEPORT_SUBTITLE =
            "<gradient:#CB2D3E:#EF473A><b><remaining></b></gradient>"
        const val TELEPORT_CANCELLED =
            "<gradient:#CB2D3E:#EF473A><b>Teleportation cancelled — you moved!</b></gradient>"
        const val TELEPORT_UNSAFE =
            "<gradient:#CB2D3E:#EF473A><b>Teleportation cancelled — destination is unsafe!</b></gradient>"
    }

    /** Holds [Sound] instances for teleport anchor interactions. */
    private object Sounds {
        val COUNTDOWN_TICK: Sound =
            Sound.sound(Key.key("ui.button.click"), Sound.Source.PLAYER, 1.0f, 1.0f)
        val TELEPORT_CANCEL: Sound =
            Sound.sound(Key.key("block.beacon.deactivate"), Sound.Source.PLAYER, 1.0f, 1.0f)
        val ENDERMAN_TELEPORT: Sound =
            Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.PLAYER, 1.0f, 1.0f)
    }

    private val TELEPORTING = mutableSetOf<Player>()
    private val PAGE_CHANGE_ANIMATION =
        Animation
            .builder()
            .setSlotSelector(Animation::horizontalSnakeSlotSelector)
            .filterTaggedSlots('x')
            .setFreezing(false)
            .build()
    private val BACK_BUTTON =
        BoundItem
            .pagedBuilder()
            .setItemProvider(ItemBuilder(Material.ARROW).hideTooltip(true))
            .addClickHandler { _, gui, _ ->
                gui.cancelAnimation()
                gui.page--
                gui.playAnimation(PAGE_CHANGE_ANIMATION)
            }.build()
    private val FORWARD_BUTTON =
        BoundItem
            .pagedBuilder()
            .setItemProvider(ItemBuilder(Material.ARROW).hideTooltip(true))
            .addClickHandler { _, gui, _ ->
                gui.cancelAnimation()
                gui.page++
                gui.playAnimation(PAGE_CHANGE_ANIMATION)
            }.build()

    @EventHandler
    fun on(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val block = event.clickedBlock ?: return

        if (block.world.environment != World.Environment.NORMAL) return

        val anchor = AnchorTable.findByLocation(block.location) ?: return

        when {
            event.action != Action.RIGHT_CLICK_BLOCK -> {
                return
            }

            event.clickedBlock?.type != Material.LODESTONE -> {
                return
            }

            event.item?.type == Material.COMPASS -> {
                return
            }

            event.item?.type == Material.NAME_TAG -> {
                rename(event)
                return
            }

            else -> {
                playAnchorFlame(anchor.location, scale = 1.0f)
                window(anchor, event.player).open(event.player)
            }
        }
    }

    @EventHandler
    fun on(event: BlockPlaceEvent) {
        val block = event.blockPlaced

        if (block.world.environment != World.Environment.NORMAL) return
        if (block.type != Material.LODESTONE) return

        val location = block.location

        if (AnchorTable.findByLocation(location) != null) return

        AnchorTable.insert(location)
        event.player.sendActionBar(MM.deserialize(Messages.ANCHOR_CREATION))
    }

    @EventHandler
    fun on(event: BlockBreakEvent) {
        val block = event.block

        if (block.world.environment != World.Environment.NORMAL) return
        if (block.type != Material.LODESTONE) return
        if (!AnchorTable.deleteByLocation(block.location)) return

        event.player.sendActionBar(MM.deserialize(Messages.ANCHOR_REMOVAL))
    }

    /**
     * Builds a paginated GUI showing all teleport anchors except the one at [source].
     *
     * @param source The [AnchorData] of the anchor currently being interacted with; it is omitted from the list.
     * @param player The [Player] opening the GUI, used to calculate per-player teleport costs.
     * @return The configured [PagedGui] for anchor selection.
     */
    private fun gui(
        source: AnchorData,
        player: Player,
    ) = PagedGui
        .itemsBuilder()
        .setStructure(
            "# # # # # # # # #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# # # < # > # # #",
        ).addIngredient('#', Item.simple(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).hideTooltip(true)))
        .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
        .addIngredient('<', BACK_BUTTON)
        .addIngredient('>', FORWARD_BUTTON)
        .setContent(
            AnchorTable
                .all()
                .filterNot { it.matches(source.location) }
                .map { anchorItem(source, it, player) },
        ).build()

    /**
     * Creates a window for the teleport destination selector GUI.
     *
     * @param source The [AnchorData] of the anchor being interacted with.
     * @param player The [Player] opening the GUI, used to calculate per-player teleport costs.
     * @return The configured [Window] builder.
     */
    private fun window(
        source: AnchorData,
        player: Player,
    ) = Window
        .builder()
        .setTitle(MM.deserialize(source.name))
        .setUpperGui(gui(source, player))

    /**
     * Creates a GUI item representing a teleport anchor.
     *
     * @param source The [AnchorData] the player is teleporting from.
     * @param anchor The [AnchorData] to represent.
     * @param player The [Player] opening the GUI, used to calculate and display teleport cost.
     * @return The constructed [Item] for the GUI.
     */
    @Suppress("UnstableApiUsage")
    private fun anchorItem(
        source: AnchorData,
        anchor: AnchorData,
        player: Player,
    ): Item =
        Item
            .builder()
            .setItemProvider(
                ItemStack
                    .of(Material.LODESTONE)
                    .apply {
                        val cost = calculateCost(source.location, anchor.location, player)
                        setData(DataComponentTypes.ITEM_NAME, MM.deserialize(anchor.name))
                        setData(
                            DataComponentTypes.LORE,
                            ItemLore.lore(
                                listOf(
                                    Component.empty(),
                                    MM.deserialize(
                                        "Location: ${anchor.location.x}, ${anchor.location.y}, ${anchor.location.z}",
                                    ),
                                    MM.deserialize(
                                        "<gray>Cost: <cost> XP</gray>".replace("<cost>", cost.toString()),
                                    ),
                                ),
                            ),
                        )
                    },
            ).addClickHandler { _, click ->
                val cost = calculateCost(source.location, anchor.location, click.player)

                handleTeleport(click.player, source.location, anchor.location, cost)
            }.build()

    /**
     * Calculates the XP cost for teleporting from [source] to [anchor].
     *
     * Base cost is 1 XP per block of distance.
     * Modifiers:
     * - Mount: +50%
     * - Each leashed entity: +25%
     *
     * @param source The [Location] of the source anchor.
     * @param anchor The [Location] of the destination anchor.
     * @param player The [Player] to check for mount and leashed entities.
     * @return The total XP cost.
     */
    private fun calculateCost(
        source: Location,
        anchor: Location,
        player: Player,
    ): Int {
        val distance = source.distance(anchor)

        var cost = distance.toInt().coerceAtLeast(1)

        if (player.vehicle != null) cost = (cost * 1.5).toInt()

        return cost
    }

    /**
     * Handles teleporting a player to an anchor after a 3-second countdown.
     *
     * @param player The [Player] to teleport.
     * @param source The [Location] the player is teleporting from.
     * @param anchor The [Location] destination.
     * @param cost The XP cost to deduct on teleport.
     */
    private fun handleTeleport(
        player: Player,
        source: Location,
        anchor: Location,
        cost: Int,
    ) {
        if (player in TELEPORTING) return
        if (!XpManager.canAfford(player, cost)) return

        TELEPORTING.add(player)
        player.closeInventory()

        lateinit var task: BukkitTask
        var remaining = 3
        val distance = source.distance(anchor)
        val initialLocation = player.location.clone()

        task =
            schedule(period = 20L) {
                if (remaining > 0) {
                    if (player.location.distance(initialLocation) > 0.5) {
                        player.sendActionBar(MM.deserialize(Messages.TELEPORT_CANCELLED))
                        player.playSound(Sounds.TELEPORT_CANCEL)
                        TELEPORTING.remove(player)
                        task.cancel()
                        return@schedule
                    }

                    val progress = (3 - remaining) / 3.0f
                    val scale = 1.0f + (3 - remaining) * 0.5f

                    playAnchorFlame(source, scale)
                    playAnchorFlame(anchor, scale)
                    playExpansionEffect(source, distance, progress)
                    playExpansionEffect(anchor, distance, progress)
                    playCloudEffect(source, progress)
                    playCloudEffect(anchor, progress)
                    player.showTitle(
                        Title.title(
                            MM.deserialize(Messages.TELEPORT_TITLE),
                            MM.deserialize(
                                Messages.TELEPORT_SUBTITLE.replace("<remaining>", remaining.toString()),
                            ),
                        ),
                    )
                    player.playSound(Sounds.COUNTDOWN_TICK)
                    remaining--
                } else {
                    val world = anchor.world
                    val bx = anchor.blockX
                    val by = anchor.blockY
                    val bz = anchor.blockZ
                    val hasMount = player.vehicle != null

                    if (hasMount) {
                        if (!isSafeForMount(world, bx, by, bz)) {
                            player.sendActionBar(MM.deserialize(Messages.TELEPORT_UNSAFE))
                            player.playSound(Sounds.TELEPORT_CANCEL)
                            TELEPORTING.remove(player)
                            task.cancel()
                            return@schedule
                        }
                    } else {
                        if (!isSafeForPlayer(world, bx, by, bz)) {
                            player.sendActionBar(MM.deserialize(Messages.TELEPORT_UNSAFE))
                            player.playSound(Sounds.TELEPORT_CANCEL)
                            TELEPORTING.remove(player)
                            task.cancel()
                            return@schedule
                        }
                    }

                    val destination = anchor.clone().add(0.5, 1.0, 0.5)

                    playLightningEffect(player.location)

                    val mount = player.vehicle

                    if (mount != null) {
                        mount.teleport(destination)
                        player.teleport(destination)
                        mount.addPassenger(player)
                    } else {
                        player.teleport(destination)
                    }

                    if (player.gameMode != GameMode.CREATIVE) player.giveExp(-cost)

                    player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false))

                    playTeleportEffects(player, destination)
                    playLightningEffect(destination)
                    TELEPORTING.remove(player)
                    task.cancel()
                }
            }
    }

    /**
     * Plays an enderman teleport sound and portal particles at the given location.
     *
     * @param player The [Player] context for the world.
     * @param location The [Location] where the effects should play.
     */
    private fun playTeleportEffects(
        player: Player,
        location: Location,
    ) {
        player.world.players
            .filter { it.location.distance(location) <= 32 }
            .forEach { it.playSound(Sounds.ENDERMAN_TELEPORT) }
        player.world.spawnParticle(
            Particle.PORTAL,
            location.clone().add(0.0, 1.0, 0.0),
            50,
            0.5,
            1.0,
            0.5,
            0.1,
        )
    }

    /**
     * Spawns purple flame-like particles above the specified anchor.
     *
     * @param location The [Location] to spawn particles above.
     * @param scale The scale multiplier for particle spread and count.
     */
    private fun playAnchorFlame(
        location: Location,
        scale: Float = 1.0f,
    ) {
        location.world.spawnParticle(
            Particle.DUST,
            location.clone().add(0.5, 1.2, 0.5),
            (15 * scale).toInt(),
            0.15 * scale,
            0.3 * scale,
            0.15 * scale,
            0.0,
            Particle.DustOptions(Color.fromRGB(147, 51, 255), scale.coerceAtLeast(1.0f)),
        )
    }

    /**
     * Spawns an expanding horizontal ring of purple particles from the anchor outward.
     *
     * @param location The [Location] center of the expansion.
     * @param maxDistance The maximum distance the ring should expand to.
     * @param progress A float from 0.0 to 1.0 representing how far the ring has expanded.
     */
    private fun playExpansionEffect(
        location: Location,
        maxDistance: Double,
        progress: Float,
    ) {
        val center = location.clone().add(0.5, 1.0, 0.5)
        val radius = (maxDistance * progress).coerceAtLeast(0.1)
        val points = (20 + 20 * progress).toInt()

        for (i in 0 until points) {
            val angle = 2 * Math.PI * i / points
            val x = center.x + radius * cos(angle)
            val z = center.z + radius * sin(angle)
            val loc = Location(location.world, x, center.y, z)

            location.world.spawnParticle(
                Particle.DUST,
                loc,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(Color.fromRGB(147, 51, 255), 0.8f),
            )
        }
    }

    /**
     * Spawns an expanding ring of cloud and crit particles at the given location.
     *
     * @param location The [Location] to center the effect on.
     * @param progress A float from 0.0 to 1.0 representing how far the ring has expanded.
     */
    private fun playCloudEffect(
        location: Location,
        progress: Float,
    ) {
        val maxRadius = 4.0
        val radius = maxRadius * progress

        if (radius < 0.1) return

        val world = location.world
        val particleCount = (radius * 16).toInt().coerceAtLeast(8)

        for (i in 0 until particleCount) {
            val angle = 2 * Math.PI * i / particleCount
            val x = location.x + radius * cos(angle)
            val z = location.z + radius * sin(angle)
            val y = location.y + 0.1
            val particleLoc = Location(world, x, y, z)

            world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.05, 0.0, 0.05, 0.01)
            world.spawnParticle(Particle.CRIT, particleLoc.add(0.0, 0.2, 0.0), 1, 0.05, 0.05, 0.05, 0.0)
        }
    }

    /**
     * Strikes a visual-only lightning bolt at the given location.
     *
     * @param location The [Location] to strike lightning at.
     */
    private fun playLightningEffect(location: Location) {
        location.world.strikeLightningEffect(location.block.center())
    }

    /**
     * Checks if the blocks directly above the anchor are safe for a player.
     */
    private fun isSafeForPlayer(
        world: World,
        bx: Int,
        by: Int,
        bz: Int,
    ): Boolean {
        val feet = world.getBlockAt(bx, by + 1, bz)
        val head = world.getBlockAt(bx, by + 2, bz)

        return !feet.type.isSolid && !head.type.isSolid && feet.type !in DANGEROUS && head.type !in DANGEROUS
    }

    /**
     * Checks if a 3x3x3 area above the anchor is clear of solid and dangerous blocks.
     * Mounts are wider and taller than players, so they need more clearance.
     */
    private fun isSafeForMount(
        world: World,
        bx: Int,
        by: Int,
        bz: Int,
    ): Boolean {
        for (y in (by + 1)..(by + 3)) {
            for (dx in -1..1) {
                for (dz in -1..1) {
                    val block = world.getBlockAt(bx + dx, y, bz + dz)

                    if (block.type.isSolid || block.type in DANGEROUS) return false
                }
            }
        }
        return true
    }

    /**
     * Renames a teleport anchor using the custom name from a name tag.
     *
     * @param event The [PlayerInteractEvent] triggered when a player interacts with a lodestone while holding a name tag.
     */
    private fun rename(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val item = event.item ?: return
        val anchor = AnchorTable.findByLocation(block.location) ?: return
        val newName = item.getCustomName() ?: return

        AnchorTable.updateName(anchor.uuid, newName)
        event.player.sendActionBar(MM.deserialize(Messages.ANCHOR_NAME_UPDATE))

        item.amount--

        if (item.amount <= 0) event.player.inventory.setItemInMainHand(null)

        event.isCancelled = true
    }
}
