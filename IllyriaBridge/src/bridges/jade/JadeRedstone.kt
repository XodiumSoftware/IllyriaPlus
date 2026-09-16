package org.xodium.illyriabridge.bridges.jade

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.CalibratedSculkSensorBlock
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity
import net.minecraft.world.level.block.entity.ComparatorBlockEntity
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftWorld

/**
 * Provides redstone signal data to Jade for comparators and calibrated sculk sensors.
 * Levers, repeaters, and redstone wire read their state client-side and need no server data.
 */
internal object JadeRedstone : JadeBlockProvider {
    override val key: String = "minecraft:redstone"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val level = (block.world as CraftWorld).handle
        val pos = BlockPos(block.x, block.y, block.z)

        val signal =
            when (val blockEntity = level.getBlockEntity(pos)) {
                is ComparatorBlockEntity -> blockEntity.outputSignal

                is CalibratedSculkSensorBlockEntity -> {
                    val state = blockEntity.blockState
                    val facing =
                        if (state.hasProperty(CalibratedSculkSensorBlock.FACING)) {
                            state.getValue(CalibratedSculkSensorBlock.FACING).opposite
                        } else {
                            return false
                        }
                    level.getSignal(pos.relative(facing), facing)
                }

                else -> return false
            }

        tag.putByteArray(key, varIntPayload(signal))
        return true
    }
}
