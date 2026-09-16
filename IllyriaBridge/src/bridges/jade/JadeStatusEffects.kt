package org.xodium.illyriabridge.bridges.jade

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Provides a living entity's active, visible potion effects to Jade, rendered as a colored
 * list with durations. Effect add/update time tracking is simplified to now, so the client's
 * fade-in animation is not shown.
 */
internal object JadeStatusEffects : JadeEntityProvider {
    override val key: String = "minecraft:potion_effects"

    override fun write(
        entity: Entity,
        tag: CompoundTag,
    ): Boolean {
        val living = entity as? LivingEntity ?: return false
        val effects = living.activePotionEffects.filter { it.hasIcon() }
        if (effects.isEmpty()) return false

        val buf = newBuffer()
        buf.writeVarInt(effects.size)
        effects.forEach { writeEffect(buf, it) }
        tag.putByteArray(key, buf.toByteArray())
        return true
    }

    /**
     * Writes one Jade Effect record: the NMS effect instance followed by two wall-clock longs
     * used by the client for sorting and fade-in animation.
     *
     * @param buf The buffer to write into
     * @param effect The Bukkit potion effect to encode
     */
    private fun writeEffect(
        buf: RegistryFriendlyByteBuf,
        effect: PotionEffect,
    ) {
        val holder = mobEffectHolder(effect.type) ?: return
        val nms =
            MobEffectInstance(
                holder,
                effect.duration,
                effect.amplifier,
                effect.isAmbient,
                effect.hasParticles(),
                effect.hasIcon(),
            )
        MobEffectInstance.STREAM_CODEC.encode(buf, nms)
        val now = System.currentTimeMillis()
        buf.writeLong(now) // updateTime
        buf.writeLong(now) // addTime
    }

    /**
     * Resolves a Bukkit potion effect type to its NMS holder.
     *
     * @param type The Bukkit potion effect type
     * @return The NMS holder, or null if it can't be resolved
     */
    private fun mobEffectHolder(type: PotionEffectType) =
        BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(type.key.toString())).orElse(null)
}
