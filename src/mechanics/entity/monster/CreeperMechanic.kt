package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.entity.Creeper
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling creeper behavior and spawns within the system. */
internal object CreeperMechanic : MechanicInterface {
    private const val IS_POWERED: Boolean = true

    private val explosionRadiusRange: IntRange = 4..7
    private val difficulty: Difficulty = Difficulty.HARD

    override val faqTab = FaqTab.ENTITY_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.CREEPER_HEAD)
                .setName(Utils.MM.deserialize("<mango>Creeper Mechanics</gradient>"))
                .addLoreLines(
                    Utils.MM.deserialize(""),
                    Utils.MM.deserialize(
                        "<yellow>Charged Spawn</yellow> <firewatch>></gradient> " +
                            "<white>All creepers spawn powered (charged by lightning).</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Volatile Payload</yellow> <firewatch>></gradient> " +
                            "<white>Creeper explosion radius is increased to ${explosionRadiusRange.first}-${explosionRadiusRange.last} blocks.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Creeper ?: return)
        }
    }

    /**
     * Modifies a creeper's attributes on Hard difficulty.
     *
     * @param creeper The creeper to modify.
     */
    private fun modifySpawn(creeper: Creeper) {
        creeper.isPowered = IS_POWERED
        creeper.explosionRadius = explosionRadiusRange.random()
    }
}
