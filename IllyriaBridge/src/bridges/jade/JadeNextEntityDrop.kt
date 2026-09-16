package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.animal.armadillo.Armadillo
import net.minecraft.world.entity.animal.sniffer.Sniffer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Chicken
import org.bukkit.entity.Entity

/**
 * Provides a mob's next-drop countdown to Jade. Unlike most providers this writes plain named
 * ints into the response NBT, matching Jade's NextEntityDropProvider. Covers chickens (eggs),
 * armadillos (scutes), and sniffers (sniff cooldown).
 */
internal object JadeNextEntityDrop : JadeEntityProvider {
    private const val EGG_TAG = "NextEggIn"
    private const val SCUTE_TAG = "NextScuteIn"
    private const val SNIFF_TAG = "NextSniffIn"

    private const val MAX_TIME = 24000 * 2

    override val key: String = "minecraft:next_entity_drop"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean =
        when (entity) {
            is Chicken -> writeChicken(entity, tag)
            is org.bukkit.entity.Armadillo -> writeArmadillo(entity, tag)
            is org.bukkit.entity.Sniffer -> writeSniffer(entity, tag)
            else -> false
        }

    /** Writes a chicken's egg-lay countdown. */
    private fun writeChicken(
        chicken: Chicken,
        tag: CompoundTag,
    ): Boolean {
        if (!chicken.isAdult) return false
        val time = chicken.eggLayTime
        if (time !in 1..<MAX_TIME) return false
        tag.putInt(EGG_TAG, time)
        return true
    }

    /** Writes an armadillo's scute-drop countdown, reading the private field reflectively. */
    private fun writeArmadillo(
        armadillo: Entity,
        tag: CompoundTag,
    ): Boolean {
        val handle = (armadillo as CraftEntity).handle
        if (handle !is Armadillo || handle.isBaby) return false
        val time = scuteTime(handle)
        if (time !in 1..<MAX_TIME) return false
        tag.putInt(SCUTE_TAG, time)
        return true
    }

    /** Writes a sniffer's sniff cooldown from its brain memory. */
    private fun writeSniffer(
        sniffer: Entity,
        tag: CompoundTag,
    ): Boolean {
        val handle = (sniffer as CraftEntity).handle
        if (handle !is Sniffer || handle.isBaby) return false
        val time = handle.brain.getTimeUntilExpiry(MemoryModuleType.SNIFF_COOLDOWN)
        if (time !in 1..<MAX_TIME) return false
        tag.putInt(SNIFF_TAG, time.toInt())
        return true
    }

    /** Reads the private `scuteTime` field of an NMS armadillo via reflection. */
    private fun scuteTime(armadillo: Armadillo): Int =
        runCatching {
            Armadillo::class.java
                .getDeclaredField("scuteTime")
                .apply { isAccessible = true }
                .getInt(armadillo)
        }.getOrDefault(0)
}
