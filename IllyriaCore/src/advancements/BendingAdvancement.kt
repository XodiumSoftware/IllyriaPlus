package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement
import org.bukkit.Material
import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.BendingTypeEnum

/**
 * Owns the `bending` advancement tab and its tree of advancements. The tab is built once
 * via [AdvancementBuilder] and committed during plugin enable.
 */
internal object BendingAdvancement {
    private const val ADVANCEMENT_KEY_PREFIX = "_1"

    /** Advancement granted the first time a player commits to a bending type. */
    private var choosePath: BaseAdvancement? = null

    /** Per-element advancement granted alongside [choosePath] matching the player's pick. */
    private val elementAdvancements = mutableMapOf<BendingTypeEnum, BaseAdvancement>()

    /** The registered tab handle, available after [register] has been called. */
    var tab: AdvancementBuilder.RegisteredTab? = null
        private set

    /**
     * Builds and registers the `bending` tab. No-ops if called more than once.
     */
    fun register() {
        if (tab != null) return

        tab =
            AdvancementBuilder
                .tab("bending")
                .icon(Material.WIND_CHARGE)
                .title("Bending")
                .description("Master the elements")
                .background("textures/block/blue_ice.png")
                .root {
                    choosePath =
                        advancement("choose_path") {
                            icon(Material.NETHER_STAR)
                            title("Choose Your Path")
                            description("Commit to a bending type (/bending)")
                            at(1f, 0f)
                        }

                    BendingTypeEnum.entries.forEachIndexed { index, type ->
                        val label = type.name.lowercase().replaceFirstChar(Char::uppercase)
                        elementAdvancements[type] =
                            advancement(type.name.lowercase() + ADVANCEMENT_KEY_PREFIX) {
                                icon(type.material)
                                title("$label Bender")
                                description("Walk the path of $label")
                                at(1f, index + 1f)
                            }
                    }
                }.register()
    }

    /**
     * Grants the "Choose Your Path" advancement to the given player. No-ops if the
     * advancement hasn't been registered yet.
     *
     * @param player The player to grant the advancement to.
     */
    fun grantChoosePath(player: Player) {
        choosePath?.grant(player)
    }

    /**
     * Grants the element-specific advancement for the given type. No-ops if the advancement
     * hasn't been registered yet.
     *
     * @param player The player to grant the advancement to.
     * @param type The [BendingTypeEnum] the player committed to.
     */
    fun grantElement(
        player: Player,
        type: BendingTypeEnum,
    ) {
        elementAdvancements[type]?.grant(player)
    }
}
