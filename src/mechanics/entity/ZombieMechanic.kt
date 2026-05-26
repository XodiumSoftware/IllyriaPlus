package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling zombie behavior and drops within the system. */
internal object ZombieMechanic : MechanicInterface {
    private const val HORDE_RADIUS: Double = 24.0

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(MM.deserialize("<mango>Zombie Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Horde Alert</yellow> <firewatch>></gradient> <white>When a zombie spots a player, it alerts nearby zombies within $HORDE_RADIUS blocks to join the chase.</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.ENTITY

    @EventHandler
    fun on(event: EntityTargetLivingEntityEvent) = alertHorde(event)

    /**
     * Alerts nearby zombies to join the chase when a zombie targets a player.
     *
     * @param event The EntityTargetLivingEntityEvent triggered when an entity targets another.
     */
    private fun alertHorde(event: EntityTargetLivingEntityEvent) {
        val zombie = event.entity as? Zombie ?: return
        val target = event.target as? Player ?: return

        zombie
            .getNearbyEntities(HORDE_RADIUS, HORDE_RADIUS, HORDE_RADIUS)
            .filterIsInstance<Zombie>()
            .filter { it.uniqueId != zombie.uniqueId && it.target != target }
            .forEach { it.target = target }
    }
}
