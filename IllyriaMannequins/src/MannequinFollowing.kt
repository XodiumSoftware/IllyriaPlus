package org.xodium.illyriamannequins

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.following
import org.xodium.illyriamannequins.MannequinPDC.owner

/** Makes mannequins follow their owner like a tamed wolf. */
@Suppress("UnstableApiUsage")
internal object MannequinFollowing {
    /** The squared distance at which a following mannequin stops moving toward its owner. */
    private const val FOLLOW_STOP_RANGE_SQUARED = 4.0

    /** The squared distance beyond which a following mannequin teleports to its owner. */
    private const val FOLLOW_TELEPORT_RANGE_SQUARED = 144.0

    /** The movement speed in blocks per tick for following mannequins (player walking speed). */
    private const val FOLLOW_SPEED = 0.21585

    /** The interval in ticks at which mannequin following updates. */
    private const val FOLLOW_INTERVAL_TICKS = 2L

    /** Registers the following task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinFollowing::updateFollowing,
            FOLLOW_INTERVAL_TICKS,
            FOLLOW_INTERVAL_TICKS,
        )
    }

    /** Makes all following mannequins move toward their owner. */
    private fun updateFollowing() {
        instance.server.worlds.forEach { world ->
            world
                .entities
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
            mannequin.teleport(owner.groundLocation())
            return
        }
        val here = mannequin.location
        val distanceSquared = here.distanceSquared(owner.location)
        if (distanceSquared <= FOLLOW_STOP_RANGE_SQUARED) return
        if (distanceSquared > FOLLOW_TELEPORT_RANGE_SQUARED) {
            mannequin.teleport(owner.groundLocation())
            return
        }
        val step =
            owner
                .location
                .toVector()
                .subtract(here.toVector())
                .setY(0)
        if (step.lengthSquared() == 0.0) return
        step.normalize().multiply(FOLLOW_SPEED)
        mannequin.isJumping = blockedAhead(mannequin, step)
        mannequin.velocity = step.apply { y = mannequin.velocity.y }
    }

    /**
     * Checks if a mannequin's next horizontal step is blocked by a solid block.
     *
     * @param mannequin The mannequin to check.
     * @param step The horizontal movement step.
     * @return `true` if a solid block blocks the step.
     */
    private fun blockedAhead(
        mannequin: Mannequin,
        step: Vector,
    ): Boolean =
        mannequin
            .location
            .clone()
            .add(step.clone().normalize().multiply(0.6))
            .block
            .type
            .isSolid

    /**
     * Finds the nearest solid ground at or below a player's location for a mannequin to teleport to.
     *
     * @receiver The player whose ground to find.
     * @return The [Location] on top of the highest motion-blocking block at the player's position.
     */
    private fun Player.groundLocation(): Location =
        world
            .getHighestBlockAt(location, HeightMap.MOTION_BLOCKING_NO_LEAVES)
            .location
            .add(0.5, 1.0, 0.5)

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
