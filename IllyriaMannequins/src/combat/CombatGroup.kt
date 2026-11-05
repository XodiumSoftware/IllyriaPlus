package org.xodium.illyriamannequins.combat

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The mannequins owned by a single player that fight together.
 *
 * A group keeps a threat table of hostile entities near any of its members and commits to a single
 * focus target at a time. Members around the focus are assigned evenly spaced slots so they surround
 * the target instead of stacking on the same spot.
 */
internal class CombatGroup(
    /** The UUID of the player that owns every member of this group. */
    val owner: UUID,
) {
    /** The members of this group, in a stable order used for slot assignment. */
    val members: MutableList<Mannequin> = mutableListOf()

    /** The entity every member is currently focused on. */
    var focus: LivingEntity? = null

    /** The threat scores of hostile entities near this group, keyed by entity UUID. */
    val threats: MutableMap<UUID, Double> = mutableMapOf()

    /**
     * Checks if a mannequin belongs to this group.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if the mannequin is a member of this group.
     */
    fun contains(mannequin: Mannequin): Boolean = members.any { it.uniqueId == mannequin.uniqueId }

    /**
     * Computes the slot offset around the focus target for a mannequin in this group.
     *
     * Each member gets an evenly spaced slot on a circle around the target, so a group of N
     * surrounds its focus instead of crowding the exact target location.
     *
     * @param mannequin The mannequin to compute a slot for.
     * @param radius The radius of the surrounding circle, in blocks.
     * @return The offset from the focus target, or `null` if the mannequin is not a member.
     */
    fun slotOffset(
        mannequin: Mannequin,
        radius: Double,
    ): SlotOffset? {
        val index = members.indexOfFirst { it.uniqueId == mannequin.uniqueId }
        if (index < 0) return null
        val angle = 2.0 * PI * index / members.size
        return SlotOffset(cos(angle) * radius, sin(angle) * radius)
    }

    /**
     * Records a threat against an entity, adding to its existing score.
     *
     * @param source The hostile entity.
     * @param amount The score to add.
     */
    fun addThreat(
        source: LivingEntity,
        amount: Double,
    ) {
        threats[source.uniqueId] = (threats[source.uniqueId] ?: 0.0) + amount
    }

    /**
     * Decays every accumulated threat score, dropping entities whose score falls to zero.
     *
     * @param factor The multiplier applied to each score each scan.
     */
    fun decayThreats(factor: Double) {
        threats.entries.removeIf { entry ->
            val decayed = entry.value * factor
            entry.setValue(decayed)
            decayed < THREAT_EPSILON
        }
    }

    /**
     * Picks the highest scoring entity in this group's threat table.
     *
     * @return The top threat, or `null` if the table is empty.
     */
    fun topThreat(): LivingEntity? =
        threats
            .maxByOrNull { it.value }
            ?.key
            ?.let { instance.server.getEntity(it) as? LivingEntity }
            ?.takeIf { it.isValid }

    companion object {
        /** The threat score below which an entry is discarded. */
        private const val THREAT_EPSILON = 0.01
    }
}

/**
 * A horizontal offset from a focus target.
 *
 * @property x The X component, in blocks.
 * @property z The Z component, in blocks.
 */
internal data class SlotOffset(
    val x: Double,
    val z: Double,
)
