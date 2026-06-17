package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockCanBuildEvent
import org.xodium.illyriaplus.data.BuildSetupData
import org.xodium.illyriaplus.data.BuildSetupData.Companion.toMaterialMap
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling custom block placement rules within the system. */
internal object BlockPlacementMechanic : MechanicInterface {
    private val SETUPS: Map<Material, BuildSetupData> =
        listOf(
            BuildSetupData(setOf(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM), Tag.LOGS),
        ).toMaterialMap()

    @EventHandler
    fun on(event: BlockCanBuildEvent) {
        SETUPS[event.material]?.let {
            val (x, y, z) = it.offset
            if (it.target.isTagged(event.block.getRelative(x, y, z).type)) event.isBuildable = true
        }
    }
}
