package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.AbstractSkeleton
import org.bukkit.entity.SkeletonHorse
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling all skeleton variants behavior and spawns within the system. */
internal object AbstractSkeletonMechanic : MechanicInterface, MonsterInterface {
    private const val SKELETON_HORSE_CHANCE: Int = 5
    private const val SHOULD_BURN_IN_DAY: Boolean = false

    private val skeletonAttributes: Map<Attribute, (AbstractSkeleton, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.ARMOR to { _, attr -> attr.baseValue = (1..3).random().toDouble() },
            Attribute.ARMOR_TOUGHNESS to { _, attr -> attr.baseValue = (1..2).random().toDouble() },
        )
    private val skeletonHorseAttributes: Map<Attribute, (SkeletonHorse, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
            Attribute.JUMP_STRENGTH to { _, attr -> attr.baseValue *= (10..13).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (1..4).random().toDouble() },
            Attribute.SAFE_FALL_DISTANCE to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
        )

    override val faqTab = FaqTab.ENTITY_MECHANIC

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

    /**
     * Modifies a skeleton variant's attributes and enables skeleton horse mounts on Hard difficulty.
     *
     * @param skeleton The skeleton variant to modify.
     */
    private fun modifySpawn(skeleton: AbstractSkeleton) {
        skeleton.setShouldBurnInDay(SHOULD_BURN_IN_DAY)
        skeleton.spawnWithSkeletonHorse(SKELETON_HORSE_CHANCE)
        skeletonAttributes.forEach { (attribute, apply) ->
            skeleton.getAttribute(attribute)?.let { apply(skeleton, it) }
        }
    }

    /**
     * Spawns a skeleton horse mount for the skeleton variant with a configurable chance.
     *
     * @param chance The percentage chance for the skeleton variant to spawn riding a skeleton horse.
     */
    private fun AbstractSkeleton.spawnWithSkeletonHorse(chance: Int) {
        if ((1..100).random() > chance) return

        world
            .spawn(location, SkeletonHorse::class.java) { horse ->
                horse.isTamed = true
                skeletonHorseAttributes.forEach { (attribute, apply) ->
                    horse.getAttribute(attribute)?.let { apply(horse, it) }
                }
            }.addPassenger(this)
    }
}
