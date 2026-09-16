package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager

/**
 * Provides breeding state to Jade: `-1` while an animal is in love mode, otherwise the breeding
 * cooldown in ticks (animals and villagers).
 */
internal object JadeMobBreeding : JadeEntityProvider {
    private const val IN_LOVE = -1

    override val key: String = "minecraft:mob_breeding"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        if (entity is Animals && entity.isLoveMode) {
            tag.putByteArray(key, varIntPayload(IN_LOVE))
            return true
        }

        val time =
            when (entity) {
                is Animals -> entity.age
                is Villager -> entity.age
                else -> return false
            }

        if (time <= 0) return false
        tag.putByteArray(key, varIntPayload(time))
        return true
    }
}
