package org.xodium.illyriaplus.mechanics.world

import org.bukkit.block.data.type.Snow
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.util.noise.PerlinNoiseGenerator
import org.xodium.illyriaplus.mechanics.MechanicInterface
import java.util.concurrent.ConcurrentHashMap

/** Represents a mechanic handling snow within the system. */
internal object SnowMechanic : MechanicInterface {
    private val generators = ConcurrentHashMap<String, PerlinNoiseGenerator>()

    @EventHandler
    fun on(event: BlockFormEvent) {
        val snow = event.newState.blockData as? Snow ?: return
        val world = event.block.world
        val generator = generators.computeIfAbsent(world.name) { PerlinNoiseGenerator(world) }
        val noise = generator.noise(event.block.x * 0.02, event.block.z * 0.02)
        val layers = (((noise + 1.0) * 0.5) * 7.0).toInt().coerceIn(1, 8)

        snow.layers = layers
        event.newState.blockData = snow
    }
}
