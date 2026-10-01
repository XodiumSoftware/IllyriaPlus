package org.xodium.illyriacore.mechanics.world

import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.world.LootGenerateEvent
import org.xodium.illyriacore.mechanics.MechanicInterface

/**
 * Handles custom loot injection for dimension-related items.
 * Integrates dimension keys into trial chamber loot tables.
 */
internal object DimensionsMechanic : MechanicInterface {
    private val TARGET_TABLES =
        setOf(
            NamespacedKey.minecraft("chests/trial_chambers/reward"),
            NamespacedKey.minecraft("chests/trial_chambers/reward_ominous"),
        )

    @EventHandler
    fun on(event: LootGenerateEvent) {
        if (event.lootTable.key !in TARGET_TABLES) return

        // TODO: Implement weighted injection logic
        // Example: if (Random.nextDouble() < CHANCE) event.loot.add(KeyItem())
    }
}
