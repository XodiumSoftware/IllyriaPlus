package org.xodium.illyriaplus.enchantments.spells

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.EnchantmentUtils.displayName
import org.xodium.illyriaplus.Utils.EnchantmentUtils.isSelectedSpell
import org.xodium.illyriaplus.Utils.EnchantmentUtils.validateSpellCast
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.managers.XpManager
import java.util.*

/** Represents an object handling voidpull enchantment implementation within the system. */
@Suppress("UnstableApiUsage")
internal object VoidpullEnchantment : EnchantmentInterface {
    private const val XP_COST = 3
    private const val VELOCITY = 2.5
    private const val MAX_DISTANCE = 30.0

    private val TRAIL_TASKS: MutableMap<UUID, BukkitTask> = mutableMapOf()
    private val ORIGINS: MutableMap<UUID, Location> = mutableMapOf()

    private val CAST_SOUND: Sound = Sound.sound(Key.key("entity.ender_pearl.throw"), Sound.Source.PLAYER, 1.0f, 1.0f)
    private val PULL_SOUND: Sound = Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.HOSTILE, 1.0f, 0.8f)

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
     * Handles player interaction for casting Voidpull.
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
        val pearl = player.world.spawn(spawnLocation, EnderPearl::class.java)

        pearl.setGravity(false)
        pearl.velocity = direction.multiply(VELOCITY)
        pearl.shooter = player

        ORIGINS[pearl.uniqueId] = spawnLocation.clone()
        TRAIL_TASKS[pearl.uniqueId] = spawnPearlTrail(pearl)

        player.playSound(CAST_SOUND)
    }

    @EventHandler
    fun on(event: ProjectileHitEvent) {
        val projectile = event.entity
        val player = projectile.shooter as? Player ?: return

        if (!TRAIL_TASKS.containsKey(projectile.uniqueId)) return

        TRAIL_TASKS.remove(projectile.uniqueId)?.cancel()
        ORIGINS.remove(projectile.uniqueId)

        val target = event.hitEntity

        if (target == null) {
            projectile.remove()
            return
        }

        val destination =
            player.location
                .clone()
                .add(
                    player.location.direction
                        .normalize()
                        .multiply(2.0),
                ).apply { y = player.location.y }

        Particle.PORTAL
            .builder()
            .location(target.location.add(0.0, 1.0, 0.0))
            .count(30)
            .offset(0.3, 0.5, 0.3)
            .spawn()

        target.teleport(destination)
        projectile.remove()

        Particle.PORTAL
            .builder()
            .location(destination.add(0.0, 1.0, 0.0))
            .count(30)
            .offset(0.3, 0.5, 0.3)
            .spawn()

        player.playSound(PULL_SOUND)
    }

    /**
     * Spawns a repeating PORTAL particle trail behind [pearl] every tick until the entity is no longer valid.
     *
     * @param pearl The [EnderPearl] to trail.
     * @return The [BukkitTask] running the trail, so it can be cancelled early on hit.
     */
    private fun spawnPearlTrail(pearl: EnderPearl) =
        Utils.ScheduleUtils.spawnProjectileTrail(pearl) {
            val origin = ORIGINS[pearl.uniqueId] ?: return@spawnProjectileTrail

            if (it.distanceSquared(origin) > MAX_DISTANCE * MAX_DISTANCE) {
                pearl.remove()
                TRAIL_TASKS.remove(pearl.uniqueId)?.cancel()
                ORIGINS.remove(pearl.uniqueId)
                return@spawnProjectileTrail
            }

            Particle.PORTAL
                .builder()
                .location(it)
                .count(8)
                .offset(0.1, 0.1, 0.1)
                .spawn()
            Particle.REVERSE_PORTAL
                .builder()
                .location(it)
                .count(3)
                .offset(0.05, 0.05, 0.05)
                .spawn()
        }
}
