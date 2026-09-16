package org.xodium.illyriabridge.bridges.jade

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Block
import org.bukkit.block.Campfire
import org.bukkit.craftbukkit.inventory.CraftItemStack

/**
 * Provides a campfire's cooking items to Jade via the universal item-storage provider,
 * attaching each slot's cooking countdown so the client shows a timer on the item.
 */
internal object JadeCampfire : JadeBlockProvider {
    private const val EXTENSION_UID = "minecraft:campfire"
    private const val COOKING_TAG = "jade:cooking"

    override val key: String = "minecraft:item_storage"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val campfire = block.state as? Campfire ?: return false

        val size = campfire.size
        val stacks =
            (0 until size)
                .filter { slot ->
                    val item = campfire.getItem(slot)
                    item != null && !item.type.isAir
                }.map { slot ->
                    val nms = CraftItemStack.asNMSCopy(campfire.getItem(slot))
                    val remaining = campfire.getCookTimeTotal(slot) - campfire.getCookTime(slot)
                    val data = CompoundTag().apply { putInt(COOKING_TAG, remaining) }
                    nms.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
                    nms
                }

        if (stacks.isEmpty()) return false

        tag.putByteArray(key, itemStoragePayload(EXTENSION_UID, stacks))
        return true
    }
}
