package org.xodium.illyriabending.abilities

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Represents a contract for a bending ability within the system. */
internal interface AbilityInterface {
    /** The cooldown in milliseconds after this ability completes. */
    val cooldownMs: Long

    /** How the player activates this ability. */
    val activationType: ActivationType

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
     * Gets the remaining cooldown time in milliseconds for a player.
     *
     * @param player The player to check.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    fun getCooldownRemaining(player: Player): Long {
        val endTime = COOLDOWNS[this]?.get(player.uniqueId) ?: return 0L
        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Checks if a player is currently on cooldown.
     *
     * @param player The player to check.
     * @return True if the ability is on cooldown for this player.
     */
    fun isOnCooldown(player: Player): Boolean = getCooldownRemaining(player) > 0L

    /**
     * Sets the cooldown for a player.
     *
     * @param player The player to set cooldown for.
     */
    fun setCooldown(player: Player) {
        COOLDOWNS.getOrPut(this) { ConcurrentHashMap() }[player.uniqueId] =
            System.currentTimeMillis() + cooldownMs
    }

    /**
     * Called when the ability completes successfully. Sets the cooldown.
     *
     * @param player The player who completed the ability.
     */
    fun complete(player: Player) = setCooldown(player)

    // ============ Session Management ============

    /**
     * Whether this ability tracks per-player session state (e.g., AirBlade charging,
     * AirWheel riding). Override to true if the ability has sessions.
     */
    val hasSession: Boolean
        get() = false

    /**
     * Starts a new session for the player. Only called if [hasSession] is true.
     *
     * @param player The player starting the session.
     * @param level The player's bending level.
     */
    fun startSession(
        player: Player,
        level: Int,
    ) {}

    /**
     * Cancels and cleans up a player's session.
     *
     * @param player The player whose session to cancel.
     */
    fun cancelSession(player: Player) {}

    /**
     * Checks if a player has an active session for this ability.
     *
     * @param player The player to check.
     * @return True if the player has an active session.
     */
    fun hasActiveSession(player: Player): Boolean = false

    /**
     * Called every tick for active sessions.
     *
     * @param uuid The player's UUID.
     */
    fun tickSession(uuid: UUID) {}

    companion object {
        /** Global cooldown storage keyed by ability instance, then player UUID. */
        private val COOLDOWNS = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Long>>()

        /** Active sessions keyed by ability instance, then player UUID. */
        private val SESSIONS = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Session>>()

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

        /**
         * Starts a repeating task for an ability session.
         *
         * @param ability The ability to tick.
         * @param uuid The player's UUID.
         * @param periodTicks Ticks between each tick call.
         * @return The created BukkitTask.
         */
        fun startSessionTask(
            ability: AbilityInterface,
            uuid: UUID,
            periodTicks: Long = 1L,
        ): BukkitTask =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { ability.tickSession(uuid) },
                0L,
                periodTicks,
            )

        /**
         * Gets the session map for an ability.
         *
         * @param ability The ability to get sessions for.
         * @return The session map, or null if none exists.
         */
        fun getSessions(ability: AbilityInterface): MutableMap<UUID, Session>? = SESSIONS[ability]

        /**
         * Gets or creates the session map for an ability.
         *
         * @param ability The ability to get sessions for.
         * @return The session map.
         */
        fun getOrCreateSessions(ability: AbilityInterface): MutableMap<UUID, Session> =
            SESSIONS.getOrPut(ability) { ConcurrentHashMap() }

        /**
         * Removes a player's session for an ability.
         *
         * @param ability The ability.
         * @param uuid The player's UUID.
         */
        fun removeSession(
            ability: AbilityInterface,
            uuid: UUID,
        ) {
            SESSIONS[ability]?.remove(uuid)?.task?.cancel()
        }
    }
}

/**
 * Represents an active ability session with task management.
 */
internal class Session(
    /** The player this session belongs to. */
    val player: Player,
    /** The scheduler task for this session. */
    var task: BukkitTask? = null,
) {
    /** Cancels the session task if active. */
    fun cancel() {
        task?.cancel()
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
