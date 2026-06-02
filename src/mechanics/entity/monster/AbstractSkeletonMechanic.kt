package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.AbstractSkeleton
import org.bukkit.entity.Monster
import org.bukkit.entity.SkeletonHorse
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.Monster.trySpawnMount
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling all skeleton variants behavior and spawns within the system. */
internal object AbstractSkeletonMechanic : MonsterInterface {
    private const val SKELETON_HORSE_CHANCE: Int = 5
    private const val SHOULD_BURN_IN_DAY: Boolean = false

    override val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.ARMOR to { _, attr -> attr.baseValue = (1..3).random().toDouble() },
            Attribute.ARMOR_TOUGHNESS to { _, attr -> attr.baseValue = (1..2).random().toDouble() },
        )

    override val horseAttributes: Map<Attribute, (AbstractHorse, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
            Attribute.JUMP_STRENGTH to { _, attr -> attr.baseValue *= (10..13).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (1..4).random().toDouble() },
            Attribute.SAFE_FALL_DISTANCE to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.SKELETON_SKULL)
                .setName(Utils.MM.deserialize("<mango>Skeleton Mechanics</gradient>"))
                .addLoreLines(
                    Utils.MM.deserialize(""),
                    Utils.MM.deserialize(
                        "<yellow>Undead Cavalry</yellow> <firewatch>></gradient> " +
                            "<white>All Skeletons types have a $SKELETON_HORSE_CHANCE% " +
                            "chance to spawn riding skeleton horses.</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Attribute Modifiers</yellow> <firewatch>></gradient> " +
                            "<white>+1-3 armor, +1-2 toughness.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? AbstractSkeleton ?: return)
        }
    }

    override fun modifySpawn(monster: Monster) {
        super.modifySpawn(monster)
        (monster as AbstractSkeleton).apply {
            setShouldBurnInDay(SHOULD_BURN_IN_DAY)
            trySpawnMount<SkeletonHorse>(SKELETON_HORSE_CHANCE, horseAttributes)
        }
    }
}
