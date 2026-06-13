package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Difficulty
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.Monster
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a contract for a monster-specific mechanic within the system. */
internal interface MonsterInterface : MechanicInterface {
    /** The difficulty on which this mechanic's spawn modifications apply. */
    val difficulty: Difficulty get() = Difficulty.HARD

    /** A map of attributes to apply to a [Monster] when it spawns. */
    val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> get() = mapOf()

    /** A map of attributes to apply to an [AbstractHorse] mount when it spawns. */
    val horseAttributes: Map<Attribute, (AbstractHorse, AttributeInstance) -> Unit> get() = mapOf()

    /**
     * Applies [attributes] to the given [monster] upon spawn.
     */
    fun modifySpawn(monster: Monster) {
        attributes.forEach { (attribute, apply) ->
            monster.getAttribute(attribute)?.let { apply(monster, it) }
        }
    }
}
