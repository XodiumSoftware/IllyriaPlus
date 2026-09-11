package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling mob griefing prevention within the system. */
internal object GriefingMechanic : MechanicInterface {
    private val GRIEF_CANCELLED_ENTITIES =
        setOf(
            EntityType.BLAZE,
            EntityType.CREEPER,
            EntityType.ENDER_DRAGON,
            EntityType.ENDERMAN,
            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.WITHER,
        )

    @EventHandler
    fun on(event: EntityChangeBlockEvent) = preventEntityChangeBlock(event)

    @EventHandler
    fun on(event: EntityExplodeEvent) = preventEntityExplode(event)

    /**
     * Prevents entity block changes for griefing entities.
     *
     * @param event The EntityChangeBlockEvent to handle.
     */
    private fun preventEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.entityType in GRIEF_CANCELLED_ENTITIES) event.isCancelled = true
    }

    /**
     * Clears the block list for explosions caused by griefing entities.
     *
     * @param event The EntityExplodeEvent to handle.
     */
    private fun preventEntityExplode(event: EntityExplodeEvent) {
        if (event.entityType in GRIEF_CANCELLED_ENTITIES) event.blockList().clear()
    }
}
