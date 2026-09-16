package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.entity.Entity

/** A server-side data provider for Jade, identified by a channel key. */
internal interface JadeBlockProvider {
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

/** A server-side data provider for Jade entity data, identified by a channel key. */
internal interface JadeEntityProvider {
    /** The provider's unique key, as advertised in the handshake (e.g. "minecraft:entity_health"). */
    val key: String

    /**
     * Writes this provider's data into the response tag for the given entity.
     *
     * @param entity The target entity
     * @param tag The response NBT to write into
     * @return Whether data was written (false if the entity doesn't support this provider)
     */
    fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean
}
