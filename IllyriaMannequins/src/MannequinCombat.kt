package org.xodium.illyriamannequins

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.owner
import java.util.UUID

/** Makes mannequins retaliate against attackers and fight their owner's targets. */
@Suppress("UnstableApiUsage")
internal object MannequinCombat : Listener {
    /** The squared distance within which a mannequin performs a melee attack. */
    private const val ATTACK_RANGE_SQUARED = 4.0

    /** The squared distance beyond which a mannequin gives up chasing its attacker. */
    private const val GIVE_UP_RANGE_SQUARED = 900.0

    /** The movement speed in blocks per tick for retaliating mannequins (player sprinting speed). */
    private const val CHASE_SPEED = 0.2806

    /** The interval in ticks at which mannequin combat updates. */
    private const val COMBAT_INTERVAL_TICKS = 2L

    /** The interval in ticks between a mannequin's attacks. */
    private const val ATTACK_COOLDOWN_TICKS = 10

    /** The unarmed melee damage dealt by a mannequin. */
    private const val FIST_DAMAGE = 1.0

    /** The current retaliation target for each mannequin, keyed by entity UUID. */
    private val targets = mutableMapOf<UUID, LivingEntity>()

    /** The ticks since each mannequin's last attack, keyed by entity UUID. */
    private val cooldowns = mutableMapOf<UUID, Int>()

    /**
     * Checks if a mannequin is currently engaged in combat.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if the mannequin is retaliating against an attacker.
     */
    fun isEngaged(mannequin: Mannequin): Boolean = mannequin.uniqueId in targets

    /** Registers the combat task and event listeners. */
    fun register() {
        instance.server.pluginManager.registerEvents(this, instance)
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinCombat::updateCombat,
            COMBAT_INTERVAL_TICKS,
            COMBAT_INTERVAL_TICKS,
        )
    }

    @EventHandler
    fun on(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? LivingEntity ?: return
        val victim = event.entity as? LivingEntity ?: return
        val hitMannequin = event.entity as? Mannequin
        if (hitMannequin != null && damager.uniqueId != hitMannequin.owner) {
            engage(hitMannequin, damager)
        }
        if (damager is Player) {
            engageOwnerMannequins(damager, victim)
        }
        if (victim is Player) {
            defendOwner(victim, damager)
        }
    }

    /**
     * Makes a mannequin swing its arm and deal melee damage to its target.
     *
     * @param mannequin The mannequin performing the attack.
     * @param target The entity to damage.
     */
    private fun dealDamage(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        mannequin.swingMainHand()
        target.damage(weaponDamage(mannequin.equipment.getItem(EquipmentSlot.HAND)), mannequin)
    }

    /**
     * Computes the attack damage of an item stack from its default mainhand attributes.
     *
     * @param weapon The held item.
     * @return The total attack damage, at minimum [FIST_DAMAGE].
     */
    private fun weaponDamage(weapon: ItemStack): Double {
        if (weapon.isEmpty) return FIST_DAMAGE
        val amount =
            weapon
                .type
                .asItemType()
                ?.getDefaultAttributeModifiers(EquipmentSlot.HAND)
                ?.get(Attribute.ATTACK_DAMAGE)
                ?.sumOf { it.amount } ?: 0.0
        return FIST_DAMAGE + amount
    }

    /**
     * Makes all mannequins owned by a player attack the player's victim.
     *
     * @param owner The player whose mannequins to engage.
     * @param victim The entity the player attacked.
     */
    private fun engageOwnerMannequins(
        owner: Player,
        victim: LivingEntity,
    ) {
        if (victim.uniqueId == owner.uniqueId) return
        owner
            .world
            .entities
            .filterIsInstance<Mannequin>()
            .filter { it.owner == owner.uniqueId && it.uniqueId != victim.uniqueId }
            .forEach { engage(it, victim) }
    }

    /**
     * Makes all mannequins owned by a player attack the player's attacker.
     *
     * @param owner The player whose mannequins to engage.
     * @param attacker The entity attacking the player.
     */
    private fun defendOwner(
        owner: Player,
        attacker: LivingEntity,
    ) {
        owner
            .world
            .entities
            .filterIsInstance<Mannequin>()
            .filter { it.owner == owner.uniqueId && it.uniqueId != attacker.uniqueId }
            .forEach { engage(it, attacker) }
    }

    /**
     * Assigns a combat target to a mannequin and starts its attack cooldown.
     *
     * @param mannequin The mannequin to engage.
     * @param target The entity to attack.
     */
    private fun engage(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        targets[mannequin.uniqueId] = target
        cooldowns[mannequin.uniqueId] = ATTACK_COOLDOWN_TICKS
    }

    /** Makes all retaliating mannequins chase and attack their target. */
    private fun updateCombat() {
        val iterator = targets.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, target) = iterator.next()
            val mannequin = instance.server.getEntity(uuid) as? Mannequin
            if (mannequin == null || target.isDead || target.world != mannequin.world) {
                cooldowns.remove(uuid)
                iterator.remove()
                continue
            }
            val distanceSquared = mannequin.location.distanceSquared(target.location)
            if (distanceSquared > GIVE_UP_RANGE_SQUARED) {
                cooldowns.remove(uuid)
                iterator.remove()
                continue
            }
            if (distanceSquared <= ATTACK_RANGE_SQUARED) {
                mannequin.velocity = mannequin.velocity.setX(0.0).setZ(0.0)
                val cooldown = (cooldowns[uuid] ?: 0) - COMBAT_INTERVAL_TICKS.toInt()
                if (cooldown <= 0) {
                    dealDamage(mannequin, target)
                    cooldowns[uuid] = ATTACK_COOLDOWN_TICKS
                } else {
                    cooldowns[uuid] = cooldown
                }
                continue
            }
            val step =
                target
                    .location
                    .toVector()
                    .subtract(mannequin.location.toVector())
                    .setY(0)
            if (step.lengthSquared() == 0.0) continue
            step.normalize().multiply(CHASE_SPEED)
            mannequin.velocity = step.apply { y = mannequin.velocity.y }
        }
    }
}
