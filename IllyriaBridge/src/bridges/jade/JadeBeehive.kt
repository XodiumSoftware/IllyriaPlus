package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Beehive
import org.bukkit.block.Block

/**
 * Provides bee occupancy to Jade. Honey level is read client-side from the block state; this
 * supplies the bee count, positive when the hive is full and negative otherwise (as Jade expects).
 */
internal object JadeBeehive : JadeBlockProvider {
    override val key: String = "minecraft:beehive"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val beehive = block.state as? Beehive ?: return false
        val bees = beehive.entityCount
        tag.putByteArray(key, bytePayload(if (beehive.isFull) bees else -bees))
        return true
    }
}
