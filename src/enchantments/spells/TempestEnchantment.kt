package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import org.bukkit.entity.WindCharge
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.util.Vector
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Enchantment.displayName

/** Represents an object handling tempest enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object TempestEnchantment : SpellEnchantmentInterface {
    private const val SPREAD_AMOUNT = 0.2

    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.breeze.shoot"), Sound.Source.HOSTILE, 1.0f, 1.0f)

    override val cooldown: Long = 200L
    override val categoryCooldown: Long = 120L
    override val castDelay: Long = 8L
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
        val baseDir = player.location.direction.normalize()
        val right = baseDir.clone().crossProduct(Vector(0, 1, 0)).normalize()
        val spawnBase = player.eyeLocation.add(baseDir.clone().multiply(1.5))
        val offsets = listOf(-SPREAD_AMOUNT, 0.0, SPREAD_AMOUNT)

        offsets.forEach {
            val dir = baseDir.clone().add(right.clone().multiply(it)).normalize()
            val charge =
                player.world.spawn(
                    spawnBase.clone().add(right.clone().multiply(it * 0.5)),
                    WindCharge::class.java,
                )

            charge.shooter = player
            charge.velocity = dir.multiply(1.5)

            spawnWindChargeTrail(charge)
        }

        player.playSound(CAST_SOUND)
    }

    /**
     * Spawns a repeating particle trail behind [charge] every tick until the entity is no longer valid.
     *
     * @param charge The [WindCharge] to trail.
     */
    private fun spawnWindChargeTrail(charge: WindCharge) =
        Utils.Schedule.spawnProjectileTrail(charge) {
            Particle.GUST
                .builder()
                .location(it)
                .count(1)
                .spawn()
            Particle.CLOUD
                .builder()
                .location(it)
                .count(3)
                .offset(0.05, 0.05, 0.05)
                .extra(0.02)
                .spawn()
        }
}
