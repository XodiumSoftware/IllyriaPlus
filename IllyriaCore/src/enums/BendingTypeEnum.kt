package org.xodium.illyriacore.enums

import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.entity.Snowball
import org.bukkit.entity.WindCharge

/** Represents the different types of bending available. */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
    val cooldownTicks: Long,
    private val launchFn: (Player) -> Unit,
) {
    AIR("white", Material.WIND_CHARGE, 20L, { it.launchProjectile(WindCharge::class.java) }),
    EARTH("brown", Material.RESIN_CLUMP, 60L, { launchEarthBoulder(it) }),
    FIRE("red", Material.FIRE_CHARGE, 20L, { it.launchProjectile(SmallFireball::class.java) }),
    WATER("blue", Material.HEART_OF_THE_SEA, 30L, { it.launchProjectile(Snowball::class.java) }),
    ;

    /**
     * Launches the projectile/ability associated with this bending type from [player].
     *
     * @param player The player casting the ability.
     */
    fun launch(player: Player) = launchFn(player)

    private companion object {
        /** Forward speed applied to the earth boulder projectile. */
        private const val EARTH_BOULDER_SPEED = 1.5

        /** Upward bias applied to the earth boulder so it arcs. */
        private const val EARTH_BOULDER_LIFT = 0.3

        /** Damage dealt per fallen block when the boulder lands on an entity. */
        private const val EARTH_BOULDER_DAMAGE_PER_BLOCK = 2.0f

        /** Maximum damage a boulder can deal to a single entity. */
        private const val EARTH_BOULDER_MAX_DAMAGE = 6

        /** The material the earth boulder is made of. */
        private val EARTH_BOULDER_MATERIAL: Material = Material.MUD

        /**
         * Lobs a falling-block boulder made of [EARTH_BOULDER_MATERIAL] from the player's eye,
         * arcing in the direction they're looking. The boulder damages entities it lands on and
         * does not drop an item when it settles.
         *
         * @param player The player launching the boulder.
         */
        private fun launchEarthBoulder(player: Player) {
            val direction = player.location.direction
            player.world.spawn(player.eyeLocation, FallingBlock::class.java) { boulder ->
                boulder.blockData = EARTH_BOULDER_MATERIAL.createBlockData()
                boulder.velocity =
                    direction.multiply(EARTH_BOULDER_SPEED).setY(direction.y + EARTH_BOULDER_LIFT)
                boulder.setHurtEntities(true)
                boulder.damagePerBlock = EARTH_BOULDER_DAMAGE_PER_BLOCK
                boulder.maxDamage = EARTH_BOULDER_MAX_DAMAGE
                boulder.dropItem = false
                boulder.cancelDrop = true
            }
        }
    }
}
