package org.xodium.illyriacore.items.flask

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/**
 * The player's Estus-like healing flask. Charges are stored on the item and in the player's PDC,
 * so the item itself is mainly a key to trigger the heal.
 */
internal object FlaskItem : ItemsInterface {
    /** Default starting flask charges for new players. */
    internal const val DEFAULT_CHARGES: Int = 5

    /** Internal PDC tag used to identify an actual flask item. */
    internal val FLASK_TAG_KEY = NamespacedKey(instance, "is_flask")

    /** PDC tag storing the flask's maximum charges on the item itself. */
    private val FLASK_MAX_CHARGES_TAG_KEY = NamespacedKey(instance, "flask_item_max_charges")

    /** PDC tag storing the flask's heal potency on the item itself. */
    private val FLASK_POTENCY_TAG_KEY = NamespacedKey(instance, "flask_item_potency")

    /** Base healing per charge at potency 0 (3 hearts). */
    private const val BASE_HEAL: Double = 6.0

    /** Additional healing per potency tier (1 heart). */
    private const val HEAL_PER_POTENCY: Double = 2.0

    override fun invoke(): ItemStack = withStats(DEFAULT_CHARGES, 0)

    /**
     * Creates a flask item with the specified maximum charges and potency tier.
     *
     * @param maxCharges The maximum charges the flask should hold.
     * @param potency The heal potency tier.
     * @return A fresh flask ItemStack.
     */
    fun withStats(
        maxCharges: Int,
        potency: Int,
    ): ItemStack =
        ItemStack.of(Material.HONEY_BOTTLE).apply {
            setData(DataComponentTypes.ITEM_NAME, MM.deserialize("<gold>Illyria Flask</gold>"))
            lore(
                buildList {
                    add(MM.deserialize("<gray>A blessed vessel of restorative warmth.</gray>"))
                    add(MM.deserialize("<gray>Drink to heal. Recharges at bonfires or on death.</gray>"))
                    add(MM.deserialize("<yellow>Right-click to drink a charge.</yellow>"))
                    add(MM.deserialize("<dark_aqua>Max charges: <gold>$maxCharges</gold></dark_aqua>"))
                    add(MM.deserialize("<dark_aqua>Healing: <red>${healAmount(potency).toInt()} HP</red></dark_aqua>"))
                },
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            editPersistentDataContainer {
                it.set(FLASK_TAG_KEY, PersistentDataType.BOOLEAN, true)
                it.set(FLASK_MAX_CHARGES_TAG_KEY, PersistentDataType.INTEGER, maxCharges.coerceAtLeast(1))
                it.set(FLASK_POTENCY_TAG_KEY, PersistentDataType.INTEGER, potency.coerceAtLeast(0))
            }
        }

    /** Checks if this item stack is a flask. */
    fun ItemStack.isFlask(): Boolean = persistentDataContainer.has(FLASK_TAG_KEY, PersistentDataType.BOOLEAN)

    /** Reads the max-charge value stored on this flask item, or the default. */
    fun ItemStack.flaskItemMaxCharges(): Int =
        persistentDataContainer.getOrDefault(FLASK_MAX_CHARGES_TAG_KEY, PersistentDataType.INTEGER, DEFAULT_CHARGES)

    /** Reads the potency value stored on this flask item, or 0. */
    fun ItemStack.flaskItemPotency(): Int =
        persistentDataContainer.getOrDefault(FLASK_POTENCY_TAG_KEY, PersistentDataType.INTEGER, 0)

    /** Calculates the total HP restored by one charge at the given potency tier. */
    fun healAmount(potency: Int): Double = BASE_HEAL + (potency * HEAL_PER_POTENCY)
}
