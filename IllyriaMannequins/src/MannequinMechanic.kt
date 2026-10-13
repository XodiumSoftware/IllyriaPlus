package org.xodium.illyriamannequins

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Location
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.anchor
import org.xodium.illyriamannequins.MannequinPDC.arrows
import org.xodium.illyriamannequins.MannequinPDC.owner
import org.xodium.illyriamannequins.combat.MannequinCoreCombat
import org.xodium.illyriamannequins.combat.MannequinTridentCombat

/** Manages the mannequin and its interactions. */
@Suppress("UnstableApiUsage")
internal object MannequinMechanic : Listener {
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
    fun on(event: EntityDeathEvent) {
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

    @EventHandler
    fun on(event: PlayerInteractAtEntityEvent) {
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
}
