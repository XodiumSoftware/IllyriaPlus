package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * EarthGlove: sheath the caster's fist in compacted earth, empowering their next strike.
 *
 * When activated, chunks of stone and dust cling to the caster's dominant hand for
 * [WINDOW_TICKS]. The next melee hit they land during the window is massively amplified,
 * dealing bonus damage, knocking the target backwards, and briefly applying Mining
 * Fatigue so the victim's hands feel like clay.
 *
 * Window duration, bonus damage, and knockback scale with level.
 */
internal object EarthGlove : AbilityInterface {
    override val cooldownMs: Long = 6500

    /** How long the glove persists if no hit lands, in ticks. */
    private const val WINDOW_TICKS = 100L

    /** Additional window ticks per level. */
    private const val WINDOW_PER_LEVEL = 20L

    /** Bonus damage in hearts applied to the empowered melee hit (before level scaling). */
    private const val BONUS_DAMAGE = 2.0

    /** Additional bonus damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.4

    /** Horizontal knockback applied on the empowered hit (before level scaling). */
    private const val KNOCKBACK = 0.9

    /** Additional knockback per level. */
    private const val KNOCKBACK_PER_LEVEL = 0.15

    /** Vertical knockback component applied on the empowered hit. */
    private const val KNOCKUP = 0.25

    /** Mining Fatigue duration in ticks applied to the struck victim. */
    private const val FATIGUE_TICKS = 60

    /** Mining Fatigue amplifier (1 = Mining Fatigue II). */
    private const val FATIGUE_AMPLIFIER = 1

    /** Active gloves keyed by player UUID. */
    private val active = ConcurrentHashMap<UUID, Glove>()

    /** Holds state for a single EarthGlove arming. */
    private class Glove(
        val player: Player,
        val level: Int,
        var remainingTicks: Long,
        var task: BukkitTask? = null,
        var listener: Listener? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (active.containsKey(uuid)) return

        val window = WINDOW_TICKS + (level - 1) * WINDOW_PER_LEVEL
        val glove = Glove(player, level, window)
        active[uuid] = glove

        val world = player.world
        val hand = player.location.clone().add(0.0, 1.1, 0.0)
        world.spawnParticle(
            Particle.BLOCK,
            hand,
            30,
            0.3,
            0.2,
            0.3,
            0.04,
            Material.COBBLESTONE.createBlockData(),
        )
        world.playSound(player.location, Sound.BLOCK_STONE_PLACE, 1.0f, 0.9f)

        // Listen for the next melee hit and detonate the glove on contact.
        val listener =
            object : Listener {
                @EventHandler
                fun on(event: EntityDamageByEntityEvent) {
                    val damager = event.damager
                    if (damager != player) return
                    val victim = event.entity as? LivingEntity ?: return

                    event.damage += BONUS_DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
                    val away =
                        victim
                            .location
                            .toVector()
                            .subtract(player.location.toVector())
                            .setY(0.0)
                            .let {
                                if (it.lengthSquared() <
                                    1.0e-4
                                ) {
                                    return@let Vector(1.0, 0.0, 0.0)
                                } else {
                                    it.normalize()
                                }
                            }
                    victim.velocity =
                        victim
                            .velocity
                            .add(away.multiply(KNOCKBACK + (level - 1) * KNOCKBACK_PER_LEVEL))
                            .add(Vector(0.0, KNOCKUP, 0.0))
                    victim.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.MINING_FATIGUE,
                            FATIGUE_TICKS,
                            FATIGUE_AMPLIFIER,
                            false,
                            false,
                            true,
                        ),
                    )
                    victim.world.spawnParticle(
                        Particle.BLOCK,
                        victim.location.clone().add(0.0, 1.0, 0.0),
                        22,
                        0.4,
                        0.4,
                        0.4,
                        0.06,
                        Material.COBBLESTONE.createBlockData(),
                    )
                    victim.world.playSound(victim.location, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f)

                    remove(uuid)
                }
            }
        glove.listener = listener
        instance.server.pluginManager.registerEvents(listener, instance)

        // Count down the glove window; expire if no hit lands.
        glove.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    val current = active[uuid] ?: return@Runnable
                    current.remainingTicks -= 1L
                    if (!player.isOnline || player.isDead || current.remainingTicks <= 0L) {
                        remove(uuid)
                    }
                },
                0L,
                1L,
            )
    }

    /** Cancels and removes the glove for [uuid], unregistering its listener and task. */
    private fun remove(uuid: UUID) {
        val glove = active.remove(uuid) ?: return
        glove.task?.cancel()
        glove.listener?.let { HandlerList.unregisterAll(it) }
    }
}
