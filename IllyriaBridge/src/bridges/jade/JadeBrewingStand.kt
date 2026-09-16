package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.BrewingStand
import org.xodium.illyriabridge.bridges.jade.JadeBridge.varIntPairPayload

/**
 * Provides brewing stand state to Jade: remaining blaze-powder fuel and, while brewing,
 * the time left until the current batch finishes.
 */
internal object JadeBrewingStand : JadeProvider {
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
