package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import org.bukkit.entity.SmallFireball
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import kotlin.uuid.ExperimentalUuidApi

/** Represents an object handling inferno enchantment implementation within the system. */
@OptIn(ExperimentalUuidApi::class)
@Suppress("UnstableApiUsage")
internal object InfernoEnchantment : SpellEnchantmentInterface {
    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.blaze.shoot"), Sound.Source.HOSTILE, 1.0f, 1.0f)

    override val cooldown: Long = 60L
    override val categoryCooldown: Long = 60L
    override val castDelay: Long = 0L
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
        val fireball = player.world.spawn(spawnLocation, SmallFireball::class.java)

        fireball.shooter = player
        fireball.direction = direction.clone().multiply(1.5)
        fireball.yield = 0.0f

        spawnFireballTrail(fireball)
        player.playSound(CAST_SOUND)
    }

    /**
     * Spawns a repeating particle trail behind [fireball] every tick until the entity is no longer valid.
     * Emits [Particle.FLAME] and [Particle.LAVA] at the fireball's current location.
     *
     * @param fireball The [SmallFireball] to trail.
     */
    private fun spawnFireballTrail(fireball: SmallFireball) =
        Utils.Schedule.spawnProjectileTrail(fireball) {
            Particle.FLAME
                .builder()
                .location(it)
                .count(5)
                .offset(0.05, 0.05, 0.05)
                .spawn()
            Particle.LAVA
                .builder()
                .location(it)
                .count(1)
                .spawn()
        }
}
