package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Block
import org.bukkit.block.Jukebox

/**
 * Provides the record inside a jukebox to Jade. The client only requests it when a record is
 * inserted; this streams the record item so the playing song name shows in the tooltip.
 */
internal object JadeJukebox : JadeBlockProvider {
    override val key: String = "minecraft:jukebox"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val jukebox = block.state as? Jukebox ?: return false
        if (!jukebox.hasRecord()) return false
        tag.putByteArray(key, itemStackPayload(jukebox.record))
        return true
    }
}
