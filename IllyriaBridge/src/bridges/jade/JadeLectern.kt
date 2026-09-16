package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.Lectern

/**
 * Provides the book placed on a lectern to Jade. The client only requests it when a book is present;
 * this streams the book item so its name shows in the tooltip.
 */
internal object JadeLectern : JadeProvider {
    override val key: String = "minecraft:lectern"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val lectern = block.state as? Lectern ?: return false
        if (lectern.inventory.isEmpty) return false
        tag.putByteArray(key, itemStackPayload(lectern.inventory.getItem(0)))
        return true
    }
}
