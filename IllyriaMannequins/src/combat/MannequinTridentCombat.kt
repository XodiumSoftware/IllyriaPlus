package org.xodium.illyriamannequins.combat

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Trident
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
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

    /** Tridents thrown by mannequins, keyed by trident UUID. */
    private val thrown = mutableMapOf<UUID, ThrownTrident>()

    /**
     * A trident thrown by a mannequin.
     *
     * @property mannequin The unique id of the mannequin that threw the trident.
     * @property thrownAt The world time when the trident was thrown.
     * @property item A snapshot of the thrown trident, used to restore the mannequin's hand if tracking is lost.
     */
    private data class ThrownTrident(
        val mannequin: UUID,
        val thrownAt: Long,
        val item: ItemStack,
    )

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
        thrown[trident.uniqueId] =
            ThrownTrident(
                mannequin = mannequin.uniqueId,
                thrownAt = mannequin.world.gameTime,
                item = tridentItem.clone(),
            )
        mannequin.equipment.setItem(EquipmentSlot.HAND, null)
    }

    /** Catches returning loyalty tridents back into their throwing mannequins' hands. */
    private fun updateReturns() {
        val iterator = thrown.entries.iterator()
        while (iterator.hasNext()) {
            val (tridentId, thrownTrident) = iterator.next()
            val mannequin = instance.server.getEntity(thrownTrident.mannequin) as? Mannequin

            // Mannequin is gone — nothing to restore the trident to.
            if (mannequin == null) {
                iterator.remove()
                continue
            }

            val trident = instance.server.getEntity(tridentId) as? Trident
            // Trident entity vanished (chunk unload, removal) — restore the mannequin's hand.
            if (trident == null) {
                restoreItem(mannequin, thrownTrident.item)
                iterator.remove()
                continue
            }

            // Mannequin crossed worlds — leave the trident behind and restore the mannequin's hand.
            if (trident.world != mannequin.world) {
                restoreItem(mannequin, thrownTrident.item)
                iterator.remove()
                continue
            }

            if (trident.world.gameTime - thrownTrident.thrownAt < CATCH_DELAY_TICKS) continue
            if (trident.loyaltyLevel <= 0) continue
            if (trident.location.distanceSquared(mannequin.location) <= CATCH_RANGE_SQUARED) {
                mannequin.equipment.setItem(EquipmentSlot.HAND, trident.itemStack)
                trident.remove()
                iterator.remove()
            }
        }
    }

    /**
     * Returns a trident snapshot to a mannequin's main hand if the slot is free.
     *
     * @param mannequin The mannequin to restore the item to.
     * @param item The snapshot of the thrown trident.
     */
    private fun restoreItem(
        mannequin: Mannequin,
        item: ItemStack,
    ) {
        if (mannequin.equipment.getItem(EquipmentSlot.HAND).isEmpty) {
            mannequin.equipment.setItem(EquipmentSlot.HAND, item)
        }
    }
}
