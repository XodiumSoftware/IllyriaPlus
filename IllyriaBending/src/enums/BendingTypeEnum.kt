package org.xodium.illyriabending.enums

import org.bukkit.Material
import org.xodium.illyriabending.projectiles.AirProjectile
import org.xodium.illyriabending.projectiles.EarthProjectile
import org.xodium.illyriabending.projectiles.FireProjectile
import org.xodium.illyriabending.projectiles.ProjectileInterface
import org.xodium.illyriabending.projectiles.WaterProjectile

/** Default kill thresholds to reach levels 2-5, indexed by `currentLevel - 1`. */
private val DEFAULT_KILL_THRESHOLDS = intArrayOf(5, 15, 40, 100)

/**
 * The available bending types. Each entry carries everything needed to render, gate, and
 * launch its bending ability.
 *
 * @property color MiniMessage color name used in advancement/dialog text for this element.
 * @property material The offhand focus [Material] required to cast this element.
 * @property cooldownTicks Cooldown in Minecraft ticks (20 = 1 second) between successful casts.
 * @property projectile The [ProjectileInterface] launched when the player casts this element.
 * @property subSkill The themed [SubSkill] unlocked at level 3.
 * @property killThresholds Kill counts required to advance from level N to N+1, indexed by
 * `currentLevel - 1`. Defaults to [DEFAULT_KILL_THRESHOLDS].
 */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
    val cooldownTicks: Long,
    val projectile: ProjectileInterface,
    val subSkill: SubSkill,
    val killThresholds: IntArray = DEFAULT_KILL_THRESHOLDS,
) {
    AIR("white", Material.WIND_CHARGE, 20L, AirProjectile, SubSkill.VACUUM),
    EARTH("brown", Material.RESIN_CLUMP, 60L, EarthProjectile, SubSkill.METAL),
    FIRE("red", Material.FIRE_CHARGE, 20L, FireProjectile, SubSkill.LIGHTNING),
    WATER("blue", Material.HEART_OF_THE_SEA, 30L, WaterProjectile, SubSkill.BLOOD),
    ;

    /**
     * The themed sub-skill an element unlocks at level 3. Carries the metadata needed to render
     * and address the sub-skill's advancement chain.
     *
     * @property keyPrefix The registry key prefix for the advancement chain, e.g. `vacuum_1`.
     * @property label The display label used in advancement titles, e.g. `Vacuum Bender III`.
     * @property icon The advancement-chain icon material.
     */
    internal enum class SubSkill(
        val keyPrefix: String,
        val label: String,
        val icon: Material,
    ) {
        VACUUM("vacuum", "Vacuum", Material.PHANTOM_MEMBRANE),
        METAL("metal", "Metal", Material.IRON_NUGGET),
        LIGHTNING("lightning", "Lightning", Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE),
        BLOOD("blood", "Blood", Material.SPIDER_EYE),
    }
}
