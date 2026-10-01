@file:Suppress("Unused")

package org.xodium.illyriacore

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.EntityType
import org.bukkit.entity.Tameable
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/** General utilities. */
internal object Utils {
    /** Item-related utilities. */
    object Item {
        /**
         * Creates a spawn egg [ItemStack] for this [EntityType] using the typed item registry.
         *
         * @return A spawn egg item stack, or `null` if this entity type has no spawn egg.
         */
        fun EntityType.spawnEgg(): ItemStack? =
            Registry.ITEM.get(NamespacedKey.minecraft("${key.key}_spawn_egg"))?.createItemStack()
    }

    /** Schedule-related utilities. */
    object Schedule {
        /**
         * Schedules a repeating task.
         *
         * @param delay Initial delay in ticks.
         * @param period Interval between executions.
         * @param duration Optional total runtime.
         * @param content Task logic.
         * @return The scheduled BukkitTask.
         */
        fun schedule(
            delay: Long = 0L,
            period: Long = 2L,
            duration: Long? = null,
            content: () -> Unit,
        ): BukkitTask =
            instance
                .server
                .scheduler
                .runTaskTimer(instance, content, delay, period)
                .also { task ->
                    duration?.let {
                        instance.server.scheduler.runTaskLater(
                            instance,
                            task::cancel,
                            it,
                        )
                    }
                }
    }

    /** World-related utilities. */
    object World {
        /**
         * Returns a copy of this location with the Y adjusted to the highest solid block's Y + 1 at the current X/Z.
         * If no solid block exists (e.g. flat world), falls back to the original Y.
         *
         * @return A new [Location] at surface level.
         */
        fun Location.toSurface(): Location =
            clone().apply {
                val world = world ?: return@apply
                val surfaceY = world.getHighestBlockYAt(blockX, blockZ) + 1
                y = maxOf(surfaceY, blockY).toDouble()
            }
    }

    /** Block-related utilities. */
    object Block {
        /**
         * Gets the center location of a block, handling double chests.
         *
         * @return The center Location.
         */
        fun org.bukkit.block.Block.center(): Location {
            val baseAddition =
                Location(location.world, location.x + 0.5, location.y + 0.5, location.z + 0.5)
            val chestState = state as? Chest ?: return baseAddition
            val holder = chestState.inventory.holder as? DoubleChest ?: return baseAddition
            val leftBlock = (holder.leftSide as? Chest)?.block
            val rightBlock = (holder.rightSide as? Chest)?.block

            if (leftBlock == null || rightBlock == null || leftBlock.world !== rightBlock.world) {
                return baseAddition
            }

            val world = leftBlock.world
            val cx = (leftBlock.x + rightBlock.x) / 2.0 + 0.5
            val cy = (leftBlock.y + rightBlock.y) / 2.0 + 0.5
            val cz = (leftBlock.z + rightBlock.z) / 2.0 + 0.5

            return Location(world, cx, cy, cz)
        }
    }

    /** Player-related utilities. */
    object Player {
        /**
         * Gets nearby containers in a chunk radius.
         *
         * @return Set of containers.
         */
        fun org.bukkit.entity.Player.getContainersAround(): Set<Container> =
            buildSet {
                for (chunk in getChunksAround()) {
                    for (state in chunk.tileEntities) {
                        if (state is Container) add(state)
                    }
                }
            }

        /**
         * Gets surrounding chunks.
         *
         * @param range Radius in chunks.
         * @return Set of chunks.
         */
        fun org.bukkit.entity.Player.getChunksAround(range: Int = 1): Set<Chunk> {
            val (baseX, baseZ) = location.chunk.run { x to z }

            return buildSet {
                for (x in -range..range) {
                    for (z in -range..range) {
                        add(world.getChunkAt(baseX + x, baseZ + z))
                    }
                }
            }
        }

        /**
         * Gets the first leashed tameable entity owned by the player.
         *
         * @param radius Search radius.
         * @return The entity or null.
         */
        fun org.bukkit.entity.Player.getLeashedEntity(radius: Double = 10.0): Tameable? =
            getNearbyEntities(radius, radius, radius)
                .filterIsInstance<Tameable>()
                .firstOrNull { it.isLeashed && it.leashHolder == this }
    }

    /** Monster-related utilities. */
    object Monster {
        /**
         * Spawns an [AbstractHorse] mount for this [Monster] with a configurable chance,
         * then applies [attributes] to the horse and makes this monster its passenger.
         *
         * @param H The concrete horse type to spawn.
         * @param chance The percentage chance for the mount to spawn.
         * @param attributes A map of attribute mutations to apply to the spawned horse.
         */
        inline fun <reified H : AbstractHorse> org.bukkit.entity.Monster.trySpawnMount(
            chance: Int,
            attributes: Map<Attribute, (AbstractHorse, AttributeInstance) -> Unit>,
        ) {
            if ((1..100).random() > chance) return
            world
                .spawn(location, H::class.java) { horse ->
                    horse.isTamed = true
                    attributes.forEach { (attribute, apply) ->
                        horse.getAttribute(attribute)?.let { apply(horse, it) }
                    }
                }.addPassenger(this)
        }
    }
}
