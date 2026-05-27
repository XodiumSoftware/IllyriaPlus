package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.util.*
import kotlin.random.Random

/** Represents a mechanic handling zombie behavior and drops within the system. */
internal object ZombieMechanic : MechanicInterface {
    private const val HORDE_RADIUS: Double = 96.0
    private const val HORDE_COOLDOWN_TICKS: Long = 100
    private const val SPAWN_AMPLIFY_CHANCE: Double = 0.25
    private const val MAX_EXTRA_ZOMBIES: Int = 2
    private const val SPAWN_AMPLIFY_RADIUS: Double = 4.0
    private const val INFECT_DURATION_TICKS: Int = 100
    private const val INFECT_AMPLIFIER: Int = 0
    private const val CAN_BREAK_DOOR: Boolean = true

    private val INFECTIOUS_EFFECTS: List<PotionEffectType> =
        listOf(
            PotionEffectType.SLOWNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.WEAKNESS,
        )

    private val hordeCooldowns: MutableSet<UUID> = mutableSetOf()

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(MM.deserialize("<mango>Zombie Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Horde Alert</yellow> <firewatch>></gradient> <white>When a zombie spots a player, " +
                            "it alerts nearby zombies within $HORDE_RADIUS blocks to join the chase.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Door Breaking</yellow> <firewatch>></gradient> " +
                            "<white>Zombies can break wooden doors on Hard difficulty.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Amplified Spawns</yellow> <firewatch>></gradient> <white>Natural spawns have a " +
                            "${(SPAWN_AMPLIFY_CHANCE * 100).toInt()}% chance to bring up to " +
                            "$MAX_EXTRA_ZOMBIES extra zombies on Hard difficulty.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Infectious Touch</yellow> <firewatch>></gradient> " +
                            "<white>Zombie melee hits inflict slowness, hunger, and weakness.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Daylight Immunity</yellow> <firewatch>></gradient> " +
                            "<white>Zombies do not burn in sunlight.</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.ENTITY

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityTargetLivingEntityEvent) = alertHorde(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageByEntityEvent) = infectiousTouch(event)

    @EventHandler
    fun on(event: EntityCombustEvent) {
        if (event.entity !is Zombie) return
        if (event is EntityCombustByBlockEvent || event is EntityCombustByEntityEvent) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        val zombie = event.entity as? Zombie ?: return

        if (event.entity.world.difficulty == Difficulty.HARD) zombie.setCanBreakDoors(CAN_BREAK_DOOR)

        amplifySpawn(event)
    }

    /**
     * Chance to spawn extra zombies around a naturally spawned zombie.
     *
     * @param event The CreatureSpawnEvent triggered when an entity spawns.
     */
    private fun amplifySpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) return
        if (event.entity.world.difficulty != Difficulty.HARD) return
        if (Random.nextDouble() > SPAWN_AMPLIFY_CHANCE) return

        val parent = event.entity as? Zombie ?: return

        repeat(Random.nextInt(1, MAX_EXTRA_ZOMBIES + 1)) {
            val loc =
                event.entity.location.add(
                    (Random.nextDouble() - 0.5) * 2 * SPAWN_AMPLIFY_RADIUS,
                    0.0,
                    (Random.nextDouble() - 0.5) * 2 * SPAWN_AMPLIFY_RADIUS,
                )

            event.entity.world.spawn(loc, Zombie::class.java) { child ->
                child.setCanBreakDoors(CAN_BREAK_DOOR)
                parent.getAttribute(Attribute.MAX_HEALTH)?.value?.let { maxHealth ->
                    child.getAttribute(Attribute.MAX_HEALTH)?.baseValue = maxHealth
                    child.health = maxHealth
                }

                if (parent.customName() != null) {
                    child.customName(parent.customName())
                    child.isCustomNameVisible = parent.isCustomNameVisible
                }

                parent.equipment.let { parentEquip ->
                    child.equipment.let { childEquip ->
                        childEquip.setHelmet(parentEquip.helmet.clone())
                        childEquip.setChestplate(parentEquip.chestplate.clone())
                        childEquip.setLeggings(parentEquip.leggings.clone())
                        childEquip.setBoots(parentEquip.boots.clone())
                        childEquip.setItemInMainHand(parentEquip.itemInMainHand.clone())
                        childEquip.setItemInOffHand(parentEquip.itemInOffHand.clone())
                    }
                }
            }
        }
    }

    /**
     * Inflicts slowness, hunger, and weakness on a player hit by a zombie.
     *
     * @param event The EntityDamageByEntityEvent triggered when an entity damages another.
     */
    private fun infectiousTouch(event: EntityDamageByEntityEvent) {
        if (event.damager !is Zombie) return
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return

        val player = event.entity as? Player ?: return

        INFECTIOUS_EFFECTS.forEach {
            player.addPotionEffect(PotionEffect(it, INFECT_DURATION_TICKS, INFECT_AMPLIFIER, false, true, true))
        }
    }

    /**
     * Alerts nearby zombies to join the chase when a zombie targets a player.
     *
     * @param event The EntityTargetLivingEntityEvent triggered when an entity targets another.
     */
    private fun alertHorde(event: EntityTargetLivingEntityEvent) {
        val zombie = event.entity as? Zombie ?: return
        val target = event.target as? Player ?: return

        if (zombie.uniqueId in hordeCooldowns) return

        hordeCooldowns.add(zombie.uniqueId)
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable { hordeCooldowns.remove(zombie.uniqueId) },
            HORDE_COOLDOWN_TICKS,
        )

        zombie
            .getNearbyEntities(HORDE_RADIUS, HORDE_RADIUS, HORDE_RADIUS)
            .filterIsInstance<Zombie>()
            .filter { it.uniqueId != zombie.uniqueId && it.target != target }
            .forEach { it.target = target }
    }
}
