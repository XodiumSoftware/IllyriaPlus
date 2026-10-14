package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.pdcs.PlayerPDC.MAX_BENDING_LEVEL
import org.xodium.illyriacore.pdcs.PlayerPDC.MIN_BENDING_LEVEL

/**
 * Owns the `bending` advancement tab and its tree of advancements. The tab is built once
 * via [AdvancementBuilder] and committed during plugin enable.
 */
internal object BendingAdvancement {
    /** Per-element, per-level advancement storage: `elementAdvancements[type][level - 1]`. */
    private val elementAdvancements =
        mutableMapOf<BendingTypeEnum, MutableList<BaseAdvancement>>()

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
                    BendingTypeEnum.entries.forEachIndexed { column, type ->
                        val label = type.name.lowercase().replaceFirstChar(Char::uppercase)
                        val levels = elementAdvancements.getOrPut(type) { mutableListOf() }
                        (MIN_BENDING_LEVEL..MAX_BENDING_LEVEL).forEach { level ->
                            levels +=
                                advancement("${type.name.lowercase()}_$level") {
                                    icon(type.material)
                                    title("$label Bender ${level.toRoman()}")
                                    description("Walk the path of $label — Level ${level.toRoman()}")
                                    at(level.toFloat(), column + 1f)
                                }
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
     * Grants the level-1 element advancement and adds one of that element's focus material to
     * the player's inventory. No-ops if the advancement hasn't been registered yet.
     *
     * @param player The player to grant the advancement to.
     * @param type The [BendingTypeEnum] the player committed to.
     */
    fun grantElement(
        player: Player,
        type: BendingTypeEnum,
    ) {
        grantLevels(player, type, MIN_BENDING_LEVEL)
        player.inventory.addItem(ItemStack.of(type.material))
    }

    /**
     * Grants every element advancement for the given type from level 1 through [level], in
     * order. No-ops for any advancement that hasn't been registered yet.
     *
     * @param player The player to grant advancements to.
     * @param type The [BendingTypeEnum] to grant levels for.
     * @param level The highest level to grant (inclusive).
     */
    fun grantLevels(
        player: Player,
        type: BendingTypeEnum,
        level: Int,
    ) {
        val levels = elementAdvancements[type] ?: return
        (MIN_BENDING_LEVEL..level.coerceIn(MIN_BENDING_LEVEL, MAX_BENDING_LEVEL)).forEach {
            levels.getOrNull(it - 1)?.grant(player)
        }
    }

    /**
     * Revokes all element advancements from the player. Used when the player's bending type
     * is reset so they can re-earn the advancement on their next pick. No-ops if no
     * advancements have been registered yet.
     *
     * @param player The player to revoke the advancements from.
     */
    fun revokeElements(player: Player) {
        elementAdvancements.values.forEach { levels -> levels.forEach { it.revoke(player) } }
    }

    /** Converts an integer in `1..5` to its roman numeral representation. */
    private fun Int.toRoman(): String =
        when (this) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> toString()
        }
}
