package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Block
import org.bukkit.block.data.type.Hopper

/**
 * Provides the hopper lock state to Jade, replacing the object name with a locked
 * indicator when redstone power disables the hopper.
 */
internal object JadeHopperLock : JadeBlockProvider {
    override val key: String = "minecraft:hopper_lock"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val hopper = block.blockData as? Hopper ?: return false
        tag.putByteArray(key, boolPayload(!hopper.isEnabled))
        return true
    }
}
