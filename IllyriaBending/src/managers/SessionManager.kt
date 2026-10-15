package org.xodium.illyriabending.managers

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Manages per-player active sessions for abilities. */
internal object SessionManager {
    /** Active sessions keyed by ability instance, then player UUID. */
    private val sessions = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Session>>()

    /**
     * Starts a new session with a repeating tick task.
     *
     * @param ability The ability to tick.
     * @param uuid The player's UUID.
     * @param periodTicks Ticks between each tick call.
     * @return The created session.
     */
    fun start(
        ability: AbilityInterface,
        uuid: UUID,
        periodTicks: Long = 1L,
    ): Session {
        val session = Session(instance.server.getPlayer(uuid)!!)
        sessions.getOrPut(ability) { ConcurrentHashMap() }[uuid] = session
        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { ability.tickSession(uuid) },
                0L,
                periodTicks,
            )
        return session
    }

    /**
     * Gets an active session.
     *
     * @param ability The ability.
     * @param uuid The player's UUID.
     * @return The session, or null if not active.
     */
    fun get(
        ability: AbilityInterface,
        uuid: UUID,
    ): Session? = sessions[ability]?.get(uuid)

    /**
     * Checks if a player has an active session.
     *
     * @param ability The ability.
     * @param uuid The player's UUID.
     * @return True if session is active.
     */
    fun has(
        ability: AbilityInterface,
        uuid: UUID,
    ): Boolean = sessions[ability]?.containsKey(uuid) == true

    /**
     * Cancels and removes a session.
     *
     * @param ability The ability.
     * @param uuid The player's UUID.
     */
    fun remove(
        ability: AbilityInterface,
        uuid: UUID,
    ) {
        sessions[ability]?.remove(uuid)?.cancel()
    }

    /**
     * Cancels and removes all sessions for an ability.
     *
     * @param ability The ability.
     */
    fun clear(ability: AbilityInterface) {
        sessions[ability]?.values?.forEach { it.cancel() }
        sessions.remove(ability)
    }
}

/**
 * Represents an active ability session with task management.
 *
 * @property player The player this session belongs to.
 */
internal class Session(
    val player: Player,
) {
    /** The scheduler task for this session. */
    var task: BukkitTask? = null

    /** Cancels the session task if active. */
    fun cancel() {
        task?.cancel()
    }
}
