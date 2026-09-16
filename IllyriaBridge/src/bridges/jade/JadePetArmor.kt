package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.entity.CraftMob
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.Entity
import org.bukkit.entity.HappyGhast
import org.bukkit.entity.Tameable

/**
 * Provides a mob's equipped body armor to Jade — wolf armor, horse armor, llama
 * carpets and happy ghast harnesses. The body armor slot is not synced to vanilla
 * clients, so the stack is sent server-side. Absent data means no armor equipped.
 */
internal object JadePetArmor : JadeEntityProvider {
    override val key: String = "minecraft:pet_armor"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        if (entity !is Tameable && entity !is AbstractHorse && entity !is HappyGhast) return false
        val armor = (entity as CraftMob).handle.bodyArmorItem
        if (armor.isEmpty) return false
        tag.putByteArray(key, itemStackPayload(CraftItemStack.asCraftMirror(armor)))
        return true
    }
}
