package org.xodium.illyriabending.pdcs

import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriabending.IllyriaBending.Companion.instance

/**
 * Helpers for tagging bending projectiles with their per-level scaled damage, so damage-event
 * listeners can apply custom numbers regardless of vanilla projectile behavior.
 */
internal object ProjectilePDC {
    /** [NamespacedKey] storing a projectile's scaled base damage. */
    private val DAMAGE_KEY = NamespacedKey(instance, "bending_projectile_damage")

    /** Tags this entity with the given damage value. Applied by `EntityDamageByEntityEvent` listeners. */
    fun Entity.tagDamage(damage: Float) {
        persistentDataContainer.set(DAMAGE_KEY, PersistentDataType.FLOAT, damage)
    }

    /** Reads the tagged damage on this entity, or `null` if untagged. */
    fun Entity.taggedDamage(): Float? = persistentDataContainer.get(DAMAGE_KEY, PersistentDataType.FLOAT)
}
