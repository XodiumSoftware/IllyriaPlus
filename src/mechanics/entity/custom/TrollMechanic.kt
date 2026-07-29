package org.xodium.illyriaplus.mechanics.entity.custom

import org.bukkit.Difficulty
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.block.Biome
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Schedule.schedule
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.measureTime

/** Represents a mechanic that spawns slow, tanky cave trolls in mountain and spruce forest biomes. */
internal object TrollMechanic : MechanicInterface {
    private const val TROLL_HEALTH_MULTIPLIER: Double = 3.50
    private const val TROLL_SCALE_MULTIPLIER: Double = 1.35
    private const val TROLL_DAMAGE_MULTIPLIER: Double = 1.40
    private const val TROLL_SPEED_MULTIPLIER: Double = 0.60
    private const val TROLL_KNOCKBACK_RESISTANCE: Double = 0.85
    private const val TROLL_GROUP_MIN_SIZE: Int = 1
    private const val TROLL_GROUP_MAX_SIZE: Int = 3
    private const val TROLL_GROUP_RADIUS: Double = 6.0
    private const val TROLL_REGEN_THRESHOLD: Double = 0.50
    private const val TROLL_REGEN_INTERVAL_TICKS: Long = 60L
    private const val TROLL_REGEN_AMOUNT: Double = 2.0
    private const val TROLL_REGEN_DURATION_TICKS: Int = 100
    private const val TROLL_DROP_BASE_MIN: Int = 1
    private const val TROLL_DROP_BASE_MAX: Int = 4
    private const val TROLL_MAX_SURFACE_LIGHT: Int = 10

    private const val TROLL_SPAWN_INTERVAL_TICKS: Long = 600L
    private const val TROLL_SPAWN_CHANCE: Double = 0.15
    private const val TROLL_SPAWN_MIN_RADIUS: Double = 24.0
    private const val TROLL_SPAWN_MAX_RADIUS: Double = 64.0
    private const val TROLL_SPAWN_ATTEMPTS: Int = 6
    private const val TROLL_MAX_NEARBY: Int = 3
    private const val TROLL_MAX_PLAYERS_PER_TICK: Int = 2

    private val TROLL_BIOMES =
        setOf(
            Biome.JAGGED_PEAKS,
            Biome.FROZEN_PEAKS,
            Biome.STONY_PEAKS,
            Biome.SNOWY_SLOPES,
            Biome.GROVE,
            Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.OLD_GROWTH_SPRUCE_TAIGA,
            Biome.TAIGA,
            Biome.SNOWY_TAIGA,
        )

    private val TROLL_DROP_MATERIALS =
        mapOf(
            Material.COBBLESTONE to 0.50,
            Material.RAW_IRON to 0.30,
            Material.BONE to 0.40,
            Material.SPRUCE_LOG to 0.20,
            Material.MOSSY_COBBLESTONE to 0.15,
        )

    private val TROLL_TAG_KEY = NamespacedKey(instance, "is_troll")
    private val REGENERATING_TROLLS = mutableSetOf<Int>()

    override fun register(): Long =
        super.register() +
            measureTime {
                schedule(
                    delay = TROLL_SPAWN_INTERVAL_TICKS,
                    period = TROLL_SPAWN_INTERVAL_TICKS,
                ) { attemptTrollSpawns() }
                schedule(
                    delay = TROLL_REGEN_INTERVAL_TICKS,
                    period = TROLL_REGEN_INTERVAL_TICKS,
                ) { attemptTrollRegen() }
            }.inWholeMilliseconds

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) = trollRegenTrigger(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDeathEvent) = trollDrops(event)

    /**
     * Attempts to spawn troll encounters near players in troll biomes.
     */
    private fun attemptTrollSpawns() {
        val players = instance.server.onlinePlayers
        if (players.isEmpty()) return

        players
            .shuffled()
            .take(TROLL_MAX_PLAYERS_PER_TICK)
            .forEach { player ->
                if (!canSpawnTrolls(player)) return@forEach

                val spawnLocation = findSpawnLocation(player.location) ?: return@forEach
                spawnTrollEncounter(player.world, spawnLocation)
            }
    }

    /**
     * Attempts to regenerate trolls that are below the health threshold.
     */
    private fun attemptTrollRegen() {
        val iterator = REGENERATING_TROLLS.iterator()
        while (iterator.hasNext()) {
            val entityId = iterator.next()
            val troll =
                instance.server.worlds
                    .asSequence()
                    .flatMap { it.entities.asSequence() }
                    .filterIsInstance<IronGolem>()
                    .firstOrNull { it.entityId == entityId && isTroll(it) }
                    ?: run {
                        iterator.remove()
                        continue
                    }

            if (troll.isDead) {
                iterator.remove()
                continue
            }

            val maxHealth = troll.getAttribute(Attribute.MAX_HEALTH)?.value ?: continue
            if (troll.health >= maxHealth * TROLL_REGEN_THRESHOLD) {
                iterator.remove()
                troll.world.spawnParticle(Particle.HAPPY_VILLAGER, troll.location, 6, 0.5, 0.5, 0.5)
                continue
            }

            troll.health = (troll.health + TROLL_REGEN_AMOUNT).coerceAtMost(maxHealth)
            troll.world.spawnParticle(Particle.WAX_ON, troll.location, 4, 0.4, 0.6, 0.4)
        }
    }

    /**
     * Checks whether trolls may attempt to spawn around this player.
     */
    private fun canSpawnTrolls(player: Player): Boolean {
        val world = player.world
        if (world.difficulty == Difficulty.PEACEFUL) return false
        if (player.location.block.biome !in TROLL_BIOMES) return false
        if (Random.nextDouble() >= TROLL_SPAWN_CHANCE) return false
        if (countNearbyTrolls(player.location) >= TROLL_MAX_NEARBY) return false
        return true
    }

    /**
     * Counts trolls within the spawn radius of a location.
     */
    private fun countNearbyTrolls(location: Location): Int =
        location.world
            .getNearbyEntities(
                location,
                TROLL_SPAWN_MAX_RADIUS,
                TROLL_SPAWN_MAX_RADIUS,
                TROLL_SPAWN_MAX_RADIUS,
            )
            .filterIsInstance<IronGolem>()
            .count { isTroll(it) }

    /**
     * Finds a valid troll spawn location near the given center.
     *
     * @param center The location to spawn around.
     * @return A valid spawn location, or null if none was found.
     */
    private fun findSpawnLocation(center: Location): Location? {
        val world = center.world

        repeat(TROLL_SPAWN_ATTEMPTS) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val distance = Random.nextDouble(TROLL_SPAWN_MIN_RADIUS, TROLL_SPAWN_MAX_RADIUS)
            val x = center.x + distance * cos(angle)
            val z = center.z + distance * sin(angle)
            val y = world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble() + 1.0
            val location = Location(world, x, y, z)

            if (location.block.biome !in TROLL_BIOMES) return@repeat
            if (shouldCancelSurfaceSpawn(location)) return@repeat

            val ground = location.clone().subtract(0.0, 1.0, 0.0).block
            if (!ground.isSolid) return@repeat

            val headSpace = location.clone().add(0.0, 2.0, 0.0).block
            if (!headSpace.isEmpty) return@repeat

            return location
        }

        return null
    }

    /**
     * Spawns a troll and a group of nearby trolls.
     *
     * @param world The world to spawn in.
     * @param location The center location to spawn around.
     */
    private fun spawnTrollEncounter(
        world: World,
        location: Location,
    ) {
        spawnSingleTroll(world, location)
        spawnTrollGroup(world, location)
    }

    /**
     * Spawns a single troll at the given location.
     *
     * @param world The world to spawn in.
     * @param location The location to spawn at.
     */
    private fun spawnSingleTroll(
        world: World,
        location: Location,
    ) {
        world.spawn(location, IronGolem::class.java) { troll ->
            tagTroll(troll)
            applyTrollAttributes(troll)
            troll.isPlayerCreated = false
        }
    }

    /**
     * Spawns a group of additional trolls around the initial troll.
     *
     * @param world The world to spawn in.
     * @param location The center location to spawn around.
     */
    private fun spawnTrollGroup(
        world: World,
        location: Location,
    ) {
        val groupSize = Random.nextInt(TROLL_GROUP_MIN_SIZE, TROLL_GROUP_MAX_SIZE)
        repeat(groupSize - 1) {
            val offsetX = Random.nextDouble(-TROLL_GROUP_RADIUS, TROLL_GROUP_RADIUS)
            val offsetZ = Random.nextDouble(-TROLL_GROUP_RADIUS, TROLL_GROUP_RADIUS)
            val spawnLocation = location.clone().add(offsetX, 0.0, offsetZ)
            spawnLocation.y = world.getHighestBlockYAt(spawnLocation).toDouble() + 1.0

            if (spawnLocation.block.biome !in TROLL_BIOMES) return@repeat
            if (shouldCancelSurfaceSpawn(spawnLocation)) return@repeat

            spawnSingleTroll(world, spawnLocation)
        }
    }

    /**
     * Tags an iron golem as a troll so it can be identified later.
     *
     * @param troll The iron golem to tag.
     */
    private fun tagTroll(troll: IronGolem) {
        troll.persistentDataContainer.set(TROLL_TAG_KEY, PersistentDataType.BOOLEAN, true)
    }

    /**
     * Checks whether an iron golem is a troll.
     *
     * @param troll The iron golem to check.
     * @return True if the iron golem is a troll.
     */
    private fun isTroll(troll: IronGolem): Boolean =
        troll.persistentDataContainer.has(TROLL_TAG_KEY, PersistentDataType.BOOLEAN)

    /**
     * Determines whether a surface troll spawn should be skipped due to daylight.
     *
     * @param location The spawn location to evaluate.
     * @return True if the spawn should be skipped because it is too bright on the surface.
     */
    private fun shouldCancelSurfaceSpawn(location: Location): Boolean {
        if (location.world.isThundering || !location.world.isDayTime) return false
        val blockLight = location.block.lightLevel
        return blockLight > TROLL_MAX_SURFACE_LIGHT && location.blockY >= location.world.seaLevel
    }

    /**
     * Applies troll attribute modifiers to an iron golem.
     *
     * @param troll The iron golem to transform.
     */
    private fun applyTrollAttributes(troll: IronGolem) {
        troll.getAttribute(Attribute.MAX_HEALTH)?.let {
            it.baseValue = it.value * TROLL_HEALTH_MULTIPLIER
            troll.health = it.baseValue
        }
        troll.getAttribute(Attribute.SCALE)?.let {
            it.baseValue = it.value * TROLL_SCALE_MULTIPLIER
        }
        troll.getAttribute(Attribute.ATTACK_DAMAGE)?.let {
            it.baseValue = it.value * TROLL_DAMAGE_MULTIPLIER
        }
        troll.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.baseValue = it.value * TROLL_SPEED_MULTIPLIER
        }
        troll.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.let {
            it.baseValue = TROLL_KNOCKBACK_RESISTANCE
        }
    }

    /**
     * Triggers regeneration for trolls that fall below the health threshold.
     *
     * @param event The EntityDamageEvent triggered when a troll takes damage.
     */
    private fun trollRegenTrigger(event: EntityDamageEvent) {
        val troll = event.entity as? IronGolem ?: return
        if (!isTroll(troll)) return
        val entityId = troll.entityId
        if (entityId in REGENERATING_TROLLS) return

        val maxHealth = troll.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        if (troll.health - event.finalDamage > maxHealth * TROLL_REGEN_THRESHOLD) return

        REGENERATING_TROLLS.add(entityId)
        troll.addPotionEffect(
            PotionEffect(
                PotionEffectType.REGENERATION,
                TROLL_REGEN_DURATION_TICKS,
                0,
                false,
                true,
            ),
        )
        troll.world.playSound(troll.location, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.7f)
    }

    /**
     * Replaces default iron golem drops with troll-themed loot.
     *
     * @param event The EntityDeathEvent triggered when a troll dies.
     */
    private fun trollDrops(event: EntityDeathEvent) {
        val troll = event.entity as? IronGolem ?: return
        if (!isTroll(troll)) return
        val killer = event.entity.killer
        val lootingLevel =
            killer?.inventory?.itemInMainHand?.getEnchantmentLevel(Enchantment.LOOTING) ?: 0

        event.drops.clear()
        event.droppedExp = 20

        TROLL_DROP_MATERIALS.forEach { (material, chance) ->
            val adjustedChance = chance + (lootingLevel * 0.05)
            if (Random.nextDouble() < adjustedChance) {
                val amount = Random.nextInt(TROLL_DROP_BASE_MIN, TROLL_DROP_BASE_MAX + 1) + lootingLevel
                event.drops.add(ItemStack.of(material, amount.coerceAtLeast(1)))
            }
        }

        if (Random.nextDouble() < 0.40 + (lootingLevel * 0.03)) {
            event.drops.add(ItemStack.of(Material.POPPY, Random.nextInt(0, 3)))
        }
    }
}
