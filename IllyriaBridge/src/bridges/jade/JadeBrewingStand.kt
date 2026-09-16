package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.BrewingStand

/**
 * Provides brewing stand state to Jade: remaining blaze-powder fuel and, while brewing,
 * the time left until the current batch finishes.
 */
internal object JadeBrewingStand : JadeBlockProvider {
    override val key: String = "minecraft:brewing_stand"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val brewingStand = block.state as? BrewingStand ?: return false
        tag.putByteArray(key, varIntPairPayload(brewingStand.fuelLevel, brewingStand.brewingTime))
        return true
    }
}
