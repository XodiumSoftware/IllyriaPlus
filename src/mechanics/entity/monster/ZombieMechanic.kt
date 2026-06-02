package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Monster.trySpawnMount
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.util.*

/** Represents a mechanic handling zombie behavior and drops within the system. */
internal object ZombieMechanic : MonsterInterface {
    private const val HORDE_RADIUS: Double = 96.0
    private const val HORDE_COOLDOWN_TICKS: Long = 100
    private const val CAN_BREAK_DOOR: Boolean = true
    private const val SHOULD_BURN_IN_DAY: Boolean = false
    private const val ZOMBIE_HORSE_CHANCE: Int = 5

    private val infectiousEffects: List<() -> PotionEffect> =
        listOf(
            { PotionEffect(PotionEffectType.SLOWNESS, (60..100).random(), 0, false, true, true) },
            { PotionEffect(PotionEffectType.HUNGER, (80..140).random(), 0, false, true, true) },
            { PotionEffect(PotionEffectType.WEAKNESS, (100..180).random(), 0, false, true, true) },
        )
    private val hordeCooldowns: MutableSet<UUID> = mutableSetOf()

    override val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.ATTACK_KNOCKBACK to { _, attr -> attr.baseValue = (5..10).random() / 10.0 },
            Attribute.KNOCKBACK_RESISTANCE to { _, attr -> attr.baseValue = (3..7).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (2..6).random().toDouble() },
            Attribute.ARMOR_TOUGHNESS to { _, attr -> attr.baseValue = (1..3).random().toDouble() },
            Attribute.SPAWN_REINFORCEMENTS to { _, attr -> attr.baseValue = 5.0 },
        )
    override val horseAttributes: Map<Attribute, (AbstractHorse, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (12..16).random() / 10.0 },
            Attribute.JUMP_STRENGTH to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (2..5).random().toDouble() },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(Utils.MM.deserialize("<mango>Zombie Mechanics</gradient>"))
                .addLoreLines(
                    Utils.MM.deserialize(""),
                    Utils.MM.deserialize(
                        "<yellow>Horde Alert</yellow> <firewatch>></gradient> <white>When a zombie spots a player, " +
                            "it alerts nearby zombies within $HORDE_RADIUS blocks to join the chase.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Door Breaking</yellow> <firewatch>></gradient> " +
                            "<white>Zombies can break wooden doors on Hard difficulty.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Undead Cavalry</yellow> <firewatch>></gradient> " +
                            "<white>Zombies have a $ZOMBIE_HORSE_CHANCE% chance to spawn riding zombie horses.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Attribute Modifiers</yellow> <firewatch>></gradient> " +
                            "<white>+0.5-1.0 KB, +30-70% KB resist, " +
                            "+2-6 armor, +1-3 toughness, +500% reinforcements.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Infectious Touch</yellow> <firewatch>></gradient> " +
                            "<white>Zombie melee hits inflict slowness, hunger, and weakness.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Daylight Immunity</yellow> <firewatch>></gradient> " +
                            "<white>Zombies do not burn in sunlight.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityTargetLivingEntityEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> alertHorde(event.entity as? Zombie ?: return, event.target as? Player ?: return)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageByEntityEvent) {
        when {
            event.damager !is Zombie -> return
            event.entity.world.difficulty != difficulty -> return
            event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK -> return
            else -> infectiousTouch(event.entity as? Player ?: return)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Zombie ?: return)
        }
    }

    override fun modifySpawn(monster: Monster) {
        super.modifySpawn(monster)
        (monster as Zombie).apply {
            setCanBreakDoors(CAN_BREAK_DOOR)
            setShouldBurnInDay(SHOULD_BURN_IN_DAY)
            trySpawnMount<ZombieHorse>(ZOMBIE_HORSE_CHANCE, horseAttributes)
        }
    }

    /**
     * Inflicts slowness, hunger, and weakness on a player hit by a zombie.
     *
     * @param player The player to affect.
     */
    private fun infectiousTouch(player: Player) {
        infectiousEffects.forEach { player.addPotionEffect(it()) }
    }

    /**
     * Alerts nearby zombies to join the chase when a zombie targets a player.
     *
     * @param zombie The zombie that targeted the player.
     * @param target The player being targeted.
     */
    private fun alertHorde(
        zombie: Zombie,
        target: Player,
    ) {
        if (zombie.uniqueId in hordeCooldowns) return

        hordeCooldowns.add(zombie.uniqueId)
        IllyriaPlus.instance.server.scheduler.runTaskLater(
            IllyriaPlus.instance,
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
