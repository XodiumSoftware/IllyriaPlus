package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.Ageable
import org.bukkit.entity.Entity

/**
 * Provides a baby mob's remaining growth time to Jade, in ticks, while it is still growing.
 */
internal object JadeMobGrowth : JadeEntityProvider {
    override val key: String = "minecraft:mob_growth"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val ageable = entity as? Ageable ?: return false
        val remaining = -ageable.age
        if (remaining <= 0) return false
        tag.putByteArray(key, varIntPayload(remaining))
        return true
    }
}
