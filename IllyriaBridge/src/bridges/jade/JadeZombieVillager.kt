package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.Entity
import org.bukkit.entity.ZombieVillager

/**
 * Provides a zombie villager's remaining conversion time to Jade, in ticks, while it is
 * being cured.
 */
internal object JadeZombieVillager : JadeEntityProvider {
    override val key: String = "minecraft:zombie_villager"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val zombieVillager = entity as? ZombieVillager ?: return false
        if (!zombieVillager.isConverting) return false
        val time = zombieVillager.conversionTime
        if (time <= 0) return false
        tag.putByteArray(key, varIntPayload(time))
        return true
    }
}
