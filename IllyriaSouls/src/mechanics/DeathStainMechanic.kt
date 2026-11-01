package org.xodium.illyriasouls.mechanics

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriasouls.IllyriaSouls
import org.xodium.illyriasouls.PlayerPDC.clearStain
import org.xodium.illyriasouls.PlayerPDC.stainLocation
import org.xodium.illyriasouls.PlayerPDC.stainedSouls
import org.xodium.illyriasouls.items.SoulFragmentItem
import java.util.UUID
import kotlin.time.measureTime

/**
 * Creates and manages death stains: recoverable stores of lost souls that persist in the world
 * until the player reclaims them. Dying again while holding a stain destroys part of the
 * previous souls and replaces the stain at the new death location.
 */
internal object DeathStainMechanic : MechanicInterface {
    /** Fraction of souls lost when a player dies while already holding an active stain. */
    private const val REPEAT_DEATH_PENALTY: Double = 0.25

    /** Maximum distance at which a player can reclaim their stain. */
    private const val RECLAIM_RADIUS: Double = 3.0

    /** Hollowed state duration in seconds. Cleared early if the stain is reclaimed. */
    private const val HOLLOWED_DURATION_SECONDS: Int = 300

    /** Max-health reduction applied while Hollowed. */
    private const val HOLLOWED_HEALTH_PENALTY: Double = 4.0

    /** Internal PDC tag storing the UUID of the player who owns a stain entity. */
    private val STAIN_OWNER_KEY = NamespacedKey(IllyriaSouls.instance, "stain_owner")

    private val activeStains = mutableMapOf<UUID, ArmorStand>()
    private var cleanupTask: BukkitTask? = null

    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: PlayerDeathEvent) = handleDeath(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerRespawnEvent) = applyHollowed(event.player)

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractAtEntityEvent) = tryReclaim(event.player, event.rightClicked)

    @EventHandler(ignoreCancelled = true)
    fun on(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        tryReclaim(attacker, event.entity)
    }

    @EventHandler
    fun on(event: EntityDamageEvent) {
        val stand = event.entity as? ArmorStand ?: return
        if (isStain(stand)) event.isCancelled = true
    }

    @EventHandler
    fun on(event: ChunkUnloadEvent) = removeLostStains(event.chunk.entities.toList())

    override fun register(): Long {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(IllyriaSouls.instance, ::cleanupDisconnectedOwners, 0L, 200L)
        return measureTime {
            IllyriaSouls
                .instance
                .server
                .pluginManager
                .registerEvents(this, IllyriaSouls.instance)
        }.inWholeMilliseconds
    }

    override fun onDisable() {
        cleanupTask?.cancel()
        activeStains.values.forEach { it.remove() }
        activeStains.clear()
    }

    /** Handles a player death: penalize any existing stain, store the new one, and spawn a marker. */
    private fun handleDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val lostXp = event.newTotalExp

        event.newTotalExp = 0
        val previousAmount = player.stainedSouls
        val newStainAmount: Int

        if (previousAmount > 0) {
            val remaining = (previousAmount * (1.0 - REPEAT_DEATH_PENALTY)).toInt()
            newStainAmount = remaining + lostXp
            player.sendMessage(
                MM.deserialize(
                    "<dark_red>You died again. " +
                        "${(REPEAT_DEATH_PENALTY * 100).toInt()}% of your lingering souls were lost.</dark_red>",
                ),
            )
        } else {
            newStainAmount = lostXp
        }

        activeStains[player.uniqueId]?.remove()
        activeStains.remove(player.uniqueId)

        if (newStainAmount <= 0) {
            player.clearStain()
            return
        }

        player.stainedSouls = newStainAmount
        player.stainLocation = player.location
        spawnStain(player, player.location, newStainAmount)

        player.sendMessage(
            MM.deserialize(
                "<dark_red>You died and left a stain at your death location. " +
                    "Reclaim it before you die again.</dark_red>",
            ),
        )
    }

    /** Spawns (or re-spawns) a visible stain marker for the given player and amount. */
    private fun spawnStain(
        player: Player,
        location: Location,
        amount: Int,
    ) {
        activeStains[player.uniqueId]?.remove()

        val stand =
            location.world.spawn(location, ArmorStand::class.java) { stand ->
                stand.isInvisible = true
                stand.isMarker = true
                stand.isInvulnerable = true
                stand.isCollidable = false
                stand.isSmall = true
                stand.setGravity(false)
                stand.setCanMove(false)
                stand.setCanTick(false)
                stand.equipment.setItem(EquipmentSlot.HEAD, SoulFragmentItem())
                stand.persistentDataContainer.set(
                    STAIN_OWNER_KEY,
                    PersistentDataType.STRING,
                    player.uniqueId.toString(),
                )
                stand.customName(
                    MM.deserialize("${player.name}'s <dark_red>$amount</dark_red> lost souls"),
                )
                stand.isCustomNameVisible = true
            }

        activeStains[player.uniqueId] = stand
    }

    /** Attempts to reclaim a stain if the entity is a stain owned by the player. */
    private fun tryReclaim(
        player: Player,
        entity: Entity,
    ) {
        val stand = entity as? ArmorStand ?: return
        if (!isStain(stand)) return

        val ownerId =
            stand.persistentDataContainer.get(STAIN_OWNER_KEY, PersistentDataType.STRING)?.let(UUID::fromString)
        if (ownerId != player.uniqueId) return

        val location = stand.location
        if (player.location.distanceSquared(location) > RECLAIM_RADIUS * RECLAIM_RADIUS) {
            player.sendMessage(
                MM.deserialize("<gray>Move closer to reclaim your lost souls.</gray>"),
            )
            return
        }

        val amount = player.stainedSouls
        if (amount <= 0) {
            removeStain(player, stand)
            return
        }

        player.giveExp(amount)
        player.clearStain()
        removeStain(player, stand)
        removeHollowed(player)

        player.sendMessage(
            MM.deserialize("<gold>You reclaimed <dark_red>$amount</dark_red> lost souls.</gold>"),
        )
    }

    /** Returns whether the given armor stand is a death-stain marker. */
    private fun isStain(stand: ArmorStand): Boolean =
        stand.persistentDataContainer.has(STAIN_OWNER_KEY, PersistentDataType.STRING)

    /** Removes the stain marker and clears its tracking entry. */
    private fun removeStain(
        player: Player,
        stand: ArmorStand,
    ) {
        stand.remove()
        activeStains.remove(player.uniqueId)
    }

    /** Applies a Hollowed state to the player: weakness and reduced max health. */
    private fun applyHollowed(player: Player) {
        if (player.stainedSouls <= 0) return

        player.addPotionEffect(
            PotionEffect(PotionEffectType.WEAKNESS, HOLLOWED_DURATION_SECONDS * 20, 0, false, true, true),
        )

        val healthAttr = player.getAttribute(Attribute.MAX_HEALTH) ?: return
        val modifier =
            AttributeModifier(
                NamespacedKey(IllyriaSouls.instance, "hollowed_health"),
                -HOLLOWED_HEALTH_PENALTY,
                AttributeModifier.Operation.ADD_NUMBER,
            )
        healthAttr.removeModifier(modifier)
        healthAttr.addModifier(modifier)

        player.sendMessage(
            MM.deserialize(
                "<gray>You are <dark_red>Hollowed</dark_red>. Reclaim your stain to restore yourself.</gray>",
            ),
        )
    }

    /** Removes the Hollowed health penalty from a player. */
    private fun removeHollowed(player: Player) {
        player.getAttribute(Attribute.MAX_HEALTH)?.let { attr ->
            attr
                .modifiers
                .filter { it.key.key == "hollowed_health" }
                .forEach { attr.removeModifier(it) }
        }
    }

    /** Cleans up any stains whose owners are no longer online. */
    private fun cleanupDisconnectedOwners() {
        val online =
            IllyriaSouls
                .instance
                .server
                .onlinePlayers
                .map { it.uniqueId }
                .toSet()
        activeStains.entries.removeAll { (ownerId, stand) ->
            val remove = ownerId !in online
            if (remove) stand.remove()
            remove
        }
    }

    /** Removes stain markers found in an unloaded chunk if their owners are offline. */
    private fun removeLostStains(entities: List<Entity>) {
        val online =
            IllyriaSouls
                .instance
                .server
                .onlinePlayers
                .map { it.uniqueId }
                .toSet()
        entities.filterIsInstance<ArmorStand>().filter { isStain(it) }.forEach { stand ->
            val ownerId =
                stand.persistentDataContainer.get(STAIN_OWNER_KEY, PersistentDataType.STRING)?.let(UUID::fromString)
            if (ownerId != null && ownerId !in online) {
                activeStains.remove(ownerId)
                stand.remove()
            }
        }
    }
}
