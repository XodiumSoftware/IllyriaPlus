package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.Furnace

/**
 * Provides furnace state to Jade: current smelt progress, total cook time, and the
 * input/fuel/result slots, so the tooltip shows a live progress arrow with items.
 */
internal object JadeFurnace : JadeProvider {
    override val key: String = "minecraft:furnace"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val furnace = block.state as? Furnace ?: return false
        val inventory = furnace.inventory
        tag.putByteArray(
            key,
            furnacePayload(
                furnace.cookTime.toInt(),
                furnace.cookTimeTotal,
                inventory.smelting,
                inventory.fuel,
                inventory.result,
            ),
        )
        return true
    }
}
