package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.util.Vector
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Represents an object handling quake enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object QuakeEnchantment : SpellEnchantmentInterface {
    private const val RADIUS = 4.0
    private const val DAMAGE = 6.0
    private const val KNOCKBACK_STRENGTH = 1.2
    private const val RING_COUNT = 3

    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.warden.attack_impact"), Sound.Source.BLOCK, 1.0f, 0.8f)
    private val HIT_SOUND: Sound = Sound.sound(Key.key("block.anvil.land"), Sound.Source.BLOCK, 0.6f, 0.5f)

    override val cooldown: Long = 240L
    override val categoryCooldown: Long = 120L
    override val castDelay: Long = 12L
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
        val location = player.location
        val world = player.world

        for (ring in 1..RING_COUNT) {
            val ringRadius = RADIUS * ring / RING_COUNT
            val particleCount = (ringRadius * 16).toInt()

            for (i in 0 until particleCount) {
                val angle = 2 * PI * i / particleCount
                val x = location.x + ringRadius * cos(angle)
                val z = location.z + ringRadius * sin(angle)
                val y = location.y + 0.1
                val particleLoc = Location(world, x, y, z)

                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.05, 0.0, 0.05, 0.01)
                world.spawnParticle(Particle.CRIT, particleLoc.add(0.0, 0.2, 0.0), 1, 0.05, 0.05, 0.05, 0.0)
            }
        }

        world
            .getNearbyEntities(location, RADIUS, RADIUS / 2, RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach {
                val direction =
                    it.location
                        .toVector()
                        .subtract(location.toVector())
                        .normalize()
                val upward = Vector(0.0, 0.4, 0.0)
                val knockback = direction.multiply(KNOCKBACK_STRENGTH).add(upward)

                it.velocity = knockback
                it.damage(DAMAGE, player)

                Particle.CRIT
                    .builder()
                    .location(it.location.add(0.0, 1.0, 0.0))
                    .count(8)
                    .offset(0.3, 0.5, 0.3)
                    .spawn()
            }

        world.spawnParticle(
            Particle.CAMPFIRE_COSY_SMOKE,
            location.clone().add(0.0, 0.1, 0.0),
            20,
            RADIUS / 2,
            0.1,
            RADIUS / 2,
            0.05,
        )

        world.players
            .filter { it.location.distance(location) <= 32 }
            .forEach {
                it.playSound(CAST_SOUND)
                it.playSound(HIT_SOUND)
            }
    }
}
