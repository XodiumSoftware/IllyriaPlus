package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.random.Random

/** Represents a mechanic handling spawn egg drops within the system. */
internal object SpawnEggMechanic : MechanicInterface {
    private const val SPAWN_EGG_DROP_CHANCE: Double = 0.001

    override val faqTab = FaqTab.ENTITY_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
                .setName(MM.deserialize("<mango>Spawn Egg Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Rare Drops</yellow> <firewatch>></gradient> " +
                            "<white>0.1% chance for mobs to drop their spawn egg</white>",
                    ),
                ),
        )

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
