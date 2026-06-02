package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.util.Vector
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.Utils.Enchantment.isSelectedSpell
import org.xodium.illyriaplus.Utils.Enchantment.validateSpellCast
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.managers.XpManager

/** Represents an object handling tempest enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object TempestEnchantment : EnchantmentInterface {
    private const val XP_COST = 4
    private const val SPREAD_AMOUNT = 0.2

    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.breeze.shoot"), Sound.Source.HOSTILE, 1.0f, 1.0f)

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
     * Handles player interaction for casting Tempest.
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
