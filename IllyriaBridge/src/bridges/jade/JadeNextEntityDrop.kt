package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.Chicken
import org.bukkit.entity.Entity

/**
 * Provides a chicken's egg-laying countdown to Jade. Unlike most providers this writes plain
 * named ints into the response NBT, matching Jade's NextEntityDropProvider.
 */
internal object JadeNextEntityDrop : JadeEntityProvider {
    private const val EGG_TAG = "NextEggIn"

    override val key: String = "minecraft:next_entity_drop"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val chicken = entity as? Chicken ?: return false
        if (!chicken.isAdult) return false
        val time = chicken.eggLayTime
        if (time <= 0) return false
        tag.putInt(EGG_TAG, time)
        return true
    }
}
