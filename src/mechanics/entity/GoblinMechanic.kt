package org.xodium.illyriaplus.mechanics.entity

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.xodium.illyriaplus.mechanics.MechanicInterface
import kotlin.random.Random

/** Represents a mechanic that re-flavors zombies as cowardly cave-dwelling goblins. */
internal object GoblinMechanic : MechanicInterface {
    private const val GOBLIN_SPEED_MULTIPLIER: Double = 1.35
    private const val GOBLIN_BABY_CHANCE: Double = 0.70
    private const val GOBLIN_GEAR_CHANCE: Double = 0.40
    private const val GOBLIN_DAMAGE_CHANCE: Double = 0.50
    private const val GOBLIN_FLEE_HEALTH_THRESHOLD: Double = 0.30
    private const val GOBLIN_FLEE_DURATION_TICKS: Int = 100
    private const val GOBLIN_MAX_SURFACE_LIGHT: Int = 7

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

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) = goblinSpawn(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) = goblinFlee(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDeathEvent) = goblinDrops(event)

    /**
     * Modifies natural zombie spawns into smaller, faster goblins with crude gear.
     *
     * @param event The CreatureSpawnEvent triggered when a creature spawns.
     */
    private fun goblinSpawn(event: CreatureSpawnEvent) {
        if (event.entityType != EntityType.ZOMBIE) return
        val zombie = event.entity as? Zombie ?: return

        if (shouldCancelSurfaceSpawn(zombie.location)) {
            event.isCancelled = true
            return
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

        if (Random.nextDouble() < GOBLIN_GEAR_CHANCE) {
            equipGoblinGear(zombie)
        }

        zombie.setCanPickupItems(true)
    }

    /**
     * Determines whether a surface goblin spawn should be cancelled due to daylight.
     *
     * @param location The spawn location to evaluate.
     * @return True if the spawn should be cancelled because it is too bright on the surface.
     */
    private fun shouldCancelSurfaceSpawn(location: Location): Boolean {
        if (location.world.isThundering || !location.world.isDayTime) return false
        val blockLight = location.block.lightLevel
        return blockLight > GOBLIN_MAX_SURFACE_LIGHT && location.blockY >= location.world.seaLevel
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

    private fun goblinFlee(event: EntityDamageEvent) {
        // TODO: implement goblin flee logic
    }

    private fun goblinDrops(event: EntityDeathEvent) {
        // TODO: implement goblin drop logic
    }
}
