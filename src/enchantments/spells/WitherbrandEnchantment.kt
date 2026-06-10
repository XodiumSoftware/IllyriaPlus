package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import org.bukkit.entity.WitherSkull
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Enchantment.displayName

/** Represents an object handling witherbrand enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object WitherbrandEnchantment : SpellEnchantmentInterface {
    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.wither.shoot"), Sound.Source.HOSTILE, 1.0f, 1.0f)

    override val cooldown: Long = 120L
    override val categoryCooldown: Long = 60L
    override val castDelay: Long = 4L
    override val category: SpellCategory = SpellCategory.PROJECTILE

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
        Utils.Schedule.spawnProjectileTrail(skull) {
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
