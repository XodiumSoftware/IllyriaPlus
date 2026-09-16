package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.TrialSpawner

/**
 * Provides the trial spawner's remaining cooldown to Jade, in ticks, while it is cooling down.
 */
internal object JadeTrialSpawnerCooldown : JadeBlockProvider {
    override val key: String = "minecraft:mob_spawner.cooldown"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val spawner = block.state as? TrialSpawner ?: return false
        val remaining = spawner.cooldownEnd - block.world.fullTime
        if (remaining <= 0) return false
        tag.putByteArray(key, varIntPayload(remaining.toInt()))
        return true
    }
}
