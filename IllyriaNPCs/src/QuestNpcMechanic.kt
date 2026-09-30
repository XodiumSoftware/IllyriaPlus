package org.xodium.illyrianpcs

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.MainHand
import org.xodium.illyrianpcs.IllyriaNPCs.Companion.instance

/** Manages the quest mannequin NPC and its interactions. */
@Suppress("UnstableApiUsage")
internal object QuestNpcMechanic : Listener {
    private val NPC_NAME: Component = Component.text("Quest Giver")

    /** Registers the quest command and event listeners. */
    fun register() {
        instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands
                    .literal("quests")
                    .then(
                        Commands
                            .literal("npc")
                            .requires { it.sender is Player }
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                spawnNpc(player)
                                Command.SINGLE_SUCCESS
                            },
                    ).build(),
                "Quest management",
                listOf("q"),
            )
        }
        instance.server.pluginManager.registerEvents(this, instance)
    }

    @EventHandler
    fun on(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked as? Mannequin ?: return
        // TODO: Open quest GUI or dialogue
        instance.logger.info("${event.player.name} interacted with quest NPC")
    }

    /**
     * Spawns a quest NPC mannequin at the player's location.
     *
     * @param player The player to spawn the NPC for.
     */
    private fun spawnNpc(player: Player) {
        player.world.spawn(player.location, Mannequin::class.java) { npc ->
            npc.customName(NPC_NAME)
            npc.isCustomNameVisible = true
            npc.isImmovable = true
            npc.mainHand = MainHand.RIGHT
            npc.setGravity(false)
        }
    }
}
