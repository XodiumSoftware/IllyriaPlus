package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * Provides a living entity's absorption amount to Jade. The hearts and armor shown in the
 * tooltip are read client-side; the server only contributes the absorption portion.
 */
internal object JadeEntityHealth : JadeEntityProvider {
    override val key: String = "minecraft:entity_health"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val living = entity as? LivingEntity ?: return false
        tag.putByteArray(key, floatPayload(living.absorptionAmount.toFloat()))
        return true
    }
}
