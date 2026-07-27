package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic that re-flavors zombies as cowardly cave-dwelling goblins. */
internal object GoblinMechanic : MechanicInterface {
    private const val GOBLIN_SPEED_MULTIPLIER: Double = 1.35
    private const val GOBLIN_GEAR_CHANCE: Double = 0.40
    private const val GOBLIN_FLEE_HEALTH_THRESHOLD: Double = 0.30
    private const val GOBLIN_FLEE_DURATION_TICKS: Int = 100
    private const val GOBLIN_MAX_SURFACE_LIGHT: Int = 7

    @EventHandler
    fun on(event: CreatureSpawnEvent) = goblinSpawn(event)

    @EventHandler
    fun on(event: EntityDamageEvent) = goblinFlee(event)

    @EventHandler
    fun on(event: EntityDeathEvent) = goblinDrops(event)

    private fun goblinSpawn(event: CreatureSpawnEvent) {
        // TODO: implement goblin spawn logic
    }

    private fun goblinFlee(event: EntityDamageEvent) {
        // TODO: implement goblin flee logic
    }

    private fun goblinDrops(event: EntityDeathEvent) {
        // TODO: implement goblin drop logic
    }
}
