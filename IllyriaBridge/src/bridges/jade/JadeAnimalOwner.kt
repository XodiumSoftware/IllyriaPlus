package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Tameable

/**
 * Provides a tamed animal's owner name to Jade, resolving the owner's name from the offline
 * player cache when the owner isn't online.
 */
internal object JadeAnimalOwner : JadeEntityProvider {
    override val key: String = "minecraft:animal_owner"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val tameable = entity as? Tameable ?: return false
        val ownerId = tameable.ownerUniqueId ?: return false
        val name = Bukkit.getPlayer(ownerId)?.name ?: Bukkit.getOfflinePlayer(ownerId).name ?: return false
        tag.putByteArray(key, componentPayload(name))
        return true
    }
}
