package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Skeleton
import org.bukkit.entity.SkeletonHorse
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling skeleton behavior and spawns within the system. */
internal object SkeletonMechanic : MechanicInterface {
    private const val SKELETON_HORSE_CHANCE: Int = 5

    private val skeletonHorseAttributes: Map<Attribute, (SkeletonHorse, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.MOVEMENT_SPEED to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
            Attribute.JUMP_STRENGTH to { _, attr -> attr.baseValue *= (10..13).random() / 10.0 },
            Attribute.ARMOR to { _, attr -> attr.baseValue = (1..4).random().toDouble() },
            Attribute.SAFE_FALL_DISTANCE to { _, attr -> attr.baseValue *= (11..14).random() / 10.0 },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.SKELETON_SKULL)
                .setName(MM.deserialize("<mango>Skeleton Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Undead Cavalry</yellow> <firewatch>></gradient> " +
                            "<white>Skeletons have a $SKELETON_HORSE_CHANCE% chance to spawn riding skeleton horses.</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.ENTITY

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) = modifySpawn(event)

    /**
     * Modifies a skeleton's attributes and enables skeleton horse mounts on Hard difficulty.
     *
     * @param event The CreatureSpawnEvent triggered when an entity spawns.
     */
    private fun modifySpawn(event: CreatureSpawnEvent) {
        val skeleton = event.entity as? Skeleton ?: return

        if (event.entity.world.difficulty != Difficulty.HARD) return

        skeleton.spawnWithSkeletonHorse(SKELETON_HORSE_CHANCE)
    }

    /**
     * Spawns a skeleton horse mount for the skeleton with a configurable chance.
     *
     * @param chance The percentage chance for the skeleton to spawn riding a skeleton horse.
     */
    private fun Skeleton.spawnWithSkeletonHorse(chance: Int) {
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
