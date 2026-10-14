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

    /** Per-element advancement granted when the player commits to a bending type. */
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
                .icon(Material.NETHER_STAR)
                .title("Bending")
                .description("Choose Your Path [/bending]")
                .background("textures/block/blue_ice.png")
                .root {
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
     * Grants the tab's root advancement. No-ops if the tab hasn't been registered yet.
     *
     * @param player The player to grant the advancement to.
     */
    fun grantRoot(player: Player) {
        tab?.rootAdvancement?.grant(player)
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

    /**
     * Revokes all element advancements from the player. Used when the player's bending type
     * is reset so they can re-earn the advancement on their next pick. No-ops if no
     * advancements have been registered yet.
     *
     * @param player The player to revoke the advancements from.
     */
    fun revokeElements(player: Player) {
        elementAdvancements.values.forEach { it.revoke(player) }
    }
}
