package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.InventoryHolder

/**
 * Provides inventory-bearing entity contents to Jade via the universal item-storage
 * provider. Covers chest/hopper minecarts, horses, llamas, donkeys, mules, allays,
 * and any other entity exposing a Bukkit inventory. Loot that has not been generated
 * yet (e.g. an unopened chest minecart) is reported as a "loot not generated" hint
 * instead of the empty contents.
 */
internal object JadeEntityItemStorage : JadeEntityProvider {
    private const val DEFAULT_UID = "minecraft:item_storage.default"
    private const val LOOT_TAG = "Loot"

    override val key: String = "minecraft:item_storage"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        if (entity is Player) return false
        val holder = entity as? InventoryHolder ?: return false
        val inventory = holder.inventory
        if (hasUngenereatedLoot(entity)) {
            tag.putBoolean(LOOT_TAG, true)
            return true
        }
        val contents = inventory.contents
        val stacks =
            contents
                .filterNotNull()
                .filter { !it.type.isAir }
                .map(CraftItemStack::asNMSCopy)
        if (stacks.isEmpty()) return false
        tag.putByteArray(key, itemStoragePayload(DEFAULT_UID, stacks))
        return true
    }

    /**
     * Checks whether a container entity still holds ungenerated loot, in which case the
     * client should show "loot not generated" rather than empty contents. Mirrors
     * Jade's `putData` check on `ContainerEntity.getContainerLootTable`.
     *
     * @param entity The entity to check
     * @return Whether the entity has an unresolved loot table
     */
    private fun hasUngenereatedLoot(entity: Entity): Boolean =
        when (entity) {
            is StorageMinecart -> entity.lootTable != null
            else -> false
        }
}
