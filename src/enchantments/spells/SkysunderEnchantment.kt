package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.Utils.Enchantment.isSelectedSpell
import org.xodium.illyriaplus.Utils.Enchantment.validateSpellCast
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.managers.XpManager

/** Represents an object handling skysunder enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object SkysunderEnchantment : EnchantmentInterface {
    private const val XP_COST = 3
    private const val RANGE = 30.0

    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(4)
            .maxLevel(1)
            .weight(1)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(20, 5))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(65, 5))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    /**
     * Handles player interaction for casting Skysunder.
     *
     * @param event The interaction event.
     */
    @EventHandler
    fun on(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (!isSelectedSpell(item, get())) return
        if (!validateSpellCast(event.action, item, get())) return
        if (!XpManager.consumeXp(event, XP_COST)) return

        val blockResult = player.rayTraceBlocks(RANGE)
        val entityResult = player.rayTraceEntities(RANGE.toInt())
        val eyeLoc = player.eyeLocation
        val blockDist = blockResult?.hitPosition?.distance(eyeLoc.toVector())
        val entityDist = entityResult?.hitPosition?.distance(eyeLoc.toVector())

        val target =
            when {
                blockDist != null && (entityDist == null || blockDist <= entityDist) -> {
                    blockResult.hitPosition.toLocation(player.world)
                }

                entityDist != null -> {
                    entityResult.hitPosition.toLocation(player.world)
                }

                else -> {
                    eyeLoc.add(
                        player.location.direction
                            .normalize()
                            .multiply(RANGE),
                    )
                }
            }

        Particle.ELECTRIC_SPARK
            .builder()
            .location(target)
            .count(30)
            .offset(0.3, 0.5, 0.3)
            .spawn()

        player.world.strikeLightning(target)
    }
}
