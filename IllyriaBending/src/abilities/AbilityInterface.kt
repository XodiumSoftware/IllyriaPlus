package org.xodium.illyriabending.abilities

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.managers.CooldownManager
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID

/** Represents a contract for a bending ability within the system. */
internal interface AbilityInterface {
    /** The cooldown in milliseconds after this ability completes. */
    val cooldownMs: Long

    /** How the player activates this ability. */
    val activationType: ActivationType

    /**
     * Whether this ability tracks per-player session state (e.g., AirBlade charging,
     * AirWheel riding). Override to true if the ability has sessions.
     */
    val hasSession: Boolean
        get() = false

    /**
     * Called when the player activates the ability (sneak-hold with focus item).
     *
     * @param player The casting player.
     * @param level The player's bending level, in `1..5`.
     */
    fun activate(
        player: Player,
        level: Int,
    )

    /**
     * Called every tick for active sessions. Only called if [hasSession] is true.
     *
     * @param uuid The player's UUID.
     */
    fun tickSession(uuid: UUID) {}

    /**
     * Gets the remaining cooldown time in milliseconds for a player.
     *
     * @param player The player to check.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    fun getCooldownRemaining(player: Player): Long = CooldownManager.getRemaining(this, player.uniqueId)

    /**
     * Checks if a player is currently on cooldown.
     *
     * @param player The player to check.
     * @return True if the ability is on cooldown for this player.
     */
    fun isOnCooldown(player: Player): Boolean = CooldownManager.isOnCooldown(this, player.uniqueId)

    /**
     * Called when the ability completes successfully. Sets the cooldown.
     *
     * @param player The player who completed the ability.
     */
    fun complete(player: Player) = CooldownManager.set(this, player.uniqueId, cooldownMs)

    /**
     * Checks if a player has an active session for this ability.
     *
     * @param player The player to check.
     * @return True if the player has an active session.
     */
    fun hasActiveSession(player: Player): Boolean = SessionManager.has(this, player.uniqueId)

    companion object {
        /**
         * Rotates a vector around an axis using Rodrigues' rotation formula.
         *
         * @param vec The vector to rotate.
         * @param axis The rotation axis; will be normalized before use.
         * @param angle The rotation angle in radians.
         * @return The rotated vector.
         */
        fun rotate(
            vec: Vector,
            axis: Vector,
            angle: Double,
        ): Vector {
            val k = axis.clone().normalize()
            val cosA = kotlin.math.cos(angle)
            val sinA = kotlin.math.sin(angle)
            val term1 = vec.clone().multiply(cosA)
            val term2 = k.clone().crossProduct(vec).multiply(sinA)
            val term3 = k.clone().multiply(k.dot(vec) * (1.0 - cosA))
            return term1.add(term2).add(term3)
        }

        /**
         * Gets living entities within a radius of a location.
         *
         * @param location The center location.
         * @param radius The search radius.
         * @return List of nearby living entities.
         */
        fun getNearEntities(
            location: org.bukkit.Location,
            radius: Double,
        ): List<LivingEntity> =
            location
                .world
                ?.getNearbyEntities(location, radius, radius, radius)
                ?.filterIsInstance<LivingEntity>()
                ?: emptyList()

        /**
         * Damages entities and applies knockback away from a source location.
         *
         * @param player The player dealing damage.
         * @param entities The entities to damage.
         * @param damage Amount of damage in hearts.
         * @param source The location to knock entities away from.
         * @param knockback Horizontal knockback strength.
         * @param knockup Vertical knockback strength.
         */
        fun damageEntities(
            player: Player,
            entities: List<LivingEntity>,
            damage: Double,
            source: org.bukkit.Location,
            knockback: Double = 0.0,
            knockup: Double = 0.0,
        ) {
            entities.forEach { entity ->
                entity.damage(damage, player)
                if (knockback > 0.0 || knockup > 0.0) {
                    val push =
                        entity
                            .location
                            .toVector()
                            .subtract(source.toVector())
                            .normalize()
                            .apply { y = knockup }
                            .multiply(knockback)
                    entity.velocity = entity.velocity.add(push)
                }
            }
        }
    }
}

/** Defines how an ability is activated by the player. */
internal enum class ActivationType {
    /** One-shot cast with no duration (AirBlast, AirAgility). */
    INSTANT,

    /** Hold to charge, release to launch (AirBlade). */
    CHARGE,

    /** Activate to enable, activate again to disable (AirWheel). */
    TOGGLE,

    /** Active for a fixed duration then expires (AirScooter). */
    DURATION,
}
