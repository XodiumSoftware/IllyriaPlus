package org.xodium.illyriamannequins

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.anchor
import org.xodium.illyriamannequins.MannequinPDC.movementMode
import org.xodium.illyriamannequins.MannequinPDC.owner
import org.xodium.illyriamannequins.combat.MannequinCoreCombat

/** Moves mannequins according to their movement mode: following the owner or returning to their anchor. */
@Suppress("UnstableApiUsage")
internal object MannequinFollowing {
    /** The squared distance at which a moving mannequin stops when close to its destination. */
    private const val STOP_RANGE_SQUARED = 4.0

    /** The squared distance beyond which a moving mannequin teleports to its destination. */
    private const val TELEPORT_RANGE_SQUARED = 144.0

    /** The movement speed in blocks per tick for moving mannequins (player walking speed). */
    private const val SPEED = 0.21585

    /** The interval in ticks at which mannequin movement updates. */
    private const val INTERVAL_TICKS = 2L

    /** Registers the movement task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinFollowing::updateMovement,
            INTERVAL_TICKS,
            INTERVAL_TICKS,
        )
    }

    /** Moves all mannequins according to their movement mode. */
    private fun updateMovement() {
        instance.server.worlds.forEach { world ->
            world
                .entities
                .filterIsInstance<Mannequin>()
                .filterNot { MannequinCoreCombat.isEngaged(it) }
                .filterNot { MannequinCoreCombat.isDefensive(it) }
                .forEach { mannequin ->
                    when (mannequin.movementMode) {
                        MovementMode.FOLLOWING -> followOwner(mannequin)
                        MovementMode.STATIONARY -> returnToAnchor(mannequin)
                    }
                }
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
        moveToward(mannequin, owner.location, owner.groundLocation())
    }

    /**
     * Moves a mannequin back to its anchor location, teleporting when too far away.
     *
     * @param mannequin The mannequin to move.
     */
    private fun returnToAnchor(mannequin: Mannequin) {
        val anchor = mannequin.anchor ?: return
        if (anchor.world != mannequin.world) {
            mannequin.teleport(anchor.groundLocation())
            return
        }
        moveToward(mannequin, anchor, anchor.groundLocation())
    }

    /**
     * Moves a mannequin toward a destination, teleporting when too far away.
     *
     * @param mannequin The mannequin to move.
     * @param destination The destination to move toward.
     * @param teleportTarget The location to teleport to when out of range.
     */
    private fun moveToward(
        mannequin: Mannequin,
        destination: Location,
        teleportTarget: Location,
    ) {
        val here = mannequin.location
        val distanceSquared = here.distanceSquared(destination)
        if (distanceSquared <= STOP_RANGE_SQUARED) return
        if (distanceSquared > TELEPORT_RANGE_SQUARED) {
            mannequin.teleport(teleportTarget)
            return
        }
        val step =
            destination
                .toVector()
                .subtract(here.toVector())
                .setY(0)
        if (step.lengthSquared() == 0.0) return
        step.normalize().multiply(SPEED)
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
     * Finds the nearest solid ground at or below a location's position for a mannequin to teleport to.
     *
     * @receiver The location whose ground to find.
     * @return The [Location] on top of the highest motion-blocking block.
     */
    private fun Location.groundLocation(): Location =
        world
            .getHighestBlockAt(this, HeightMap.MOTION_BLOCKING_NO_LEAVES)
            .location
            .add(0.5, 1.0, 0.5)

    /**
     * Finds the nearest solid ground at or below a player's location for a mannequin to teleport to.
     *
     * @receiver The player whose ground to find.
     * @return The [Location] on top of the highest motion-blocking block at the player's position.
     */
    private fun Player.groundLocation(): Location = location.groundLocation()

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
