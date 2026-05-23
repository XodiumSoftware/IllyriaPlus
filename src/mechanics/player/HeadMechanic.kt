@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.random.Random

/** Represents a mechanic handling player head drops within the system. */
internal object HeadMechanic : MechanicInterface {
    private const val SKULL_DROP_CHANCE: Double = 0.01

    override val infoItem =
        Item.simple(
            ItemBuilder(Material.PLAYER_HEAD)
                .setName(MM.deserialize("<mango>Head Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Player Heads</yellow> <firewatch>></gradient> " +
                            "<white>1% chance to drop on death</white>",
                    ),
                ),
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerDeathEvent) = dropPlayerHead(event.player)

    /**
     * Attempts to drop the specified player's head at their current location.
     *
     * @param player The player whose head may be dropped.
     */
    private fun dropPlayerHead(player: Player) {
        if (Random.nextDouble() > SKULL_DROP_CHANCE) return

        player.world.dropItemNaturally(player.location, player.head())
    }

    /**
     * Creates a player head item.
     *
     * @return The head ItemStack.
     */
    @Suppress("UnstableApiUsage")
    private fun Player.head(): ItemStack =
        ItemStack.of(Material.PLAYER_HEAD).apply {
            setData(
                DataComponentTypes.PROFILE,
                ResolvableProfile.resolvableProfile(playerProfile),
            )
        }
}
