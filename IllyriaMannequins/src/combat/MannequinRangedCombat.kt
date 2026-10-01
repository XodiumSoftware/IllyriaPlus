package org.xodium.illyriamannequins.combat

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.Firework
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.SpectralArrow
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.util.Vector
import org.xodium.illyriamannequins.MannequinPDC.arrows
import kotlin.math.cos
import kotlin.math.sin

/** Handles ranged combat for mannequins: bows, crossbows, and kiting. */
@Suppress("UnstableApiUsage")
internal object MannequinRangedCombat {
    /** The squared distance beyond which a mannequin does not attempt a ranged shot. */
    const val ATTACK_RANGE_SQUARED = 400.0

    /** The squared distance within which a mannequin kites backward between shots instead of firing. */
    const val STANDOFF_RANGE_SQUARED = 64.0

    /** The horizontal speed in blocks per tick at which a kiting mannequin backs away. */
    const val KITE_SPEED = 0.216

    /** The speed multiplier applied to launched arrows. */
    private const val ARROW_SPEED = 3.0

    /** The speed multiplier applied to launched firework rockets. */
    private const val FIREWORK_SPEED = 1.6

    /** The lateral spread between multishot projectiles, in radians (~10 degrees). */
    private const val MULTISHOT_SPREAD = 0.1745329

    /**
     * Checks if a mannequin is holding a bow or crossbow in its main hand.
     *
     * @param mannequin The mannequin to check.
     * @return `true` if a ranged weapon is held.
     */
    fun hasBow(mannequin: Mannequin): Boolean =
        mannequin.equipment.getItem(EquipmentSlot.HAND).type.let {
            it == Material.BOW || it == Material.CROSSBOW
        }

    /**
     * Returns the cooldown in ticks between shots for the held ranged weapon.
     *
     * @param mannequin The mannequin whose weapon to inspect.
     * @return The cooldown in ticks between shots.
     */
    fun cooldownTicks(mannequin: Mannequin): Int =
        if (mannequin.equipment.getItem(EquipmentSlot.HAND).type == Material.CROSSBOW) 25 else 20

    /**
     * Fires an arrow (or firework rocket) from a mannequin's [MannequinPDC.arrows] stack.
     *
     * Consumes 1 arrow from the stack unless the weapon has Infinity. Crossbows with
     * Multishot fire a spread of 3 projectiles. Crossbows loaded with
     * [Material.FIREWORK_ROCKET] fire a [Firework] instead.
     *
     * @param mannequin The mannequin doing the shooting.
     * @param target The entity to aim at.
     */
    fun fire(
        mannequin: Mannequin,
        target: LivingEntity,
    ) {
        val weapon = mannequin.equipment.getItem(EquipmentSlot.HAND)
        val arrowStack = mannequin.arrows ?: return
        if (arrowStack.isEmpty) return

        mannequin.swingMainHand()
        when (weapon.type) {
            Material.BOW -> {
                mannequin.world.playSound(mannequin.location, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f)
                fireProjectiles(mannequin, target, arrowStack, multishot = false)
            }

            Material.CROSSBOW -> {
                mannequin.world.playSound(mannequin.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f)
                fireProjectiles(
                    mannequin,
                    target,
                    arrowStack,
                    multishot =
                        weapon.getEnchantmentLevel(Enchantment.MULTISHOT) > 0,
                )
            }

            else -> {
                return
            }
        }

        if (weapon.getEnchantmentLevel(Enchantment.INFINITY) <= 0) {
            mannequin.arrows = arrowStack.clone().apply { amount-- }.takeIf { it.amount > 0 }
        }
        mannequin.damageItemStack(EquipmentSlot.HAND, 1)
    }

    /**
     * Launches one or more projectiles at a target from a mannequin.
     *
     * @param mannequin The shooter.
     * @param target The entity to aim at.
     * @param arrowStack The arrow stack used to derive the projectile type.
     * @param multishot Whether to fire a spread of 3 projectiles.
     */
    private fun fireProjectiles(
        mannequin: Mannequin,
        target: LivingEntity,
        arrowStack: ItemStack,
        multishot: Boolean,
    ) {
        val origin = mannequin.eyeLocation.clone().add(0.0, -0.1, 0.0)
        val aimAt = target.eyeLocation.clone().subtract(0.0, target.eyeHeight * 0.35, 0.0)
        val base = aimAt.toVector().subtract(origin.toVector())
        if (base.lengthSquared() == 0.0) return
        base.normalize()

        val angles = if (multishot) doubleArrayOf(-MULTISHOT_SPREAD, 0.0, MULTISHOT_SPREAD) else doubleArrayOf(0.0)
        angles.forEach { angle ->
            spawnProjectile(mannequin, arrowStack, origin, rotateAroundY(base, angle))
        }
    }

    /**
     * Spawns an arrow or firework projectile for a mannequin.
     *
     * @param mannequin The shooter.
     * @param arrowStack The item stack to derive the projectile from.
     * @param origin The spawn location.
     * @param direction The normalized launch direction.
     */
    private fun spawnProjectile(
        mannequin: Mannequin,
        arrowStack: ItemStack,
        origin: Location,
        direction: Vector,
    ) {
        val world = mannequin.world
        val weaponType = mannequin.equipment.getItem(EquipmentSlot.HAND).type
        when (arrowStack.type) {
            Material.FIREWORK_ROCKET -> {
                if (weaponType != Material.CROSSBOW) {
                    spawnArrow(world, mannequin, origin, direction)
                    return
                }
                world.spawn(origin, Firework::class.java) {
                    (arrowStack.itemMeta as? FireworkMeta)?.let { meta -> it.fireworkMeta = meta.clone() }
                    it.velocity = direction.clone().multiply(FIREWORK_SPEED)
                    it.shooter = mannequin
                }
            }

            Material.SPECTRAL_ARROW -> {
                world
                    .spawnArrow(origin, direction, ARROW_SPEED.toFloat(), 0.0f, SpectralArrow::class.java)
                    .apply { shooter = mannequin }
            }

            Material.TIPPED_ARROW -> {
                spawnArrow(world, mannequin, origin, direction) { copyPotionEffects(arrowStack, it) }
            }

            else -> {
                spawnArrow(world, mannequin, origin, direction)
            }
        }
    }

    /**
     * Spawns a regular [Arrow] for a mannequin.
     *
     * @param world The world to spawn in.
     * @param mannequin The shooter.
     * @param origin The spawn location.
     * @param direction The normalized launch direction.
     * @param configure Additional configuration applied after the arrow is spawned.
     */
    private fun spawnArrow(
        world: World,
        mannequin: Mannequin,
        origin: Location,
        direction: Vector,
        configure: (Arrow) -> Unit = {},
    ) = world
        .spawnArrow(origin, direction, ARROW_SPEED.toFloat(), 0.0f, Arrow::class.java)
        .apply {
            shooter = mannequin
            pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            configure(this)
        }

    /**
     * Copies potion effects from a tipped arrow item onto a spawned [Arrow] entity.
     *
     * @param arrowStack The [Material.TIPPED_ARROW] stack to copy effects from.
     * @param arrow The spawned arrow entity to apply effects to.
     */
    private fun copyPotionEffects(
        arrowStack: ItemStack,
        arrow: Arrow,
    ) {
        val meta = arrowStack.itemMeta as? PotionMeta ?: return
        meta.basePotionType?.let { arrow.basePotionType = it }
        meta.customEffects.forEach { effect ->
            arrow.addCustomEffect(effect.type.createEffect(effect.duration, effect.amplifier), true)
        }
        meta.color?.let { arrow.color = it }
    }

    /**
     * Rotates a vector around the Y axis.
     *
     * @param v The vector to rotate.
     * @param angleRadians The rotation angle in radians.
     * @return The rotated vector.
     */
    private fun rotateAroundY(
        v: Vector,
        angleRadians: Double,
    ): Vector {
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        return Vector(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos)
    }
}
