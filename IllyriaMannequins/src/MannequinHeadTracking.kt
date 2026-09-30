package org.xodium.illyriamannequins

import io.papermc.paper.entity.LookAnchor
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance

/** Makes mannequins track the nearest player with their heads. */
@Suppress("UnstableApiUsage")
internal object MannequinHeadTracking {
    /** The squared distance within which mannequins track players with their heads. */
    private const val TRACKING_RANGE_SQUARED = 64.0

    /** The interval in ticks at which mannequin head tracking updates. */
    private const val TRACKING_INTERVAL_TICKS = 2L

    /** Registers the head tracking task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinHeadTracking::updateHeadTracking,
            TRACKING_INTERVAL_TICKS,
            TRACKING_INTERVAL_TICKS,
        )
    }

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
