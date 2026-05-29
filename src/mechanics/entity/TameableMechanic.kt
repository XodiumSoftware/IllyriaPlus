package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.PlayerUtils.getLeashedEntity
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling tameable entities within the system. */
internal object TameableMechanic : MechanicInterface {
    override val faqTab = FaqTab.ENTITY

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.WOLF_SPAWN_EGG)
                .setName(MM.deserialize("<mango>Tameable Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Transfer Pets</yellow> <firewatch>></gradient> <white>Hold lead + " +
                            "right-click player</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: PlayerInteractEntityEvent) = handleInteract(event)

    /**
     * Handles player interactions to transfer tameable entity ownership.
     *
     * @param event The PlayerInteractEntityEvent triggered by the player.
     */
    private fun handleInteract(event: PlayerInteractEntityEvent) {
        if (transferOwnership(event.player, event.rightClicked as? Player ?: return)) event.isCancelled = true
    }

    /**
     * Transfers ownership of a tamed, leashed entity from [source] to [target].
     * Requires [source] to hold a lead in their main hand and be the current owner of the pet.
     *
     * @param source The player initiating the transfer.
     * @param target The player receiving ownership.
     * @return `true` if ownership was successfully transferred; `false` otherwise.
     */
    private fun transferOwnership(
        source: Player,
        target: Player,
    ): Boolean {
        if (source.inventory.itemInMainHand.type != Material.LEAD) return false

        val pet = source.getLeashedEntity() ?: return false

        if (!pet.isTamed || pet.owner != source) return false

        pet.owner = target
        pet.setLeashHolder(target)
        return true
    }
}
