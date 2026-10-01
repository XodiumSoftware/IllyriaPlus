package org.xodium.illyriamannequins.combat

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Trident
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.Utils.stepToward
import java.util.UUID

/** Handles trident combat for mannequins: throwing and catching loyalty returns. */
@Suppress("UnstableApiUsage")
internal object MannequinTridentCombat {
    /** The squared distance beyond which a mannequin throws its trident at its target. */
    const val THROW_RANGE_SQUARED = 4.0

    /** The squared distance within which a returning trident is caught by its mannequin. */
    private const val CATCH_RANGE_SQUARED = 2.25

    /** The speed in blocks per tick at which a thrown trident travels. */
    private const val THROW_SPEED = 1.5

    /** The interval in ticks at which trident returns are checked. */
    private const val RETURN_CHECK_INTERVAL_TICKS = 2L

    /** The interval in ticks to wait after throwing before the trident can be caught. */
    private const val CATCH_DELAY_TICKS = 60

    /** Tridents thrown by mannequins, keyed by trident UUID with the throw timestamp. */
    private val thrown = mutableMapOf<UUID, Pair<UUID, Long>>() // trident -> (mannequin, thrownAtWorldTime)

    /** Registers the trident return task. */
    fun register() {
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinTridentCombat::updateReturns,
            RETURN_CHECK_INTERVAL_TICKS,
            RETURN_CHECK_INTERVAL_TICKS,
        )
    }

    /**
     * Checks if a mannequin is holding a throwable trident in its main hand.
     * Only tridents with Loyalty are throwable; non-Loyalty tridents are wielded as melee weapons.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if a Loyalty trident is held.
     */
    fun hasTrident(mannequin: Mannequin): Boolean {
        val item = mannequin.equipment.getItem(EquipmentSlot.HAND)
        return item.type == Material.TRIDENT && item.getEnchantmentLevel(Enchantment.LOYALTY) > 0
    }

    /**
     * Makes a mannequin throw its held trident at a target.
     *
     * @param mannequin The mannequin throwing the trident.
     * @param target The entity to throw at.
     */
    fun throwAt(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        val tridentItem = mannequin.equipment.getItem(EquipmentSlot.HAND)
        if (tridentItem.isEmpty) return
        mannequin.swingMainHand()
        val direction = target.eyeLocation.stepToward(mannequin.eyeLocation, THROW_SPEED) ?: return
        val origin = mannequin.eyeLocation.clone().add(direction.clone().normalize().multiply(0.5))
        val trident =
            mannequin.world.spawn(origin, Trident::class.java) {
                it.itemStack = tridentItem.clone()
                it.shooter = mannequin
                it.velocity = direction
            }
        thrown[trident.uniqueId] = mannequin.uniqueId to mannequin.world.gameTime
        mannequin.equipment.setItem(EquipmentSlot.HAND, null)
    }

    /** Catches returning loyalty tridents back into their throwing mannequins' hands. */
    private fun updateReturns() {
        val iterator = thrown.entries.iterator()
        while (iterator.hasNext()) {
            val (tridentId, origin) = iterator.next()
            val trident = instance.server.getEntity(tridentId) as? Trident
            val mannequin = instance.server.getEntity(origin.first) as? Mannequin
            if (trident == null || mannequin == null || trident.world != mannequin.world) {
                iterator.remove()
                continue
            }
            if (trident.world.gameTime - origin.second < CATCH_DELAY_TICKS) continue
            val loyalty = trident.loyaltyLevel
            if (loyalty <= 0) continue
            if (trident.location.distanceSquared(mannequin.location) <= CATCH_RANGE_SQUARED) {
                mannequin.equipment.setItem(EquipmentSlot.HAND, trident.itemStack)
                trident.remove()
                iterator.remove()
            }
        }
    }
}
