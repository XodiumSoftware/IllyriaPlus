package org.xodium.illyriaplus.mechanics.world

import org.bukkit.GameRules
import org.bukkit.block.data.type.Snow
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFormEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.math.floor

/** Represents a mechanic handling snow within the system. */
internal object SnowMechanic : MechanicInterface {
    private const val NOISE_SCALE = 0.005
    private const val DETAIL_SCALE = 0.02
    private const val DETAIL_AMPLITUDE = 0.35
    private const val BASE_SEED_MIX = 0x5f3f_a2b7_8c1d_4e2aL
    private const val DETAIL_SEED_MIX = 0x7d7b_e4c1_6f82_03a5L

    @EventHandler(ignoreCancelled = true)
    fun on(event: BlockFormEvent) {
        val snow = event.newState.blockData as? Snow ?: return
        val physicalMax = snow.maximumLayers
        val ruleMax = event.block.world.getGameRuleValue(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT)
        val cap = ruleMax.coerceAtMost(physicalMax)

        if (cap <= 0) {
            event.isCancelled = true
            return
        }

        val block = event.block
        val worldSeed = block.world.seed
        val target = snowTarget(block.x, block.z, cap, worldSeed)
        val current = snow.layers

        val next =
            when {
                current < target -> (current + 1).coerceAtMost(cap)
                current > target -> (current - 1).coerceAtLeast(0)
                else -> current
            }

        if (next <= 0 && current <= 0) {
            event.isCancelled = true
            return
        }

        snow.layers = next.coerceIn(0, cap)
        event.newState.blockData = snow
    }

    /**
     * Returns a target snow-layer count for the given block column.
     *
     * The value is deterministic for a given [worldSeed] and coordinate pair, but
     * varies smoothly across the landscape to create broad drifts.
     *
     * @param x The block column x coordinate.
     * @param z The block column z coordinate.
     * @param cap The maximum layer count allowed by physics and game rules.
     * @param worldSeed The seed of the world, used to make drift patterns reproducible.
     * @return The target snow layer count in the range [0, cap].
     */
    private fun snowTarget(
        x: Int,
        z: Int,
        cap: Int,
        worldSeed: Long,
    ): Int {
        val nx = x.toDouble()
        val nz = z.toDouble()

        val base = Utils.Math.noise2D(nx * NOISE_SCALE, nz * NOISE_SCALE, worldSeed + BASE_SEED_MIX)
        val detail = Utils.Math.noise2D(nx * DETAIL_SCALE, nz * DETAIL_SCALE, worldSeed + DETAIL_SEED_MIX)

        val combined = base + detail * DETAIL_AMPLITUDE
        val normalized = (combined + 1.0) / (2.0 + DETAIL_AMPLITUDE)

        return floor(normalized * cap).toInt().coerceIn(0, cap)
    }
}
