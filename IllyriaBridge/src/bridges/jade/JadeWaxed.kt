package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.entity.CopperGolem
import org.bukkit.entity.Entity

/**
 * Provides a copper golem's waxed status to Jade. The payload is a marker only —
 * its presence under the key means the golem is waxed, its absence means it isn't.
 */
internal object JadeWaxed : JadeEntityProvider {
    override val key: String = "minecraft:waxed"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val golem = entity as? CopperGolem ?: return false
        if (golem.oxidizing != CopperGolem.Oxidizing.waxed()) return false
        tag.putByteArray(key, ByteArray(0))
        return true
    }
}
