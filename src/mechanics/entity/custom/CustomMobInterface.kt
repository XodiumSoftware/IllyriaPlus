package org.xodium.illyriaplus.mechanics.entity.custom

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Schedule.schedule
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.measureTime

/** Represents a contract for custom mob mechanics within the system. */
internal interface CustomMobInterface<T : LivingEntity> : MechanicInterface {
    /** The tag key used to identify this custom mob type. */
    val tagKey: NamespacedKey

    /** The interval between spawn attempts in ticks. */
    val spawnIntervalTicks: Long

    /** The chance a spawn attempt succeeds. */
    val spawnChance: Double

    /** Minimum horizontal distance from the player to attempt a spawn. */
    val spawnMinRadius: Double

    /** Maximum horizontal distance from the player to attempt a spawn. */
    val spawnMaxRadius: Double

    /** Number of location attempts per spawn check. */
    val spawnAttempts: Int

    /** Maximum number of nearby custom mobs allowed before spawning. */
    val maxNearby: Int

    /** Maximum players checked per spawn tick. */
    val maxPlayersPerTick: Int

    /** Maximum surface light level that permits daytime spawns. */
    val maxSurfaceLight: Int

    /** Biomes where this custom mob may spawn. */
    val biomes: Set<Biome>

    /** Spawns a single custom mob at the given location. */
    fun spawnMob(world: World, location: Location)

    /** Checks whether the given entity is this type of custom mob. */
    fun isMob(entity: T): Boolean =
        entity.persistentDataContainer.has(tagKey, PersistentDataType.BOOLEAN)

    /** Tags an entity as this custom mob type. */
    fun tagMob(entity: T) {
        entity.persistentDataContainer.set(tagKey, PersistentDataType.BOOLEAN, true)
    }

    /**
     * Attempts to spawn custom mob encounters near players in valid biomes.
     *
     * @param groupSize The range of mobs to spawn, or null to skip group spawning.
     */
    fun attemptSpawns(groupSize: IntRange? = null) {
        val players = instance.server.onlinePlayers
        if (players.isEmpty()) return

        players
            .shuffled()
            .take(maxPlayersPerTick)
            .forEach { player ->
                if (!canSpawn(player)) return@forEach

                val spawnLocation = findSpawnLocation(player.location) ?: return@forEach
                spawnEncounter(player.world, spawnLocation, groupSize)
            }
    }

    /**
     * Checks whether this custom mob may attempt to spawn around this player.
     *
     * @param player The player to check around.
     * @return True if a spawn attempt may proceed.
     */
    fun canSpawn(player: org.bukkit.entity.Player): Boolean {
        val world = player.world
        if (world.difficulty == org.bukkit.Difficulty.PEACEFUL) return false
        if (player.location.block.biome !in biomes) return false
        if (Random.nextDouble() >= spawnChance) return false
        if (countNearby(player.location) >= maxNearby) return false
        return true
    }

    /**
     * Counts nearby custom mobs of this type.
     *
     * @param location The location to search around.
     * @return The number of nearby custom mobs.
     */
    @Suppress("UNCHECKED_CAST")
    fun countNearby(location: Location): Int =
        location.world
            .getNearbyEntities(
                location,
                spawnMaxRadius,
                spawnMaxRadius,
                spawnMaxRadius,
            )
            .filterIsInstance<LivingEntity>()
            .filter { isMob(it as T) }
            .count()

    /**
     * Finds a valid spawn location near the given center.
     *
     * @param center The location to spawn around.
     * @return A valid spawn location, or null if none was found.
     */
    fun findSpawnLocation(center: Location): Location? {
        val world = center.world

        repeat(spawnAttempts) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val distance = Random.nextDouble(spawnMinRadius, spawnMaxRadius)
            val x = center.x + distance * cos(angle)
            val z = center.z + distance * sin(angle)
            val y = world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble() + 1.0
            val location = Location(world, x, y, z)

            if (location.block.biome !in biomes) return@repeat
            if (shouldCancelSurfaceSpawn(location)) return@repeat

            val ground = location.clone().subtract(0.0, 1.0, 0.0).block
            if (!ground.isSolid) return@repeat

            return location
        }

        return null
    }

    /**
     * Determines whether a surface spawn should be skipped due to daylight.
     *
     * @param location The spawn location to evaluate.
     * @return True if the spawn should be skipped because it is too bright on the surface.
     */
    fun shouldCancelSurfaceSpawn(location: Location): Boolean {
        if (location.world.isThundering || !location.world.isDayTime) return false
        val blockLight = location.block.lightLevel
        return blockLight > maxSurfaceLight && location.blockY >= location.world.seaLevel
    }

    /**
     * Spawns a single custom mob and an optional group.
     *
     * @param world The world to spawn in.
     * @param location The center location to spawn around.
     * @param groupSize The range of mobs to spawn, or null to skip group spawning.
     */
    fun spawnEncounter(
        world: World,
        location: Location,
        groupSize: IntRange? = null,
    ) {
        spawnMob(world, location)
        groupSize?.let { spawnGroup(world, location, it) }
    }

    /**
     * Spawns a group of additional mobs around the initial spawn point.
     *
     * @param world The world to spawn in.
     * @param location The center location to spawn around.
     * @param groupSize The range of additional mobs to spawn.
     */
    fun spawnGroup(
        world: World,
        location: Location,
        groupSize: IntRange,
        radius: Double = 3.0,
    ) {
        val count = Random.nextInt(groupSize.first, groupSize.last + 1)
        repeat(count - 1) {
            val offsetX = Random.nextDouble(-radius, radius)
            val offsetZ = Random.nextDouble(-radius, radius)
            val spawnLocation = location.clone().add(offsetX, 0.0, offsetZ)
            spawnLocation.y = world.getHighestBlockYAt(spawnLocation).toDouble() + 1.0

            if (spawnLocation.block.biome !in biomes) return@repeat
            if (shouldCancelSurfaceSpawn(spawnLocation)) return@repeat

            spawnMob(world, spawnLocation)
        }
    }

    /**
     * Registers this mechanic and schedules periodic spawn attempts.
     *
     * @param groupSize The range of mobs per encounter, or null for solo spawns.
     * @return Time taken to register in milliseconds.
     */
    fun register(groupSize: IntRange? = null): Long =
        super.register() +
            measureTime {
                schedule(
                    delay = spawnIntervalTicks,
                    period = spawnIntervalTicks,
                ) { attemptSpawns(groupSize) }
            }.inWholeMilliseconds
}
