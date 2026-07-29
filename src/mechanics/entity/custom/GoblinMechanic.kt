package org.xodium.illyriaplus.mechanics.entity.custom

import org.bukkit.Difficulty
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.Biome
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Represents a mechanic that spawns cowardly cave-dwelling goblins as their own mob type. */
internal object GoblinMechanic : CustomMobInterface<Zombie> {
    override val tagKey: NamespacedKey = NamespacedKey(instance, "is_goblin")
    override val spawnIntervalTicks: Long = 400L
    override val spawnChance: Double = 0.25
    override val spawnMinRadius: Double = 16.0
    override val spawnMaxRadius: Double = 48.0
    override val spawnAttempts: Int = 8
    override val maxNearby: Int = 6
    override val maxPlayersPerTick: Int = 3
    override val maxSurfaceLight: Int = 7
    override val biomes: Set<Biome> =
        setOf(
            Biome.DARK_FOREST,
            Biome.SWAMP,
            Biome.MANGROVE_SWAMP,
            Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.OLD_GROWTH_SPRUCE_TAIGA,
        )

    private const val GOBLIN_SPEED_MULTIPLIER: Double = 1.35
    private const val GOBLIN_SCALE_MULTIPLIER: Double = 0.60
    private const val GOBLIN_BABY_CHANCE: Double = 0.70
    private const val GOBLIN_GROUP_MIN_SIZE: Int = 2
    private const val GOBLIN_GROUP_MAX_SIZE: Int = 5
    private const val GOBLIN_GROUP_RADIUS: Double = 3.0
    private const val GOBLIN_GEAR_CHANCE: Double = 0.40
    private const val GOBLIN_DAMAGE_CHANCE: Double = 0.50
    private const val GOBLIN_DROP_WEAPON_CHANCE: Double = 0.05
    private const val GOBLIN_DROP_BASE_MIN: Int = 1
    private const val GOBLIN_DROP_BASE_MAX: Int = 3
    private const val GOBLIN_FLEE_HEALTH_THRESHOLD: Double = 0.30
    private const val GOBLIN_FLEE_DURATION_TICKS: Int = 100
    private const val GOBLIN_FLEE_SPEED_MULTIPLIER: Double = 1.75

    private val GOBLIN_DROP_MATERIALS =
        mapOf(
            Material.EMERALD to 0.35,
            Material.GOLD_NUGGET to 0.45,
            Material.IRON_NUGGET to 0.25,
            Material.RAW_COPPER to 0.30,
        )

    private val FLEEING_GOBLINS = mutableSetOf<Int>()

    private val GOBLIN_WEAPONS =
        listOf(
            Material.WOODEN_SWORD,
            Material.WOODEN_AXE,
            Material.STONE_SWORD,
            Material.STONE_AXE,
            Material.STONE_PICKAXE,
        )

    private val GOBLIN_ARMOR =
        listOf(
            Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS,
        )

    override fun register(): Long = super.register(GOBLIN_GROUP_MIN_SIZE..GOBLIN_GROUP_MAX_SIZE)

    override fun spawnMob(world: org.bukkit.World, location: Location) {
        world.spawn(location, Zombie::class.java) { goblin ->
            tagMob(goblin)
            applyGoblinAttributes(goblin)
            if (Random.nextDouble() < GOBLIN_GEAR_CHANCE) {
                equipGoblinGear(goblin)
            }
            goblin.setCanPickupItems(true)
        }
    }

    override fun countNearby(location: Location): Int {
        if (location.world.difficulty == Difficulty.PEACEFUL) return 0
        return super.countNearby(location)
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) = goblinFlee(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDeathEvent) = goblinDrops(event)

    /**
     * Applies goblin attribute modifiers to a zombie.
     *
     * @param zombie The zombie to transform.
     */
    private fun applyGoblinAttributes(zombie: Zombie) {
        zombie.getAttribute(Attribute.SCALE)?.let {
            it.baseValue = it.value * GOBLIN_SCALE_MULTIPLIER
        }
        zombie.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.baseValue = it.value * GOBLIN_SPEED_MULTIPLIER
        }
        zombie.getAttribute(Attribute.ATTACK_SPEED)?.let {
            it.baseValue = it.value * GOBLIN_SPEED_MULTIPLIER
        }
        zombie.getAttribute(Attribute.FOLLOW_RANGE)?.let {
            it.baseValue = it.value * 0.75
        }

        if (Random.nextDouble() < GOBLIN_BABY_CHANCE) {
            zombie.isBaby = true
        }
    }

    /**
     * Equips a zombie with randomized crude goblin gear and optional damage.
     *
     * @param zombie The zombie to equip.
     */
    private fun equipGoblinGear(zombie: Zombie) {
        val equipment = zombie.equipment ?: return

        if (Random.nextDouble() < GOBLIN_GEAR_CHANCE) {
            val weapon = ItemStack.of(GOBLIN_WEAPONS.random())
            if (Random.nextDouble() < GOBLIN_DAMAGE_CHANCE) {
                damageItem(weapon)
            }
            equipment.setItemInMainHand(weapon)
        }

        GOBLIN_ARMOR.forEach { material ->
            if (Random.nextDouble() < GOBLIN_GEAR_CHANCE / GOBLIN_ARMOR.size) {
                val armor = ItemStack.of(material)
                if (Random.nextDouble() < GOBLIN_DAMAGE_CHANCE) {
                    damageItem(armor)
                }
                when (material) {
                    Material.LEATHER_HELMET -> equipment.setItem(EquipmentSlot.HEAD, armor)
                    Material.LEATHER_CHESTPLATE -> equipment.setItem(EquipmentSlot.CHEST, armor)
                    Material.LEATHER_LEGGINGS -> equipment.setItem(EquipmentSlot.LEGS, armor)
                    Material.LEATHER_BOOTS -> equipment.setItem(EquipmentSlot.FEET, armor)
                    else -> Unit
                }
            }
        }

        equipment.helmetDropChance = 0.05f
        equipment.chestplateDropChance = 0.05f
        equipment.leggingsDropChance = 0.05f
        equipment.bootsDropChance = 0.05f
        equipment.itemInMainHandDropChance = 0.05f
    }

    /**
     * Applies random durability damage to an item stack.
     *
     * @param item The item stack to damage.
     */
    private fun damageItem(item: ItemStack) {
        val meta = item.itemMeta as? Damageable ?: return
        if (meta.isUnbreakable) return
        val maxDurability = item.type.maxDurability.toInt()
        if (maxDurability <= 0) return
        val damage = Random.nextInt(maxDurability / 2, maxDurability + 1)
        meta.damage = damage.coerceAtMost(maxDurability)
        item.itemMeta = meta
    }

    /**
     * Makes goblins panic and flee when their health drops below a threshold.
     *
     * @param event The EntityDamageEvent triggered when a goblin takes damage.
     */
    private fun goblinFlee(event: EntityDamageEvent) {
        val zombie = event.entity as? Zombie ?: return
        if (!isMob(zombie)) return
        val entityId = zombie.entityId
        if (entityId in FLEEING_GOBLINS) return

        val maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        if (zombie.health - event.finalDamage > maxHealth * GOBLIN_FLEE_HEALTH_THRESHOLD) return

        FLEEING_GOBLINS.add(entityId)
        zombie.target = null
        zombie.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.baseValue = it.value * GOBLIN_FLEE_SPEED_MULTIPLIER
        }
        zombie.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED,
                GOBLIN_FLEE_DURATION_TICKS,
                1,
                false,
                false,
            ),
        )
        zombie.world.playSound(zombie.location, Sound.ENTITY_FOX_HURT, 1.0f, 1.5f)
        zombie.world.spawnParticle(Particle.SWEEP_ATTACK, zombie.location, 8, 0.3, 0.3, 0.3)

        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                FLEEING_GOBLINS.remove(entityId)
                if (!zombie.isDead) {
                    zombie.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                        it.baseValue = it.value / GOBLIN_FLEE_SPEED_MULTIPLIER
                    }
                }
            },
            GOBLIN_FLEE_DURATION_TICKS.toLong(),
        )
    }

    /**
     * Replaces default zombie drops with goblin-themed loot.
     *
     * @param event The EntityDeathEvent triggered when a goblin dies.
     */
    private fun goblinDrops(event: EntityDeathEvent) {
        val zombie = event.entity as? Zombie ?: return
        if (!isMob(zombie)) return
        val killer = event.entity.killer
        val lootingLevel =
            killer?.inventory?.itemInMainHand?.getEnchantmentLevel(Enchantment.LOOTING) ?: 0

        event.drops.clear()
        event.droppedExp = 8

        GOBLIN_DROP_MATERIALS.forEach { (material, chance) ->
            val adjustedChance = chance + (lootingLevel * 0.05)
            if (Random.nextDouble() < adjustedChance) {
                val amount = Random.nextInt(GOBLIN_DROP_BASE_MIN, GOBLIN_DROP_BASE_MAX + 1) + lootingLevel
                event.drops.add(ItemStack.of(material, amount.coerceAtLeast(1)))
            }
        }

        if (Random.nextDouble() < GOBLIN_DROP_WEAPON_CHANCE + (lootingLevel * 0.02)) {
            val weapon = ItemStack.of(GOBLIN_WEAPONS.random())
            if (Random.nextDouble() < GOBLIN_DAMAGE_CHANCE) {
                damageItem(weapon)
            }
            event.drops.add(weapon)
        }

        if (Random.nextDouble() < 0.25) {
            event.drops.add(ItemStack.of(Material.ROTTEN_FLESH, Random.nextInt(1, 3)))
        }
    }
}
