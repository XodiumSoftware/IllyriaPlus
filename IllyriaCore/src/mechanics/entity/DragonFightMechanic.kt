package org.xodium.illyriacore.mechanics.entity

import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.EnderDragon
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.xodium.illyriacore.Utils.Schedule.schedule
import org.xodium.illyriacore.mechanics.MechanicInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a mechanic handling the remastered Ender Dragon fight within the system.
 *
 * Features:
 * - Enhanced dragon with increased health (9999 HP default)
 * - End Crystal respawn tracking and mechanics
 */
internal object DragonFightMechanic : MechanicInterface {
    private const val DRAGON_HEALTH = 9999.0
    private const val CRYSTAL_RESPAWN_DELAY_TICKS = 200L

    private val crystalMarkers = ConcurrentHashMap<UUID, Location>()

    @EventHandler
    fun on(event: EntitySpawnEvent) {
        when (val entity = event.entity) {
            is EnderDragon -> initializeDragon(entity)
            is EnderCrystal -> trackCrystal(entity)
        }
    }

    @EventHandler
    fun on(event: EntityDeathEvent) {
        when (val entity = event.entity) {
            is EnderDragon -> handleDragonDeath(entity)
            is EnderCrystal -> handleCrystalDestruction(entity)
        }
    }

    /**
     * Initializes a remastered Ender Dragon with enhanced stats.
     *
     * @param dragon The dragon entity to initialize.
     */
    private fun initializeDragon(dragon: EnderDragon) {
        dragon.getAttribute(Attribute.MAX_HEALTH)?.baseValue = DRAGON_HEALTH
        dragon.health = DRAGON_HEALTH
        crystalMarkers.clear()
    }

    /**
     * Tracks End Crystal locations for respawn mechanics.
     *
     * @param crystal The crystal to track.
     */
    private fun trackCrystal(crystal: EnderCrystal) {
        crystalMarkers[crystal.uniqueId] = crystal.location
    }

    /**
     * Handles dragon death and cleanup.
     *
     * @param dragon The deceased dragon.
     */
    private fun handleDragonDeath(dragon: EnderDragon) {
        val world = dragon.world
        crystalMarkers.keys.removeIf { uuid ->
            world.getEntity(uuid)?.isDead != false
        }
    }

    /**
     * Handles crystal destruction and schedules respawn.
     *
     * @param crystal The destroyed crystal.
     */
    private fun handleCrystalDestruction(crystal: EnderCrystal) {
        val location = crystalMarkers.remove(crystal.uniqueId) ?: crystal.location

        schedule(delay = CRYSTAL_RESPAWN_DELAY_TICKS) {
            val world = crystal.world
            if (world.entities.none { it is EnderCrystal && !it.isDead }) {
                world.spawn(location, EnderCrystal::class.java) { newCrystal ->
                    newCrystal.isShowingBottom = crystal.isShowingBottom
                }
            }
        }
    }
}
