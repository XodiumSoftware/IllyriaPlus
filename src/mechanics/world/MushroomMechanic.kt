package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockCanBuildEvent
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling mushroom placement on logs within the system. */
internal object MushroomMechanic : MechanicInterface {
    private val MATERIALS: Set<Material> = setOf(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM)

    @EventHandler
    fun on(event: BlockCanBuildEvent) {
        if (event.material !in MATERIALS) return
        if (Tag.LOGS.isTagged(event.block.getRelative(0, -1, 0).type)) event.isBuildable = true
    }
}
