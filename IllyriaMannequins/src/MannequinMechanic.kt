package org.xodium.illyriamannequins

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.anchor
import org.xodium.illyriamannequins.MannequinPDC.arrows
import org.xodium.illyriamannequins.MannequinPDC.owner
import org.xodium.illyriamannequins.combat.MannequinCoreCombat
import org.xodium.illyriamannequins.combat.MannequinTridentCombat
import kotlin.random.Random

/** Manages the mannequin and its interactions. */
@Suppress("UnstableApiUsage")
internal object MannequinMechanic : Listener {
    /** The chance that carving a villager with an amethyst shard turns it into a mannequin. */
    private const val CONVERSION_CHANCE = 0.33

    /** The action bar message shown when a villager is successfully converted. */
    private const val CONVERSION_SUCCESS = "<green>The villager stiffens into a mannequin!"

    /** The action bar message shown when the conversion roll fails. */
    private const val CONVERSION_FAILURE = "<red>The shard shatters. The villager is unmoved."

    /** Registers the mannequin command and event listeners. */
    fun register() {
        instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands
                    .literal("mannequins")
                    .requires { it.sender is Player }
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        MannequinListUI.open(player, player)
                        Command.SINGLE_SUCCESS
                    }.then(
                        Commands
                            .argument("player", ArgumentTypes.player())
                            .requires { it.sender.isOp }
                            .executes { ctx ->
                                val viewer = ctx.source.sender as Player
                                val target =
                                    ctx
                                        .getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                        .resolve(ctx.source)
                                        .singleOrNull()
                                        ?: return@executes Command.SINGLE_SUCCESS
                                MannequinListUI.open(viewer, target)
                                Command.SINGLE_SUCCESS
                            },
                    ).build(),
                "List all of your mannequins",
            )
        }
        instance.server.pluginManager.registerEvents(this, instance)
        MannequinTicker.register()
        MannequinCoreCombat.register()
        MannequinTridentCombat.register()
    }

    @EventHandler
    fun on(event: EntityDeathEvent) = dropEquipment(event)

    @EventHandler
    fun on(event: PlayerInteractEntityEvent) = convertVillager(event)

    @EventHandler
    fun on(event: PlayerInteractAtEntityEvent) = openMenu(event)

    /**
     * Spawns a mannequin at the given location, owned by the given player.
     *
     * @param location The location to spawn the mannequin at.
     * @param owner The player to own the spawned mannequin.
     */
    fun spawnMannequin(
        location: Location,
        owner: Player,
    ) = location.world.spawn(location, Mannequin::class.java) {
        it.owner = owner.uniqueId
        it.anchor = it.location
        it.isPersistent = true
        it.setAI(true)
    }

    /**
     * Drops everything a dying mannequin was wearing and holding, plus its stored arrows.
     *
     * @param event The death event of the dying entity.
     */
    private fun dropEquipment(event: EntityDeathEvent) {
        val mannequin = event.entity as? Mannequin ?: return
        val equipment = mannequin.equipment
        event.drops.addAll(
            listOfNotNull(
                equipment.helmet,
                equipment.chestplate,
                equipment.leggings,
                equipment.boots,
                equipment.itemInMainHand,
                equipment.itemInOffHand,
                mannequin.arrows,
            ).filterNot { it.isEmpty },
        )
    }

    /**
     * Turns a villager into a mannequin when the player sneaks and right-clicks it with an
     * amethyst shard, consuming the shard regardless of the outcome.
     *
     * @param event The interaction event, whose target must be a [Villager].
     */
    private fun convertVillager(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val villager = event.rightClicked as? Villager ?: return
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.AMETHYST_SHARD || !player.isSneaking) return
        event.isCancelled = true
        if (isSpawnProtected(villager)) return
        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
            if (item.amount <= 0) player.inventory.setItemInMainHand(null)
        }
        if (Random.nextDouble() >= CONVERSION_CHANCE) {
            villager.world.playSound(villager.location, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 0.5f)
            player.sendActionBar(MM.deserialize(CONVERSION_FAILURE))
            return
        }
        val location = villager.location
        val name = villager.customName()
        villager.remove()
        spawnMannequin(location, player).also { mannequin ->
            name?.let {
                mannequin.customName(it)
                mannequin.isCustomNameVisible = true
            }
        }
        location.world.playSound(location, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f)
        player.sendActionBar(MM.deserialize(CONVERSION_SUCCESS))
    }

    /**
     * Opens the equipment GUI or edit dialog for a mannequin the player owns, depending on
     * whether the player is sneaking.
     *
     * @param event The interaction event, whose target must be a [Mannequin].
     */
    private fun openMenu(event: PlayerInteractAtEntityEvent) {
        val mannequin = event.rightClicked as? Mannequin ?: return
        if (event.player.uniqueId != mannequin.owner) return
        event.isCancelled = true
        if (event.player.isSneaking) {
            MannequinEquipmentUI.open(event.player, mannequin)
            return
        }
        MannequinEditDialog.show(event.player, mannequin)
    }

    /**
     * Checks if an entity is standing inside the world's spawn protection radius.
     *
     * @param entity The entity to check.
     * @return `true` if the entity lies within the protected spawn area.
     */
    private fun isSpawnProtected(entity: LivingEntity): Boolean {
        val radius = instance.server.spawnRadius
        if (radius <= 0) return false
        return entity.world.spawnLocation.distanceSquared(entity.location) < radius * radius
    }
}
