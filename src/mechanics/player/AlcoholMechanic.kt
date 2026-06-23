package org.xodium.illyriaplus.mechanics.player

import org.bukkit.NamespacedKey
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.Schedule.schedule
import org.xodium.illyriaplus.damagetypes.AlcoholDamageType
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.pdcs.PlayerPDC.intoxication
import org.xodium.illyriaplus.pdcs.PlayerPDC.lastDrinkTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Tracks alcoholic drink consumption and applies escalating effects to [Player]s. */
internal object AlcoholMechanic : MechanicInterface {
    private const val BLINDNESS_THRESHOLD = 4
    private const val DAMAGE_THRESHOLD = 8
    private const val DECAY_AMOUNT = 1

    private val DECAY_INTERVAL = 5.minutes
    private val RESET_TIMEOUT = 15.minutes
    private val DAMAGE_INTERVAL = 3.seconds
    private val BLINDNESS_DURATION = 15.seconds
    private val trackedPlayers = mutableSetOf<Player>()

    val ALCOHOL_STRENGTH_KEY = NamespacedKey(instance, "alcohol_strength")

    override fun register(): Long {
        schedule(period = DECAY_INTERVAL.inWholeSeconds * 20L) { trackedPlayers.toList().forEach { applyDecay(it) } }
        schedule(period = DAMAGE_INTERVAL.inWholeSeconds * 20L) { trackedPlayers.toList().forEach { applyDamage(it) } }
        return super.register()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun on(event: PlayerItemConsumeEvent) = handleDrink(event.player, event.item)

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PlayerJoinEvent) {
        if (event.player.intoxication > 0) {
            trackedPlayers.add(event.player)
            applyEffects(event.player)
        }
    }

    /**
     * Handles a consumed item, adding intoxication if it carries an alcohol strength value.
     *
     * @param player The player who consumed the item.
     * @param item The consumed [ItemStack].
     */
    private fun handleDrink(
        player: Player,
        item: ItemStack,
    ) {
        player.intoxication +=
            item.persistentDataContainer.get(ALCOHOL_STRENGTH_KEY, PersistentDataType.INTEGER) ?: return
        player.lastDrinkTime = System.currentTimeMillis()
        trackedPlayers.add(player)
        player.sendActionBar(MM.deserialize("<yellow>Intoxication: <white>${player.intoxication}"))
        applyEffects(player)
    }

    /**
     * Applies or extends effects based on current intoxication level.
     *
     * @param player The player to affect.
     */
    private fun applyEffects(player: Player) {
        val level = player.intoxication

        when {
            level >= DAMAGE_THRESHOLD -> {
                player.sendMessage(MM.deserialize("<red>You are heavily intoxicated and taking damage!"))
            }

            level >= BLINDNESS_THRESHOLD -> {
                player.sendMessage(MM.deserialize("<gold>You are feeling tipsy and your vision blurs..."))
            }

            level >= 1 -> {
                player.sendMessage(MM.deserialize("<yellow>You feel a buzz from the drink."))
            }
        }

        if (level >= BLINDNESS_THRESHOLD) {
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.BLINDNESS,
                    BLINDNESS_DURATION.inWholeSeconds.toInt() * 20,
                    0,
                    false,
                    true,
                    true,
                ),
            )
        }
    }

    /**
     * Decays intoxication naturally over time and resets after inactivity.
     *
     * @param player The player whose intoxication should decay.
     */
    private fun applyDecay(player: Player) {
        if (!player.isOnline) {
            trackedPlayers.remove(player)
            return
        }

        val now = System.currentTimeMillis()
        val lastDrink = player.lastDrinkTime

        if (lastDrink == 0L) {
            trackedPlayers.remove(player)
            return
        }
        if (now - lastDrink >= RESET_TIMEOUT.inWholeMilliseconds) {
            reset(player)
            return
        }

        player.intoxication = (player.intoxication - DECAY_AMOUNT).coerceAtLeast(0)

        if (player.intoxication <= 0) {
            reset(player)
            return
        }
    }

    /**
     * Deals periodic damage to players above the damage threshold.
     *
     * @param player The player to damage.
     */
    private fun applyDamage(player: Player) {
        if (!player.isOnline) {
            trackedPlayers.remove(player)
            return
        }
        if (player.intoxication >= DAMAGE_THRESHOLD) {
            player.damage(1.0, DamageSource.builder(AlcoholDamageType.get()).build())
        }
    }

    /**
     * Clears intoxication tracking and effects from a player.
     *
     * @param player The player to reset.
     */
    private fun reset(player: Player) {
        player.intoxication = 0
        player.lastDrinkTime = 0L
        player.sendMessage(MM.deserialize("<green>You sobered up."))
        trackedPlayers.remove(player)
    }
}
