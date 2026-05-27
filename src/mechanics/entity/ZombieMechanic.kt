package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.random.Random

/** Represents a mechanic handling zombie behavior and drops within the system. */
internal object ZombieMechanic : MechanicInterface {
    private const val HORDE_RADIUS: Double = 96.0
    private const val SPAWN_AMPLIFY_CHANCE: Double = 0.25
    private const val SPAWN_AMPLIFY_EXTRA: Int = 2
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
                            "<white>Zombies can break wooden doors.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Amplified Spawns</yellow> <firewatch>></gradient> <white>Natural spawns have a " +
                            "${(SPAWN_AMPLIFY_CHANCE * 100).toInt()}% chance to bring up to " +
                            "$SPAWN_AMPLIFY_EXTRA extra zombies.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Infectious Touch</yellow> <firewatch>></gradient> " +
                            "<white>Zombie hits inflict slowness, hunger, and weakness.</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Daylight Immunity</yellow> <firewatch>></gradient> " +
                            "<white>Zombies do not burn in sunlight.</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.ENTITY

    @EventHandler
    fun on(event: EntityTargetLivingEntityEvent) = alertHorde(event)

    @EventHandler
    fun on(event: EntityDamageByEntityEvent) = infectiousTouch(event)

    @EventHandler
    fun on(event: EntityCombustEvent) {
        if (event.entity !is Zombie) return
        if (event is EntityCombustByBlockEvent || event is EntityCombustByEntityEvent) return

        event.isCancelled = true
    }

    @EventHandler
    fun on(event: CreatureSpawnEvent) {
        val zombie = event.entity as? Zombie ?: return

        zombie.setCanBreakDoors(CAN_BREAK_DOOR)
        amplifySpawn(event)
    }

    /**
     * Chance to spawn extra zombies around a naturally spawned zombie.
     *
     * @param event The CreatureSpawnEvent triggered when an entity spawns.
     */
    private fun amplifySpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) return
        if (Random.nextDouble() > SPAWN_AMPLIFY_CHANCE) return

        repeat(Random.nextInt(1, SPAWN_AMPLIFY_EXTRA + 1)) {
            val loc =
                event.entity.location.add(
                    (Random.nextDouble() - 0.5) * 2 * SPAWN_AMPLIFY_RADIUS,
                    0.0,
                    (Random.nextDouble() - 0.5) * 2 * SPAWN_AMPLIFY_RADIUS,
                )
            event.entity.world.spawn(loc, Zombie::class.java) { it.setCanBreakDoors(CAN_BREAK_DOOR) }
        }
    }

    /**
     * Inflicts slowness, hunger, and weakness on a player hit by a zombie.
     *
     * @param event The EntityDamageByEntityEvent triggered when an entity damages another.
     */
    private fun infectiousTouch(event: EntityDamageByEntityEvent) {
        if (event.damager !is Zombie) return

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

        zombie
            .getNearbyEntities(HORDE_RADIUS, HORDE_RADIUS, HORDE_RADIUS)
            .filterIsInstance<Zombie>()
            .filter { it.uniqueId != zombie.uniqueId && it.target != target }
            .forEach { it.target = target }
    }
}
