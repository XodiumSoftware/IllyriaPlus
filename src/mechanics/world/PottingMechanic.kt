package org.xodium.illyriaplus.mechanics.world

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling potted plant rotation within the world. */
internal object PottingMechanic : MechanicInterface {
    /**
     * The [NamespacedKey] used to mark [BlockDisplay] entities as potted plants.
     * Entities with this key are eligible for rotation.
     */
    internal val POT_PLANT_KEY: NamespacedKey = NamespacedKey(instance, "pot_plant")

    private const val ROTATION_DEGREES = 15.0f
    private const val ROTATION_OFFHAND_DEGREES = 7.5f
    private const val SEARCH_RADIUS = 0.9
    private const val SOUND_VOLUME = 1.0f
    private const val SOUND_PITCH = 1.5f

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) = rotatePlant(event)

    /**
     * Handles rotating a potted plant display entity when a player sneak-right-clicks
     * a pot block while holding a stick. Rotation is 15° per click, or 7.5° when a
     * stick is also held in the off-hand.
     *
     * @param event The [PlayerInteractEvent] triggered when a player interacts with the world.
     */
    private fun rotatePlant(event: PlayerInteractEvent) {
        if (!event.action.isRightClick || event.clickedBlock == null) return
        val player = event.player
        if (!player.isSneaking) return
        if (player.inventory.itemInMainHand.type != Material.STICK) return
        val block = event.clickedBlock ?: return
        if (!isPot(block.type)) return

        val center = block.location.toCenterLocation()
        val plants =
            center
                .world
                .getNearbyEntities(center, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
                .filterIsInstance<BlockDisplay>()
                .filter { it.isPotPlant() }
        if (plants.isEmpty()) return

        val degrees =
            if (player.inventory.itemInOffHand.type == Material.STICK) {
                ROTATION_OFFHAND_DEGREES
            } else {
                ROTATION_DEGREES
            }
        plants.forEach { it.teleport(it.location.apply { yaw += degrees }) }

        center.world.playSound(
            center,
            Sound.BLOCK_AZALEA_LEAVES_PLACE,
            SOUND_VOLUME,
            SOUND_PITCH,
        )
        event.isCancelled = true
    }

    /**
     * Checks whether this entity is marked as a potted plant.
     *
     * @return `true` if the entity has the pot plant key in its persistent data.
     */
    private fun Entity.isPotPlant(): Boolean = persistentDataContainer.has(POT_PLANT_KEY, PersistentDataType.BOOLEAN)

    /**
     * Checks whether the given material is considered a pot block for the purposes of this mechanic.
     *
     * @param material The [Material] to check.
     * @return `true` if the material is a pot block.
     */
    private fun isPot(material: Material): Boolean =
        material == Material.FLOWER_POT || material == Material.DECORATED_POT
}
