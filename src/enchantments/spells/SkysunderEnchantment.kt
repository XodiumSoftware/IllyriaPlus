package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.Particle
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.Enchantment.displayName

/** Represents an object handling skysunder enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object SkysunderEnchantment : SpellEnchantmentInterface {
    private const val RANGE = 30.0

    override val cooldown: Long = 300L
    override val categoryCooldown: Long = 120L
    override val castDelay: Long = 16L
    override val category: SpellCategory = SpellCategory.AREA

    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(4)
            .maxLevel(1)
            .weight(1)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(20, 5))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(65, 5))
            .activeSlots(EquipmentSlotGroup.MAINHAND)

    override fun cast(event: PlayerInteractEvent) {
        val player = event.player
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
