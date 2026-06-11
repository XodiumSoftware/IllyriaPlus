package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Creeper
import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent

/** Represents a mechanic handling creeper behavior and spawns within the system. */
internal object CreeperMechanic : MonsterInterface {
    private const val IS_POWERED: Boolean = true

    private val explosionRadiusRange: IntRange = 4..7

    override val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.KNOCKBACK_RESISTANCE to { _, attr -> attr.baseValue = (2..5).random() / 10.0 },
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Creeper ?: return)
        }
    }

    override fun modifySpawn(monster: Monster) {
        super.modifySpawn(monster)
        (monster as Creeper).apply {
            isPowered = IS_POWERED
            explosionRadius = explosionRadiusRange.random()
        }
    }
}
