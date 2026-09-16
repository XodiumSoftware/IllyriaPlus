package org.xodium.illyriabridge.bridges.jade

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.block.Block
import org.bukkit.block.Campfire
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.InventoryHolder

/**
 * Provides container contents to Jade via the universal item-storage provider.
 * Campfires attach each slot's cooking countdown; other containers (chests, barrels,
 * shulker boxes, hoppers, etc.) use the default display so hovering shows their items.
 */
internal object JadeItemStorage : JadeBlockProvider {
    private const val CAMPFIRE_UID = "minecraft:campfire"
    private const val DEFAULT_UID = "minecraft:item_storage.default"
    private const val COOKING_TAG = "jade:cooking"

    override val key: String = "minecraft:item_storage"

    override fun write(
        block: Block,
        hit: BlockHitResult,
        tag: CompoundTag,
    ): Boolean {
        val campfire = block.state as? Campfire
        return if (campfire != null) {
            writeCampfire(campfire, tag)
        } else {
            writeContainer(block, tag)
        }
    }

    /**
     * Encodes the campfire's cooking items, attaching each slot's remaining cook time as a
     * countdown the client shows next to the item.
     *
     * @param campfire The campfire state
     * @param tag The response NBT
     * @return Whether any item was written
     */
    private fun writeCampfire(
        campfire: Campfire,
        tag: CompoundTag,
    ): Boolean {
        val stacks =
            (0 until campfire.size)
                .mapNotNull { slot ->
                    val item = campfire.getItem(slot) ?: return@mapNotNull null
                    if (item.type.isAir) return@mapNotNull null
                    val nms = CraftItemStack.asNMSCopy(item)
                    val remaining = campfire.getCookTimeTotal(slot) - campfire.getCookTime(slot)
                    val data = CompoundTag().apply { putInt(COOKING_TAG, remaining) }
                    nms.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
                    nms
                }

        if (stacks.isEmpty()) return false
        tag.putByteArray(key, itemStoragePayload(CAMPFIRE_UID, stacks))
        return true
    }

    /**
     * Encodes a generic container's non-empty items (chests, barrels, shulker boxes, hoppers,
     * etc.) under the default item-storage uid so the client shows the contents.
     *
     * @param block The container block
     * @param tag The response NBT
     * @return Whether any item was written
     */
    private fun writeContainer(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val holder = block.state as? InventoryHolder ?: return false
        val contents = holder.inventory.contents
        val stacks =
            contents
                .filterNotNull()
                .filter { !it.type.isAir }
                .map(CraftItemStack::asNMSCopy)

        if (stacks.isEmpty()) return false
        tag.putByteArray(key, itemStoragePayload(DEFAULT_UID, stacks))
        return true
    }
}
