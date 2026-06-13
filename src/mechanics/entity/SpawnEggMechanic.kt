package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.random.Random

/** Represents a mechanic handling spawn egg drops within the system. */
internal object SpawnEggMechanic : MechanicInterface {
    private const val SPAWN_EGG_DROP_CHANCE: Double = 0.001

    @EventHandler
    fun on(event: EntityDeathEvent) = spawnEggDrop(event)

    /**
     * Handles spawn egg drops on entity death.
     *
     * @param event The EntityDeathEvent triggered when an entity dies.
     */
    private fun spawnEggDrop(event: EntityDeathEvent) {
        if (Random.nextDouble() <= SPAWN_EGG_DROP_CHANCE) {
            Material.matchMaterial("${event.entityType.name}_SPAWN_EGG")?.let { event.drops.add(ItemStack.of(it)) }
        }
    }
}
