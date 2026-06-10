package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Enchantment.displayName

/** Represents an object handling frostbind enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object FrostbindEnchantment : SpellEnchantmentInterface {
    private val PROJECTILE_KEY by lazy { NamespacedKey(IllyriaPlus.instance, "frostbind_projectile") }
    private val LAUNCH_SOUND: Sound =
        Sound.sound(Key.key("entity.snow_golem.shoot"), Sound.Source.NEUTRAL, 1.0f, 1.2f)
    private val HIT_SOUND: Sound =
        Sound.sound(Key.key("block.powder_snow.place"), Sound.Source.BLOCK, 1.0f, 0.8f)

    override val cooldown: Long = 160L
    override val categoryCooldown: Long = 60L
    override val castDelay: Long = 6L
    override val category: SpellCategory = SpellCategory.PROJECTILE
    private const val FREEZE_TICKS: Int = 500

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
        val snowball = player.world.spawn(spawnLocation, Snowball::class.java)

        snowball.shooter = player
        snowball.setGravity(false)
        snowball.velocity = direction.multiply(2.0)
        snowball.persistentDataContainer.set(PROJECTILE_KEY, PersistentDataType.BOOLEAN, true)

        spawnSnowballTrail(snowball)
        player.playSound(LAUNCH_SOUND)
    }

    /**
     * Handles projectile hit effects for Frostbind.
     *
     * @param event The projectile hit event.
     */
    @EventHandler
    fun on(event: ProjectileHitEvent) {
        val projectile = event.entity

        if (!projectile.persistentDataContainer.has(PROJECTILE_KEY)) return

        val entity = event.hitEntity ?: return

        entity.freezeTicks = FREEZE_TICKS

        Particle.SNOWFLAKE
            .builder()
            .location(entity.location.add(0.0, 1.0, 0.0))
            .count(20)
            .offset(0.3, 0.5, 0.3)
            .spawn()

        entity.world.players
            .filter { it.location.distance(entity.location) <= 16 }
            .forEach { it.playSound(HIT_SOUND) }
    }

    /**
     * Spawns a repeating particle trail behind a snowball.
     *
     * @param snowball The projectile to trail.
     */
    private fun spawnSnowballTrail(snowball: Snowball) =
        Utils.Schedule.spawnProjectileTrail(snowball) {
            Particle.SNOWFLAKE
                .builder()
                .location(it)
                .count(5)
                .offset(0.05, 0.05, 0.05)
                .spawn()

            Particle.ITEM_SNOWBALL
                .builder()
                .location(it)
                .count(2)
                .offset(0.05, 0.05, 0.05)
                .spawn()
        }
}
