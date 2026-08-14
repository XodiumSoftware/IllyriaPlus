package org.xodium.illyriaplus.damagetypes

import io.papermc.paper.registry.data.DamageTypeRegistryEntry
import org.bukkit.damage.DamageEffect
import org.bukkit.damage.DamageScaling
import org.bukkit.damage.DeathMessageType

/** Represents an object handling alcohol damage implementation within the system. */
internal object AlcoholDamageType : DamageTypeInterface {
    override fun invoke(builder: DamageTypeRegistryEntry.Builder): DamageTypeRegistryEntry.Builder =
        builder
            .damageEffect(DamageEffect.HURT)
            .damageScaling(DamageScaling.ALWAYS)
            .deathMessageType(DeathMessageType.DEFAULT)
            .exhaustion(1f)
            .messageId("alcohol_poisoning")
}
