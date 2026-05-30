package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling global attribute multipliers for all monsters on Hard difficulty. */
internal object MonsterMechanic : MechanicInterface, MonsterInterface {
    private val monsterAttributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
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

    override val faqTab = FaqTab.ENTITY_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.SPAWNER)
                .setName(Utils.MM.deserialize("<mango>Monster Mechanics</gradient>"))
                .addLoreLines(
                    Utils.MM.deserialize(""),
                    Utils.MM.deserialize(
                        "<yellow>Global Buffs</yellow> <firewatch>></gradient> " +
                            "<white>All monsters on Hard difficulty receive amplified attributes: " +
                            "+30-70% speed/health/damage/follow range, +0-30% size.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Monster ?: return)
        }
    }

    /**
     * Applies global multiplier attributes to a monster on Hard difficulty.
     *
     * @param monster The monster to modify.
     */
    private fun modifySpawn(monster: Monster) {
        monsterAttributes.forEach { (attribute, apply) ->
            monster.getAttribute(attribute)?.let { apply(monster, it) }
        }
    }
}
