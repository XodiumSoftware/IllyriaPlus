package org.xodium.illyriacore.enums

import org.bukkit.Material
import org.xodium.illyriacore.projectiles.AirProjectile
import org.xodium.illyriacore.projectiles.EarthProjectile
import org.xodium.illyriacore.projectiles.FireProjectile
import org.xodium.illyriacore.projectiles.ProjectileInterface
import org.xodium.illyriacore.projectiles.WaterProjectile

/** Default kill thresholds to reach levels 2-5, indexed by `currentLevel - 1`. */
private val DEFAULT_KILL_THRESHOLDS = intArrayOf(5, 15, 40, 100)

/** Represents the different types of bending available. */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
    val cooldownTicks: Long,
    val projectile: ProjectileInterface,
    val killThresholds: IntArray = DEFAULT_KILL_THRESHOLDS,
) {
    AIR("white", Material.WIND_CHARGE, 20L, AirProjectile),
    EARTH("brown", Material.RESIN_CLUMP, 60L, EarthProjectile),
    FIRE("red", Material.FIRE_CHARGE, 20L, FireProjectile),
    WATER("blue", Material.HEART_OF_THE_SEA, 30L, WaterProjectile),
}
