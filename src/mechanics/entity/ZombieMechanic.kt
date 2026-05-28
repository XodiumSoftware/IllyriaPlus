package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
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

/** Represents a mechanic handling zombie behavior and drops within the system. */
internal object ZombieMechanic : MechanicInterface {
    private const val HORDE_RADIUS: Double = 96.0
    private const val HORDE_COOLDOWN_TICKS: Long = 100
    private const val CAN_BREAK_DOOR: Boolean = true

    private val attributes: Map<Attribute, (Zombie, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.MAX_HEALTH to { zombie, attr ->
                attr.baseValue *= (15..20).random() / 10.0
                zombie.health = attr.value
            },
            Attribute.ATTACK_DAMAGE to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.FOLLOW_RANGE to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.ATTACK_KNOCKBACK to { _, attr -> attr.baseValue = (5..10).random() / 10.0 },
            Attribute.KNOCKBACK_RESISTANCE to { _, attr -> attr.baseValue = (3..7).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (2..6).random().toDouble() },
            Attribute.ARMOR_TOUGHNESS to { _, attr -> attr.baseValue = (1..3).random().toDouble() },
            Attribute.SPAWN_REINFORCEMENTS to { _, attr -> attr.baseValue = 5.0 },
            Attribute.SCALE to { _, attr -> attr.baseValue *= (10..13).random() / 10.0 },
        )
    private val infectiousEffects: List<() -> PotionEffect> =
        listOf(
            { PotionEffect(PotionEffectType.SLOWNESS, (60..100).random(), 0, false, true, true) },
            { PotionEffect(PotionEffectType.HUNGER, (80..140).random(), 0, false, true, true) },
            { PotionEffect(PotionEffectType.WEAKNESS, (100..180).random(), 0, false, true, true) },
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
                        "<yellow>Attribute Modifiers</yellow> <firewatch>></gradient> " +
                            "<white>+30-70% speed/health/dmg/range, +0.5-1.0 KB, +30-70% KB resist, " +
                            "+2-6 armor, +1-3 toughness, +500% reinforcements, +0-30% size.</white>",
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
    fun on(event: EntityCombustEvent) = daylightImmunity(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) = modifyZombieSpawn(event)

    /**
     * Prevents zombies from burning in sunlight on Hard difficulty.
     *
     * @param event The EntityCombustEvent triggered when an entity combusts.
     */
    private fun daylightImmunity(event: EntityCombustEvent) {
        when {
            event.entity !is Zombie -> return
            event.entity.world.difficulty != Difficulty.HARD -> return
            event is EntityCombustByBlockEvent || event is EntityCombustByEntityEvent -> return
            else -> event.isCancelled = true
        }
    }

    /**
     * Modifies a zombie's base speed and enables door-breaking on Hard difficulty.
     *
     * @param event The CreatureSpawnEvent triggered when an entity spawns.
     */
    private fun modifyZombieSpawn(event: CreatureSpawnEvent) {
        val zombie = event.entity as? Zombie ?: return

        if (event.entity.world.difficulty != Difficulty.HARD) return

        zombie.setCanBreakDoors(CAN_BREAK_DOOR)
        attributes.forEach { (attribute, apply) ->
            zombie.getAttribute(attribute)?.let { apply(zombie, it) }
        }
    }

    /**
     * Inflicts slowness, hunger, and weakness on a player hit by a zombie.
     *
     * @param event The EntityDamageByEntityEvent triggered when an entity damages another.
     */
    private fun infectiousTouch(event: EntityDamageByEntityEvent) {
        if (event.damager !is Zombie) return
        if (event.entity.world.difficulty != Difficulty.HARD) return
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return

        val player = event.entity as? Player ?: return

        infectiousEffects.forEach { player.addPotionEffect(it()) }
    }

    /**
     * Alerts nearby zombies to join the chase when a zombie targets a player.
     *
     * @param event The EntityTargetLivingEntityEvent triggered when an entity targets another.
     */
    private fun alertHorde(event: EntityTargetLivingEntityEvent) {
        val zombie = event.entity as? Zombie ?: return
        val target = event.target as? Player ?: return

        if (event.entity.world.difficulty != Difficulty.HARD) return
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
