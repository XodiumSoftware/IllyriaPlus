package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic that allows silencing mobs using an amethyst shard. */
internal object SilenceMechanic : MechanicInterface {
    override val faqTab = FaqTab.ENTITY_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.AMETHYST_SHARD)
                .setName(MM.deserialize("<mango>Silence Mechanic</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Silence Mobs</yellow> <firewatch>></gradient> " +
                            "<white>Using an amethyst shard on a mob toggles its " +
                            "silent state on or off.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEntityEvent) = silenceMob(event)

    /**
     * Toggles a mob's silent state when a player interacts with it using an amethyst shard.
     *
     * @param event The PlayerInteractEntityEvent triggered when a player interacts with an entity.
     */
    private fun silenceMob(event: PlayerInteractEntityEvent) {
        val player = event.player
        val item = event.hand.let { player.inventory.getItem(it) }

        if (item.type == Material.AIR || item.type != Material.AMETHYST_SHARD) return

        val entity = event.rightClicked as? LivingEntity ?: return

        if (entity is Player || entity is Monster) return

        event.isCancelled = true
        entity.isSilent = !entity.isSilent
        entity.world.spawnParticle(
            Particle.DUST,
            entity.location.add(0.0, entity.height / 2, 0.0),
            8,
            0.3,
            0.3,
            0.3,
            0.0,
            Particle.DustOptions(if (entity.isSilent) Color.RED else Color.GREEN, 1.0f),
        )

        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
            player.inventory.setItem(event.hand, if (item.amount > 0) item else null)
        }
    }
}
