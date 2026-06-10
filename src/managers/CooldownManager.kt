package org.xodium.illyriaplus.managers

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.enchantments.spells.SpellCategory
import org.xodium.illyriaplus.enchantments.spells.SpellEnchantmentInterface
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages spell cooldowns, shared category cooldowns, global cooldown,
 * and cast delays for spell enchantments.
 */
internal object CooldownManager {
    private const val COOLDOWN_MSG = "<firewatch>Cooldown: <time>s</gradient>"
    private const val CASTING_MSG = "<spellbite>Casting...</gradient>"
    private const val INTERRUPT_MSG = "<red>Cast interrupted!</red>"

    private val COOLDOWN_SOUND: Sound =
        Sound.sound(Key.key("block.beacon.deactivate"), Sound.Source.PLAYER, 0.6f, 1.0f)
    private val INTERRUPT_SOUND: Sound =
        Sound.sound(Key.key("entity.item.break"), Sound.Source.PLAYER, 0.5f, 1.0f)

    /** Global cooldown applied after any spell cast, in ticks. */
    const val GCD_TICKS: Long = 8L

    private val globalCooldowns = ConcurrentHashMap<UUID, Long>()
    private val categoryCooldowns = ConcurrentHashMap<UUID, MutableMap<SpellCategory, Long>>()
    private val spellCooldowns = ConcurrentHashMap<UUID, MutableMap<String, Long>>()
    private val activeCasts = ConcurrentHashMap<UUID, BukkitTask>()

    /**
     * Checks if the player is on cooldown for the given spell.
     * Considers global, category, and individual spell cooldowns.
     */
    fun isOnCooldown(
        player: Player,
        spell: SpellEnchantmentInterface,
    ): Boolean {
        val now = System.currentTimeMillis()

        if ((globalCooldowns[player.uniqueId] ?: 0) > now) return true
        if ((categoryCooldowns[player.uniqueId]?.get(spell.category) ?: 0) > now) return true
        if ((spellCooldowns[player.uniqueId]?.get(spell.spellKey) ?: 0) > now) return true

        return false
    }

    /**
     * Gets the remaining cooldown time in milliseconds for the given spell.
     */
    fun getRemainingMs(
        player: Player,
        spell: SpellEnchantmentInterface,
    ): Long {
        val now = System.currentTimeMillis()
        val gcd = globalCooldowns[player.uniqueId] ?: 0
        val cat = categoryCooldowns[player.uniqueId]?.get(spell.category) ?: 0
        val sp = spellCooldowns[player.uniqueId]?.get(spell.spellKey) ?: 0

        return maxOf(gcd, cat, sp) - now
    }

    /**
     * Starts all relevant cooldowns after a successful spell cast.
     */
    fun startCooldowns(
        player: Player,
        spell: SpellEnchantmentInterface,
    ) {
        val now = System.currentTimeMillis()

        globalCooldowns[player.uniqueId] = now + (GCD_TICKS * 50)
        categoryCooldowns
            .computeIfAbsent(player.uniqueId) { EnumMap(SpellCategory::class.java) }[spell.category] =
            now + (spell.categoryCooldown * 50)
        spellCooldowns
            .computeIfAbsent(player.uniqueId) { mutableMapOf() }[spell.spellKey] =
            now + (spell.cooldown * 50)
    }

    /** Returns whether the player is currently in a cast delay. */
    fun isCasting(player: Player): Boolean = activeCasts.containsKey(player.uniqueId)

    /** Interrupts an active cast for the player, if any. */
    fun interruptCast(player: Player) {
        val task = activeCasts.remove(player.uniqueId)

        if (task != null) {
            task.cancel()
            if (player.isOnline) {
                player.sendActionBar(MM.deserialize(INTERRUPT_MSG))
                player.playSound(INTERRUPT_SOUND)
            }
        }
    }

    /**
     * Starts a cast delay for the player.
     *
     * @param player The player casting.
     * @param delayTicks Ticks to wait before completing.
     * @param onComplete Called when the cast delay finishes successfully.
     * @return The scheduled [BukkitTask].
     */
    fun startCast(
        player: Player,
        delayTicks: Long,
        onComplete: () -> Unit,
    ): BukkitTask {
        activeCasts.remove(player.uniqueId)?.cancel()

        var elapsed = 0L

        val task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (!player.isOnline) {
                        activeCasts.remove(player.uniqueId)?.cancel()
                        return@Runnable
                    }
                    elapsed++
                    player.sendActionBar(MM.deserialize(CASTING_MSG))
                    val handLoc =
                        player.eyeLocation.clone().add(
                            player.location.direction
                                .normalize()
                                .multiply(0.6),
                        )
                    Particle.ENCHANT
                        .builder()
                        .location(handLoc)
                        .count(2)
                        .offset(0.1, 0.1, 0.1)
                        .spawn()

                    if (elapsed >= delayTicks) {
                        activeCasts.remove(player.uniqueId)?.cancel()
                        onComplete()
                    }
                },
                0L,
                1L,
            )

        activeCasts[player.uniqueId] = task

        return task
    }

    /**
     * Notifies the player that their spell is on cooldown,
     * showing the remaining time in seconds.
     */
    fun notifyCooldown(
        player: Player,
        spell: SpellEnchantmentInterface,
    ) {
        val remaining = getRemainingMs(player, spell) / 1000.0

        player.sendActionBar(
            MM.deserialize(
                COOLDOWN_MSG,
                Placeholder.unparsed("time", String.format("%.1f", remaining)),
            ),
        )
        player.playSound(COOLDOWN_SOUND)
    }
}
