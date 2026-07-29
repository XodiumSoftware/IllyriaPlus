package org.xodium.illyriaplus.mechanics.entity.custom

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.Biome
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Schedule.schedule
import kotlin.random.Random

/** Represents a mechanic that spawns slow, tanky cave trolls in mountain and spruce forest biomes. */
internal object TrollMechanic : CustomMobInterface<IronGolem> {
    override val tagKey: NamespacedKey = NamespacedKey(instance, "is_troll")
    override val spawnIntervalTicks: Long = 600L
    override val spawnChance: Double = 0.15
    override val spawnMinRadius: Double = 24.0
    override val spawnMaxRadius: Double = 64.0
    override val spawnAttempts: Int = 6
    override val maxNearby: Int = 3
    override val maxPlayersPerTick: Int = 2
    override val maxSurfaceLight: Int = 10
    override val biomes: Set<Biome> =
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

    private val TROLL_DROP_MATERIALS =
        mapOf(
            Material.COBBLESTONE to 0.50,
            Material.RAW_IRON to 0.30,
            Material.BONE to 0.40,
            Material.SPRUCE_LOG to 0.20,
            Material.MOSSY_COBBLESTONE to 0.15,
        )

    private val REGENERATING_TROLLS = mutableSetOf<Int>()

    override fun register(): Long = super.register(TROLL_GROUP_MIN_SIZE..TROLL_GROUP_MAX_SIZE)

    override fun spawnMob(world: org.bukkit.World, location: Location) {
        world.spawn(location, IronGolem::class.java) { troll ->
            tagMob(troll)
            applyTrollAttributes(troll)
            troll.isPlayerCreated = false
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageEvent) = trollRegenTrigger(event)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDeathEvent) = trollDrops(event)

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
                    .firstOrNull { it.entityId == entityId && isMob(it) }
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
     * Triggers regeneration for trolls that fall below the health threshold.
     *
     * @param event The EntityDamageEvent triggered when a troll takes damage.
     */
    private fun trollRegenTrigger(event: EntityDamageEvent) {
        val troll = event.entity as? IronGolem ?: return
        if (!isMob(troll)) return
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
        if (!isMob(troll)) return
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
