package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.SelectableSlotContainer
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.inventory.InventoryHolder

/**
 * Provides the book in the hovered slot of a chiseled bookshelf or shelf to Jade.
 * Only fires when hovering an occupied front slot; the client suppresses the request otherwise.
 */
internal object JadeShelf : JadeBlockProvider {
    override val key: String = "minecraft:shelf"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val level = (block.world as CraftWorld).handle
        val state = level.getBlockState(hit.blockPos)
        val container = state.block as? SelectableSlotContainer ?: return false

        val slot = container.getHitSlot(hit, hit.direction).orElse(-1)
        if (slot < 0) return false

        val holder = block.state as? InventoryHolder ?: return false
        val stack = holder.inventory.getItem(slot) ?: return false

        tag.putByteArray(key, itemStackPayload(stack))
        return true
    }
}
