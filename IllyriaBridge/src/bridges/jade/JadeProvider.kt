package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block

/** A server-side data provider for Jade, identified by a channel key. */
internal interface JadeProvider {
    /** The provider's unique key, as advertised in the handshake (e.g. "minecraft:hopper_lock"). */
    val key: String

    /**
     * Writes this provider's data into the response tag for the given block.
     *
     * @param block The target block
     * @param tag The response NBT to write into
     * @return Whether data was written (false if the block doesn't support this provider)
     */
    fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean
}
