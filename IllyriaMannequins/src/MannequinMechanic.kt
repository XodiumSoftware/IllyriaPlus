package org.xodium.illyriamannequins

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.following
import org.xodium.illyriamannequins.MannequinPDC.owner

/** Manages the mannequin and its interactions. */
@Suppress("UnstableApiUsage")
internal object MannequinMechanic : Listener {
    /** The squared distance within which mannequins track players with their heads. */
    private const val TRACKING_RANGE_SQUARED = 64.0

    /** The squared distance at which a following mannequin stops moving toward its owner. */
    private const val FOLLOW_STOP_RANGE_SQUARED = 4.0

    /** The squared distance beyond which a following mannequin teleports to its owner. */
    private const val FOLLOW_TELEPORT_RANGE_SQUARED = 144.0

    /** The movement speed in blocks per tick for following mannequins (player walking speed). */
    private const val FOLLOW_SPEED = 0.21585

    /** The interval in ticks at which mannequin head tracking updates. */
    private const val TRACKING_INTERVAL_TICKS = 2L

    /** Registers the mannequin command and event listeners. */
    fun register() {
        instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands
                    .literal("mannequin")
                    .requires { it.sender is Player && it.sender.isOp }
                    .executes { ctx ->
                        spawnMannequin(ctx.source.sender as Player)
                        Command.SINGLE_SUCCESS
                    }.build(),
                "Spawn a Mannequin at your location",
            )
        }
        instance.server.pluginManager.registerEvents(this, instance)
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinMechanic::updateHeadTracking,
            TRACKING_INTERVAL_TICKS,
            TRACKING_INTERVAL_TICKS,
        )
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinMechanic::updateFollowing,
            TRACKING_INTERVAL_TICKS,
            TRACKING_INTERVAL_TICKS,
        )
    }

    @EventHandler
    fun on(event: PlayerInteractAtEntityEvent) {
        val mannequin = event.rightClicked as? Mannequin ?: return
        if (event.player.uniqueId != mannequin.owner) return
        MannequinEditDialog.show(event.player, mannequin)
    }

    /**
     * Spawns a mannequin at the player's location.
     *
     * @param player The player to spawn the mannequin for.
     */
    private fun spawnMannequin(player: Player) =
        player.world.spawn(player.location, Mannequin::class.java) { it.owner = player.uniqueId }

    /** Makes all mannequins look at the nearest trackable player within range. */
    private fun updateHeadTracking() {
        instance.server.worlds.forEach { world ->
            world.entities
                .filterIsInstance<Mannequin>()
                .forEach { mannequin ->
                    findNearestPlayer(mannequin)?.let { target ->
                        target.eyeLocation.let { eyes ->
                            mannequin.lookAt(eyes.x(), eyes.y(), eyes.z(), LookAnchor.EYES)
                        }
                    }
                }
        }
    }

    /** Makes all following mannequins move toward their owner. */
    private fun updateFollowing() {
        instance.server.worlds.forEach { world ->
            world.entities
                .filterIsInstance<Mannequin>()
                .filter { it.following }
                .forEach { followOwner(it) }
        }
    }

    /**
     * Moves a mannequin toward its owner like a tamed wolf, teleporting when too far away.
     *
     * @param mannequin The mannequin to move.
     */
    private fun followOwner(mannequin: Mannequin) {
        val owner = mannequin.owner?.let { instance.server.getPlayer(it) } ?: return
        if (!visible(owner, mannequin)) return
        if (owner.world != mannequin.world) {
            mannequin.teleport(owner.location)
            return
        }
        val here = mannequin.location
        val distanceSquared = here.distanceSquared(owner.location)
        if (distanceSquared <= FOLLOW_STOP_RANGE_SQUARED) return
        if (distanceSquared > FOLLOW_TELEPORT_RANGE_SQUARED) {
            mannequin.teleport(owner.location)
            return
        }
        val step = owner.location.toVector().subtract(here.toVector()).setY(0)
        if (step.lengthSquared() == 0.0) return
        step.normalize().multiply(FOLLOW_SPEED)
        mannequin.teleport(
            here.clone().add(step).apply {
                yaw = here.yaw
                pitch = here.pitch
            },
        )
    }

    /**
     * Finds the nearest player a mannequin should track with its head.
     *
     * @param mannequin The mannequin to find a target player for.
     * @return The nearest trackable player within range, or `null`.
     */
    private fun findNearestPlayer(mannequin: Mannequin): Player? =
        mannequin.world.players
            .filter {
                visible(it, mannequin) &&
                    it.location.distanceSquared(mannequin.location) <= TRACKING_RANGE_SQUARED
            }.minByOrNull { it.location.distanceSquared(mannequin.location) }

    /**
     * Checks if a player is visible to a mannequin.
     *
     * @param player The player to check.
     * @param mannequin The mannequin doing the tracking.
     * @return `true` if the player is visible to the mannequin.
     */
    private fun visible(
        player: Player,
        mannequin: Entity,
    ): Boolean =
        !player.isDead &&
            player.isOnline &&
            player.uniqueId != mannequin.uniqueId &&
            !player.isInvisible
}
