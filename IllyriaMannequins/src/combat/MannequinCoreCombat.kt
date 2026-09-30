package org.xodium.illyriamannequins.combat

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriamannequins.CombatMode
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.combatMode
import org.xodium.illyriamannequins.MannequinPDC.owner
import org.xodium.illyriamannequins.Utils.stepToward
import java.util.UUID

/** Makes mannequins defend against nearby monsters and retaliate when attacked. */
@Suppress("UnstableApiUsage")
internal object MannequinCoreCombat : Listener {
    /** The squared distance within which a mannequin performs a melee attack. */
    private const val ATTACK_RANGE_SQUARED = 4.0

    /** The squared distance beyond which a mannequin gives up chasing its attacker. */
    private const val GIVE_UP_RANGE_SQUARED = 900.0

    /** The movement speed in blocks per tick for retaliating mannequins (player sprinting speed). */
    private const val CHASE_SPEED = 0.2806

    /** The interval in ticks at which mannequin combat updates. */
    private const val COMBAT_INTERVAL_TICKS = 2L

    /** The interval in ticks at which mannequins scan for nearby threats. */
    private const val SCAN_INTERVAL_TICKS = 20L

    /** The radius in blocks within which mannequins detect nearby monsters. */
    private const val SCAN_RADIUS = 12.0

    /** The interval in ticks between a mannequin's attacks. */
    private const val ATTACK_COOLDOWN_TICKS = 10

    /** The current retaliation target for each mannequin, keyed by entity UUID. */
    private val targets = mutableMapOf<UUID, LivingEntity>()

    /** The ticks since each mannequin's last attack, keyed by entity UUID. */
    private val cooldowns = mutableMapOf<UUID, Int>()

    /** The mannequins currently in a defensive state, by entity UUID. */
    private val defending = mutableSetOf<UUID>()

    /**
     * Checks if a mannequin is currently engaged in combat.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if the mannequin is retaliating against an attacker.
     */
    fun isEngaged(mannequin: Mannequin): Boolean = mannequin.uniqueId in targets

    /**
     * Checks if a mannequin is currently in a defensive state.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if the mannequin is defending against a nearby monster.
     */
    fun isDefensive(mannequin: Mannequin): Boolean = mannequin.uniqueId in defending

    /**
     * Checks if a mannequin is holding a shield in its offhand.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if a shield is held.
     */
    fun hasShield(mannequin: Mannequin): Boolean = mannequin.equipment.itemInOffHand.type == Material.SHIELD

    /** Registers the combat task and event listeners. */
    fun register() {
        instance.server.pluginManager.registerEvents(this, instance)
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinCoreCombat::updateCombat,
            COMBAT_INTERVAL_TICKS,
            COMBAT_INTERVAL_TICKS,
        )
        instance.server.scheduler.runTaskTimer(
            instance,
            MannequinCoreCombat::scanForThreats,
            SCAN_INTERVAL_TICKS,
            SCAN_INTERVAL_TICKS,
        )
    }

    @EventHandler
    fun on(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? LivingEntity ?: return
        val victim = event.entity as? LivingEntity ?: return
        if (victim is Mannequin) {
            if (damager.uniqueId == victim.owner) return
            if (victim.combatMode == CombatMode.FLEEING) return
            if ((isEngaged(victim) || isDefensive(victim)) && hasShield(victim)) {
                event.isCancelled = true
                damageShield(victim)
            } else {
                damageArmor(victim)
            }
            defending.remove(victim.uniqueId)
            engage(victim, damager)
            return
        }
        if (damager is Player) {
            engageOwnerMannequins(damager, victim)
        }
        if (victim is Player) {
            defendOwner(victim, damager)
        }
    }

    /** Puts passive mannequins near monsters into combat or a defensive state, and clears it when safe. */
    private fun scanForThreats() {
        instance.server.worlds.forEach { world ->
            world
                .entities
                .filterIsInstance<Mannequin>()
                .filterNot { isEngaged(it) }
                .forEach { mannequin ->
                    if (!mannequin.isValid) {
                        defending.remove(mannequin.uniqueId)
                        return@forEach
                    }
                    when (mannequin.combatMode) {
                        CombatMode.AGGRESSIVE -> engageNearestMonster(mannequin)
                        CombatMode.DEFENSIVE -> updateDefensiveState(mannequin)
                        CombatMode.FLEEING -> defending.remove(mannequin.uniqueId)
                    }
                }
        }
    }

    /**
     * Engages the nearest monster in scan range, if any.
     *
     * @param mannequin The mannequin to engage for.
     */
    private fun engageNearestMonster(mannequin: Mannequin) {
        nearestMonster(mannequin)?.let { engage(mannequin, it) }
    }

    /**
     * Puts a mannequin into a defensive stance while a monster is nearby.
     *
     * @param mannequin The mannequin to update.
     */
    private fun updateDefensiveState(mannequin: Mannequin) {
        if (nearestMonster(mannequin) != null) {
            defending.add(mannequin.uniqueId)
            mannequin.velocity = mannequin.velocity.setX(0.0).setZ(0.0)
            if (hasShield(mannequin)) {
                mannequin.startUsingItem(EquipmentSlot.OFF_HAND)
            }
        } else {
            disengage(mannequin)
        }
    }

    /**
     * Takes a mannequin out of the defensive state and lowers its shield.
     *
     * @param mannequin The mannequin to disengage from defense.
     */
    private fun disengage(mannequin: Mannequin) {
        if (defending.remove(mannequin.uniqueId) && mannequin.isHandRaised) {
            mannequin.clearActiveItem()
        }
    }

    /**
     * Finds the nearest monster within scan range of a mannequin.
     *
     * @param mannequin The mannequin to scan around.
     * @return The nearest [Monster], or `null`.
     */
    private fun nearestMonster(mannequin: Mannequin): Monster? =
        mannequin
            .getNearbyEntities(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)
            .filterIsInstance<Monster>()
            .filterNot { it.isDead || it.isInvisible }
            .minByOrNull { it.location.distanceSquared(mannequin.location) }

    /**
     * Blocks a hit with a mannequin's shield, chipping its durability.
     *
     * @param mannequin The mannequin whose shield to damage.
     */
    private fun damageShield(mannequin: Mannequin) {
        mannequin.world.playSound(mannequin.location, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f)
        mannequin.damageItemStack(EquipmentSlot.OFF_HAND, 1)
    }

    /**
     * Chips the durability of all armor worn by a mannequin.
     *
     * @param mannequin The mannequin whose armor to damage.
     */
    private fun damageArmor(mannequin: Mannequin) {
        EquipmentSlot
            .entries
            .filter { it.isArmor }
            .filterNot { mannequin.equipment.getItem(it).isEmpty }
            .forEach { mannequin.damageItemStack(it, 1) }
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
            .filterNot { it.combatMode == CombatMode.FLEEING }
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
            .filterNot { it.combatMode == CombatMode.FLEEING }
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
                if (mannequin != null && mannequin.isHandRaised) {
                    mannequin.clearActiveItem()
                }
                continue
            }
            val distanceSquared = mannequin.location.distanceSquared(target.location)
            if (distanceSquared > GIVE_UP_RANGE_SQUARED) {
                cooldowns.remove(uuid)
                iterator.remove()
                if (mannequin.isHandRaised) {
                    mannequin.clearActiveItem()
                }
                continue
            }
            if (hasShield(mannequin)) {
                mannequin.startUsingItem(EquipmentSlot.OFF_HAND)
            }
            val attackRange =
                if (MannequinSpearCombat.hasSpear(mannequin)) {
                    MannequinSpearCombat.ATTACK_RANGE_SQUARED
                } else {
                    ATTACK_RANGE_SQUARED
                }
            if (distanceSquared <= attackRange) {
                mannequin.velocity = mannequin.velocity.setX(0.0).setZ(0.0)
                val cooldown = (cooldowns[uuid] ?: 0) - COMBAT_INTERVAL_TICKS.toInt()
                if (cooldown <= 0) {
                    if (MannequinSpearCombat.hasSpear(mannequin)) {
                        MannequinSpearCombat.lunge(mannequin, target)
                        cooldowns[uuid] = MannequinSpearCombat.cooldownTicks()
                    } else {
                        MannequinSwordCombat.dealDamage(mannequin, target)
                        cooldowns[uuid] = ATTACK_COOLDOWN_TICKS
                    }
                } else {
                    cooldowns[uuid] = cooldown
                }
                continue
            }
            val step = target.location.stepToward(mannequin.location, CHASE_SPEED) ?: continue
            mannequin.velocity = step.apply { y = mannequin.velocity.y }
        }
    }
}
