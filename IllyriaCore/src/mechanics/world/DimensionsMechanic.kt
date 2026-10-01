package org.xodium.illyriacore.mechanics.world

import org.bukkit.event.EventHandler
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.loot.LootTables
import org.xodium.illyriacore.items.IncendiumKeyItem
import org.xodium.illyriacore.items.NullscapeKeyItem
import org.xodium.illyriacore.mechanics.MechanicInterface
import kotlin.random.Random

/**
 * Injects dimension keys into trial chamber loot tables.
 */
internal object DimensionsMechanic : MechanicInterface {
    private val KEY_CHANCES =
        mapOf(
            IncendiumKeyItem to 0.12,
            NullscapeKeyItem to 0.03,
        )

    private val TARGET_TABLES =
        setOf(
            LootTables.TRIAL_CHAMBERS_REWARD.key,
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.key,
        )

    @EventHandler
    fun on(event: LootGenerateEvent) {
        if (event.lootTable.key !in TARGET_TABLES) return

        KEY_CHANCES.forEach { (item, chance) ->
            if (Random.nextDouble() < chance) {
                event.loot.add(item())
            }
        }
    }
}
