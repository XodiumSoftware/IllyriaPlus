package org.xodium.illyriacore.mechanics.world

import org.bukkit.event.EventHandler
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.loot.LootTables
import org.xodium.illyriacore.items.IncendiumKeyItem
import org.xodium.illyriacore.items.NullscapeKeyItem
import org.xodium.illyriacore.mechanics.MechanicInterface
import kotlin.random.Random

/**
 * Injects custom items into vanilla loot tables at configurable drop chances.
 */
internal object LootMechanic : MechanicInterface {
    private val LOOT_TABLES =
        mapOf(
            LootTables.TRIAL_CHAMBERS_REWARD.key to
                mapOf(
                    IncendiumKeyItem to 0.12,
                    NullscapeKeyItem to 0.03,
                ),
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.key to
                mapOf(
                    IncendiumKeyItem to 0.12,
                    NullscapeKeyItem to 0.03,
                ),
        )

    @EventHandler
    fun on(event: LootGenerateEvent) {
        LOOT_TABLES[event.lootTable.key]?.forEach { (item, chance) ->
            if (Random.nextDouble() < chance) {
                event.loot.add(item())
            }
        }
    }
}
