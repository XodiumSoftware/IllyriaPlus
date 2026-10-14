package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.Utils.toRoman
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.pdcs.PlayerPDC.MAX_BENDING_LEVEL
import org.xodium.illyriacore.pdcs.PlayerPDC.MIN_BENDING_LEVEL

/**
 * Owns the `bending` advancement tab and its tree of advancements. The tab is built once
 * via [AdvancementBuilder] and committed during plugin enable. Each element is a chained
 * row: `{element}_1` under the tab root, levels `2..5` nested under their previous level.
 * At level [SUB_BRANCH_LEVEL], each element also branches off its [BendingTypeEnum.subSkill]
 * chain (Vacuum for AIR, Metal for EARTH, Lightning for FIRE, Blood for WATER).
 */
internal object BendingAdvancement {
    /** The bending level at which a sub-skill chain diverges from its parent element. */
    private const val SUB_BRANCH_LEVEL = 3

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
                    BendingTypeEnum.entries.forEach { type ->
                        val y = rowOf(type)
                        advancement(key(type.name.lowercase(), MIN_BENDING_LEVEL)) {
                            display(type, MIN_BENDING_LEVEL)
                            at(MIN_BENDING_LEVEL.toFloat(), y)
                            var scope = this
                            (MIN_BENDING_LEVEL + 1..MAX_BENDING_LEVEL).forEach { level ->
                                scope =
                                    scope.nested(key(type.name.lowercase(), level)) {
                                        display(type, level)
                                        at(level.toFloat(), y)
                                        if (level == SUB_BRANCH_LEVEL) addSubBranchFor(type)
                                    }
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
        val registered = tab ?: return
        (MIN_BENDING_LEVEL..level.coerceIn(MIN_BENDING_LEVEL, MAX_BENDING_LEVEL)).forEach {
            registered[key(type.name.lowercase(), it)]?.grant(player)
        }
    }

    /**
     * Revokes all element advancements (including their sub-branch chains: Vacuum for AIR,
     * Metal for EARTH, Lightning for FIRE, Blood for WATER) from the player. Used when the
     * player's bending type is reset so they can re-earn the advancement on their next pick.
     * No-ops if the tab hasn't been registered yet.
     *
     * @param player The player to revoke the advancements from.
     */
    fun revokeElements(player: Player) {
        val registered = tab ?: return
        BendingTypeEnum.entries.forEach { type ->
            (MIN_BENDING_LEVEL..MAX_BENDING_LEVEL).forEach { level ->
                registered[key(type.name.lowercase(), level)]?.revoke(player)
            }
            (MIN_BENDING_LEVEL..MAX_BENDING_LEVEL).forEach { level ->
                registered[key(type.subSkill.keyPrefix, level)]?.revoke(player)
            }
        }
    }

    /** The registry key for an element at a given level, e.g. `fire_3` or `lightning_1`. */
    private fun key(
        prefix: String,
        level: Int,
    ): String = "${prefix}_$level"

    /**
     * Attaches the sub-skill chain declared by [BendingTypeEnum.subSkill] to the current
     * advancement. The first level is parented to this scope (which should be the element's
     * level-[SUB_BRANCH_LEVEL] advancement) and subsequent levels chain below it. Each
     * sub-branch occupies its own row beneath the main element rows.
     *
     * TODO: Wire sub-branch unlocks into actual progression once designs are settled.
     */
    private fun AdvancementBuilder.AdvancementScope.addSubBranchFor(parentType: BendingTypeEnum) {
        val subSkill = parentType.subSkill
        val subY = subRowOf(parentType)
        var chain =
            nested(key(subSkill.keyPrefix, MIN_BENDING_LEVEL)) {
                display(subSkill.label, subSkill.icon, MIN_BENDING_LEVEL)
                at(SUB_BRANCH_LEVEL.toFloat(), subY)
            }
        (MIN_BENDING_LEVEL + 1..MAX_BENDING_LEVEL).forEach { subLevel ->
            chain =
                chain.nested(key(subSkill.keyPrefix, subLevel)) {
                    display(subSkill.label, subSkill.icon, subLevel)
                    at((SUB_BRANCH_LEVEL + subLevel - 1).toFloat(), subY)
                }
        }
    }

    /** The row (y-position) on the advancement tab for a given element's main chain. */
    private fun rowOf(type: BendingTypeEnum): Float = (BendingTypeEnum.entries.indexOf(type) * 2 + 1).toFloat()

    /** The row (y-position) for a given element's sub-branch: sits directly below its parent. */
    private fun subRowOf(type: BendingTypeEnum): Float = rowOf(type) + 1f

    /** Applies the common display configuration for an element at a given level. */
    private fun AdvancementBuilder.AdvancementScope.display(
        type: BendingTypeEnum,
        level: Int,
    ) {
        val label = type.name.lowercase().replaceFirstChar(Char::uppercase)
        display(label, type.material, level)
    }

    /** Applies the display configuration using an explicit label/icon material. */
    private fun AdvancementBuilder.AdvancementScope.display(
        label: String,
        iconMaterial: Material,
        level: Int,
    ) {
        icon(iconMaterial)
        title("$label Bender ${level.toRoman()}")
        description("Walk the path of $label — Level ${level.toRoman()}")
        when (level) {
            3 -> frame(AdvancementFrameType.GOAL)
            MAX_BENDING_LEVEL -> frame(AdvancementFrameType.CHALLENGE)
        }
    }
}
