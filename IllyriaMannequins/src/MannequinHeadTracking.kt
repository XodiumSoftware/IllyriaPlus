package org.xodium.illyriamannequins

import io.papermc.paper.entity.LookAnchor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Monster

/** Makes mannequins track the nearest entity with their heads. */
@Suppress("UnstableApiUsage")
internal object MannequinHeadTracking {
    /** The squared distance within which mannequins track entities with their heads. */
    private const val TRACKING_RANGE_SQUARED = 64.0

    /**
     * Makes each mannequin look at the nearest trackable entity within range.
     *
     * @param mannequins All mannequins across all worlds.
     */
    fun tick(mannequins: Collection<Mannequin>) {
        mannequins.forEach { mannequin ->
            findNearestEntity(mannequin)?.let { target ->
                target.eyeLocation.let { eyes ->
                    mannequin.lookAt(eyes.x(), eyes.y(), eyes.z(), LookAnchor.EYES)
                }
            }
        }
    }

    /**
     * Finds the entity a mannequin should track with its head.
     * Prioritizes the nearest monster over the nearest entity overall.
     *
     * @param mannequin The mannequin to find a target entity for.
     * @return The entity to track, or `null`.
     */
    private fun findNearestEntity(mannequin: Mannequin): LivingEntity? {
        val trackable =
            mannequin
                .world
                .entities
                .filterIsInstance<LivingEntity>()
                .filter {
                    trackable(it, mannequin) &&
                        it.location.distanceSquared(mannequin.location) <= TRACKING_RANGE_SQUARED
                }
        return trackable
            .filterIsInstance<Monster>()
            .ifEmpty { trackable }
            .minByOrNull { it.location.distanceSquared(mannequin.location) }
    }

    /**
     * Checks if an entity is trackable by a mannequin.
     *
     * @param entity The entity to check.
     * @param mannequin The mannequin doing the tracking.
     * @return `true` if the entity is trackable.
     */
    private fun trackable(
        entity: LivingEntity,
        mannequin: Mannequin,
    ): Boolean =
        !entity.isDead &&
            entity.uniqueId != mannequin.uniqueId &&
            entity !is Mannequin &&
            !entity.isInvisible
}
