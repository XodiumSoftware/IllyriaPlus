package org.xodium.illyriabending

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Converts an integer to its Roman numeral representation.
 *
 * Only values in `1..5` are mapped; anything outside that range falls back to the number's
 * decimal string.
 *
 * @return The Roman numeral for this value, or its decimal string if out of range.
 */
internal fun Int.toRoman(): String =
    when (this) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        else -> toString()
    }

/**
 * Rotates this vector around an axis using Rodrigues' rotation formula.
 *
 * @param axis The rotation axis; will be normalized before use.
 * @param angle The rotation angle in radians.
 * @return The rotated vector.
 */
internal fun Vector.rotateAround(
    axis: Vector,
    angle: Double,
): Vector {
    val k = axis.clone().normalize()
    val cosA = kotlin.math.cos(angle)
    val sinA = kotlin.math.sin(angle)
    val term1 = this.clone().multiply(cosA)
    val term2 = k.clone().crossProduct(this).multiply(sinA)
    val term3 = k.clone().multiply(k.dot(this) * (1.0 - cosA))
    return term1.add(term2).add(term3)
}

/**
 * Gets living entities within a radius of a location.
 *
 * @param location The center location.
 * @param radius The search radius.
 * @return List of nearby living entities.
 */
internal fun Location.nearbyLivingEntities(radius: Double): List<LivingEntity> =
    this
        .world
        ?.getNearbyEntities(this, radius, radius, radius)
        ?.filterIsInstance<LivingEntity>()
        ?: emptyList()

/**
 * Damages entities and applies knockback away from a source location.
 *
 * @param player The player dealing damage.
 * @param entities The entities to damage.
 * @param damage Amount of damage in hearts.
 * @param source The location to knock entities away from.
 * @param knockback Horizontal knockback strength.
 * @param knockup Vertical knockback strength.
 */
internal fun damageEntities(
    player: Player,
    entities: List<LivingEntity>,
    damage: Double,
    source: Location,
    knockback: Double = 0.0,
    knockup: Double = 0.0,
) {
    entities.forEach { entity ->
        entity.damage(damage, player)
        if (knockback > 0.0 || knockup > 0.0) {
            val push =
                entity
                    .location
                    .toVector()
                    .subtract(source.toVector())
                    .normalize()
                    .apply { y = knockup }
                    .multiply(knockback)
            entity.velocity = entity.velocity.add(push)
        }
    }
}
