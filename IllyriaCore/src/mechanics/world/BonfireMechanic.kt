package org.xodium.illyriacore.mechanics.world

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.flask.FlaskItem
import org.xodium.illyriacore.items.flask.FlaskItem.flaskItemMaxCharges
import org.xodium.illyriacore.items.flask.FlaskItem.flaskItemPotency
import org.xodium.illyriacore.items.flask.FlaskItem.isFlask
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.flaskCharges
import org.xodium.illyriacore.pdcs.PlayerPDC.flaskMaxCharges
import org.xodium.illyriacore.pdcs.PlayerPDC.flaskPotency
import org.xodium.illyriacore.pdcs.PlayerPDC.refillFlask
import org.xodium.illyrialib.Utils.MM
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.measureTime
import org.bukkit.block.data.type.Campfire as CampfireData

/**
 * Implements bonfire rest mechanics and flask usage. Lit campfires act as bonfires: right-clicking
 * one sets respawn, restores health, refills flask charges, and respawns hostile mobs nearby.
 * The Illyria Flask heals the player when used and recharges on rest or death.
 */
internal object BonfireMechanic : MechanicInterface {
    /** Radius around the bonfire where mobs are respawned/reset on rest. */
    private const val MOB_RESPAWN_RADIUS: Double = 32.0

    /** Radius around the bonfire where existing hostile mobs are removed on rest. */
    private const val MOB_CLEAR_RADIUS: Double = 48.0

    /** Brief regeneration buff applied after resting at a bonfire. */
    private const val REST_REGEN_SECONDS: Int = 3

    /** PDC key marking a campfire block as a registered bonfire for quick lookup. */
    private val BONFIRE_KEY = NamespacedKey(instance, "bonfire_location")

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) = handleInteract(event)

    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: PlayerDeathEvent) {
        event.entity.refillFlask()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerRespawnEvent) {
        event.player.refillFlask()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: PrepareItemCraftEvent) = applyFlaskUpgrade(event)

    override fun register(): Long =
        measureTime {
            instance.server.pluginManager.registerEvents(this, instance)
        }.inWholeMilliseconds

    /** Handles player right-click interactions for bonfire rest and flask drinking. */
    private fun handleInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock
        val heldFlask =
            player.inventory.itemInMainHand.takeIf { it.isFlask() }
                ?: player.inventory.itemInOffHand.takeIf { it.isFlask() }

        if (heldFlask != null &&
            (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK)
        ) {
            drinkFlask(player, heldFlask)
            event.isCancelled = true
            return
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK || block == null) return
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return
        val campfire = block.blockData as? CampfireData ?: return
        if (!campfire.isLit) return

        event.isCancelled = true
        restAtBonfire(player, block)
    }

    /** Applies flask upgrade stats to the crafting result for upgrade recipes. */
    private fun applyFlaskUpgrade(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        val key = (event.recipe as? org.bukkit.Keyed)?.key?.key ?: return
        val inputFlask = inventory.matrix.firstOrNull { it?.isFlask() == true } ?: return

        val newMaxCharges = inputFlask.flaskItemMaxCharges()
        val newPotency = inputFlask.flaskItemPotency()

        val (maxCharges, potency) =
            when (key) {
                "illyria_flask_charge_upgrade_shaped_recipe" -> (newMaxCharges + 1) to newPotency
                "illyria_flask_potency_upgrade_shaped_recipe" -> newMaxCharges to (newPotency + 1)
                else -> return
            }

        inventory.result = FlaskItem.withStats(maxCharges, potency)
    }

    /** Rests at a bonfire: heal, refill flask, set spawn, and reset nearby mobs. */
    private fun restAtBonfire(
        player: Player,
        block: Block,
    ) {
        val location = block.location.add(0.5, 0.0, 0.5)

        player.setRespawnLocation(location, true)
        player.health = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: player.health
        player.foodLevel = 20
        player.saturation = 5f
        player.refillFlask()
        player.addPotionEffect(
            PotionEffect(PotionEffectType.REGENERATION, REST_REGEN_SECONDS * 20, 2, false, true, true),
        )

        markBonfire(block)
        resetMobs(player.world, location)

        player.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 0.8f)
        player.sendMessage(MM.deserialize("<gold>Resting at the bonfire restored your strength.</gold>"))
    }

    /** Drinks one flask charge, healing the player and syncing item stats to player PDC. */
    private fun drinkFlask(
        player: Player,
        flask: org.bukkit.inventory.ItemStack,
    ) {
        val itemMaxCharges = flask.flaskItemMaxCharges()
        val itemPotency = flask.flaskItemPotency()

        if (itemMaxCharges > player.flaskMaxCharges) player.flaskMaxCharges = itemMaxCharges
        if (itemPotency > player.flaskPotency) player.flaskPotency = itemPotency
        player.flaskCharges = player.flaskCharges.coerceAtMost(player.flaskMaxCharges)

        if (player.flaskCharges <= 0) {
            player.sendMessage(MM.deserialize("<gray>Your flask is empty.</gray>"))
            return
        }

        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        val heal = FlaskItem.healAmount(player.flaskPotency)
        player.health = (player.health + heal).coerceAtMost(maxHealth)
        player.flaskCharges -= 1
        player.playSound(player.location, Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1f, 1f)
        player.sendMessage(
            MM.deserialize(
                "<gold>Drank from the flask.</gold> " +
                    "<gray>${player.flaskCharges}/${player.flaskMaxCharges} charges remain.</gray>",
            ),
        )
    }

    /** Marks a campfire as a registered bonfire in its persistent data. */
    private fun markBonfire(block: Block) {
        val campfire = block.state as? org.bukkit.block.Campfire ?: return
        if (campfire.persistentDataContainer.has(BONFIRE_KEY, PersistentDataType.STRING)) return
        campfire.persistentDataContainer.set(
            BONFIRE_KEY,
            PersistentDataType.STRING,
            block.location.run { "${world.uid};$x;$y;$z" },
        )
        campfire.update()
    }

    /** Removes existing hostile mobs and spawns a fresh wave around the bonfire. */
    private fun resetMobs(
        world: World,
        center: Location,
    ) {
        val clearRadiusSquared = MOB_CLEAR_RADIUS * MOB_CLEAR_RADIUS
        world
            .getNearbyEntities(center, MOB_CLEAR_RADIUS, MOB_CLEAR_RADIUS, MOB_CLEAR_RADIUS)
            .filterIsInstance<Mob>()
            .filter { it.location.distanceSquared(center) <= clearRadiusSquared }
            .forEach { it.remove() }

        if (world.time in 12541..23458) return

        val spawnRadiusSquared = MOB_RESPAWN_RADIUS * MOB_RESPAWN_RADIUS
        val spawnAttempts = 12
        val candidates =
            listOf(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.SPIDER,
                EntityType.CREEPER,
            )

        repeat(spawnAttempts) {
            val angle = Math.random() * 2 * Math.PI
            val distance = MOB_RESPAWN_RADIUS * sqrt(Math.random())
            val x = center.x + distance * cos(angle)
            val z = center.z + distance * sin(angle)
            val spawnLoc = world.getHighestBlockAt(x.toInt(), z.toInt()).location.add(0.5, 1.0, 0.5)

            if (spawnLoc.distanceSquared(center) > spawnRadiusSquared) return@repeat
            if (!spawnLoc
                    .block
                    .getRelative(0, -1, 0)
                    .type
                    .isSolid
            ) {
                return@repeat
            }

            val type = candidates.random()
            world.spawn(spawnLoc, Mob::class.java) { mob ->
                mob.setRotation((Math.random() * 360).toFloat(), 0f)
            }
        }
    }
}
