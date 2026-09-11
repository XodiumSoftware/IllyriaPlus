package org.xodium.illyriaplus.mechanics.world

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.data.PlantData
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Represents a mechanic handling the planting and rotation of potted plants within the world. */
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
    private const val PLANT_SOUND_VOLUME = 1.0f
    private const val PLANT_SOUND_PITCH = 1.0f

    /** Extra Y offset applied when planting in a decorated pot, which is taller than a flower pot. */
    private const val DECORATED_POT_Y_OFFSET = 0.6

    /** Rotation quaternion flipping a plant upside down (π radians around the X axis). */
    private val UPSIDE_DOWN_ROTATION = Quaternionf().rotationX(Math.PI.toFloat())

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEvent) {
        rotatePlant(event)
        plantPot(event)
    }

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
        val plants = findPlants(center)
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
     * Handles planting a supported plant into a pot when a player sneak-right-clicks a pot
     * while holding a plant in their main hand. Spawns a [BlockDisplay] with the plant's
     * block state, scaled and offset to fit naturally inside the pot, and marks it with
     * [POT_PLANT_KEY] so it can be rotated with a stick. One item is consumed unless the
     * player is in creative mode.
     *
     * @param event The [PlayerInteractEvent] triggered when a player interacts with the world.
     */
    private fun plantPot(event: PlayerInteractEvent) {
        if (!event.action.isRightClick || event.clickedBlock == null) return
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (!player.isSneaking) return
        val item = player.inventory.itemInMainHand
        if (item.type == Material.STICK) return
        val plant = PlantData.PLANTS[item.type] ?: return
        val block = event.clickedBlock ?: return
        if (!isPot(block.type)) return

        val center = block.location.toCenterLocation()
        if (findPlants(center).isNotEmpty()) return

        event.isCancelled = true

        val location =
            Location(
                block.world,
                block.x + 0.5,
                block.y + if (block.type == Material.DECORATED_POT) DECORATED_POT_Y_OFFSET else 0.0,
                block.z + 0.5,
            )
        block.world.spawn(location, BlockDisplay::class.java) {
            it.block = plant.blockData ?: item.type.createBlockData()
            it.transformation =
                Transformation(
                    Vector3f(plant.translation),
                    if (plant.upsideDown) UPSIDE_DOWN_ROTATION else Quaternionf(),
                    Vector3f(plant.scale),
                    Quaternionf(),
                )
            it.persistentDataContainer.set(POT_PLANT_KEY, PersistentDataType.BOOLEAN, true)
        }

        if (player.gameMode != GameMode.CREATIVE) item.subtract()

        center.world.playSound(
            center,
            Sound.ITEM_CROP_PLANT,
            PLANT_SOUND_VOLUME,
            PLANT_SOUND_PITCH,
        )
    }

    /**
     * Finds all potted plant display entities near the given location.
     *
     * @param center The [Location] to search around.
     * @return A [List] of [BlockDisplay] entities marked as potted plants.
     */
    private fun findPlants(center: Location): List<BlockDisplay> =
        center
            .world
            .getNearbyEntities(center, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .filterIsInstance<BlockDisplay>()
            .filter { it.isPotPlant() }

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
