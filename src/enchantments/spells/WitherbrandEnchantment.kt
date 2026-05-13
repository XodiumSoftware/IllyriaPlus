package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import org.bukkit.entity.WitherSkull
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.EnchantmentUtils.displayName
import org.xodium.illyriaplus.Utils.EnchantmentUtils.isSelectedSpell
import org.xodium.illyriaplus.Utils.EnchantmentUtils.validateSpellCast
import org.xodium.illyriaplus.interfaces.EnchantmentInterface
import org.xodium.illyriaplus.managers.XpManager

/** Represents an object handling witherbrand enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object WitherbrandEnchantment : EnchantmentInterface {
    private const val XP_COST = 2

    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.wither.shoot"), Sound.Source.HOSTILE, 1.0f, 1.0f)

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
     * Handles player interaction for casting Witherbrand.
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

        val direction = player.location.direction.normalize()
        val spawnLocation = player.eyeLocation.add(direction.clone().multiply(1.5))
        val skull = player.world.spawn(spawnLocation, WitherSkull::class.java)

        skull.shooter = player
        skull.direction = direction.clone().multiply(1.5)
        skull.isCharged = false

        spawnSkullTrail(skull)
        player.playSound(CAST_SOUND)
    }

    /**
     * Spawns a repeating particle trail behind [skull] every tick until the entity is no longer valid.
     * Emits [Particle.SOUL] and [Particle.ASH] at the skull's current location.
     *
     * @param skull The [WitherSkull] to trail.
     */
    private fun spawnSkullTrail(skull: WitherSkull) {
        Utils.ScheduleUtils.spawnProjectileTrail(skull) {
            Particle.SOUL
                .builder()
                .location(it)
                .count(3)
                .offset(0.05, 0.05, 0.05)
                .spawn()
            Particle.ASH
                .builder()
                .location(it)
                .count(5)
                .offset(0.1, 0.1, 0.1)
                .spawn()
        }
    }
}
