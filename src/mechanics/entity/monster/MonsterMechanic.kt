package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent

/** Represents a mechanic handling global attribute multipliers for all monsters on Hard difficulty. */
internal object MonsterMechanic : MonsterInterface {
    override val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.MAX_HEALTH to { monster, attr ->
                attr.baseValue *= (15..20).random() / 10.0
                monster.health = attr.value
            },
            Attribute.ATTACK_DAMAGE to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.FOLLOW_RANGE to { _, attr -> attr.baseValue *= (13..17).random() / 10.0 },
            Attribute.SCALE to { _, attr -> attr.baseValue *= (10..13).random() / 10.0 },
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Monster ?: return)
        }
    }
}
