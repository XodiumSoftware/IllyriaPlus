package org.xodium.illyriacore.gui.stock

import org.bukkit.Material
import org.bukkit.Tag

/**
 * A coarse, Minecraft-flavored bucket for grouping stocked items in the wandering trader GUI.
 * Classification derives from [Material] properties and vanilla [Tag]s, with [MISC] as the
 * fallback for anything unclassified.
 *
 * @property displayName The name shown on the category icon in the picker.
 * @property icon The item material representing this category in the picker.
 */
internal enum class MaterialCategory(
    val displayName: String,
    val icon: Material,
) {
    BUILDING_BLOCKS("Building Blocks", Material.BRICKS),
    TOOLS_AND_WEAPONS("Tools & Weapons", Material.IRON_PICKAXE),
    FOOD_AND_FARMING("Food & Farming", Material.BREAD),
    MATERIALS("Materials", Material.IRON_INGOT),
    REDSTONE_AND_UTILITY("Redstone & Utility", Material.REDSTONE),
    MISC("Misc", Material.CHEST),
    ;

    companion object {
        /** Returns the [MaterialCategory] [material] belongs to. */
        fun of(material: Material): MaterialCategory =
            when {
                isFoodOrFarming(material) -> FOOD_AND_FARMING
                isToolOrWeapon(material) -> TOOLS_AND_WEAPONS
                isRedstoneOrUtility(material) -> REDSTONE_AND_UTILITY
                isMaterial(material) -> MATERIALS
                material.isBlock -> BUILDING_BLOCKS
                else -> MISC
            }

        private fun isFoodOrFarming(material: Material): Boolean =
            material.isEdible ||
                material == Material.WHEAT ||
                material == Material.WHEAT_SEEDS ||
                material == Material.BEETROOT_SEEDS ||
                material == Material.CARROT ||
                material == Material.POTATO ||
                material == Material.PUMPKIN_SEEDS ||
                material == Material.MELON_SEEDS ||
                material == Material.TORCHFLOWER_SEEDS ||
                material == Material.PITCHER_POD ||
                material == Material.SUGAR_CANE ||
                material == Material.COCOA_BEANS ||
                material == Material.NETHER_WART ||
                material == Material.BONE_MEAL

        private fun isToolOrWeapon(material: Material): Boolean =
            Tag.ITEMS_SWORDS.isTagged(material) ||
                Tag.ITEMS_AXES.isTagged(material) ||
                Tag.ITEMS_PICKAXES.isTagged(material) ||
                Tag.ITEMS_SHOVELS.isTagged(material) ||
                Tag.ITEMS_HOES.isTagged(material) ||
                Tag.ITEMS_HEAD_ARMOR.isTagged(material) ||
                Tag.ITEMS_CHEST_ARMOR.isTagged(material) ||
                Tag.ITEMS_LEG_ARMOR.isTagged(material) ||
                Tag.ITEMS_FOOT_ARMOR.isTagged(material) ||
                material == Material.BOW ||
                material == Material.CROSSBOW ||
                material == Material.TRIDENT ||
                material == Material.MACE ||
                material == Material.SHIELD ||
                material == Material.FISHING_ROD ||
                material == Material.SHEARS ||
                material == Material.FLINT_AND_STEEL ||
                material == Material.SPYGLASS ||
                material == Material.BRUSH

        private fun isRedstoneOrUtility(material: Material): Boolean =
            material == Material.REDSTONE ||
                material == Material.REDSTONE_TORCH ||
                material == Material.REDSTONE_BLOCK ||
                material == Material.REPEATER ||
                material == Material.COMPARATOR ||
                material == Material.PISTON ||
                material == Material.STICKY_PISTON ||
                material == Material.OBSERVER ||
                material == Material.DISPENSER ||
                material == Material.DROPPER ||
                material == Material.HOPPER ||
                material == Material.LEVER ||
                material == Material.TRIPWIRE_HOOK ||
                material == Material.DAYLIGHT_DETECTOR ||
                material == Material.TARGET ||
                material == Material.SCULK_SENSOR ||
                material == Material.CALIBRATED_SCULK_SENSOR ||
                material == Material.RAIL ||
                material == Material.POWERED_RAIL ||
                material == Material.DETECTOR_RAIL ||
                material == Material.ACTIVATOR_RAIL ||
                material == Material.MINECART ||
                material == Material.CHEST_MINECART ||
                material == Material.FURNACE_MINECART ||
                material == Material.HOPPER_MINECART ||
                material == Material.TNT_MINECART ||
                material == Material.BUCKET ||
                material == Material.WATER_BUCKET ||
                material == Material.LAVA_BUCKET ||
                material == Material.POWDER_SNOW_BUCKET ||
                material == Material.MILK_BUCKET ||
                material.name.endsWith("_PRESSURE_PLATE") ||
                material.name.endsWith("_BUTTON")

        private fun isMaterial(material: Material): Boolean =
            Tag.ITEMS_COALS.isTagged(material) ||
                material.name.endsWith("_INGOT") ||
                material.name.endsWith("_NUGGET") ||
                material.name.endsWith("_GEM") ||
                material.name.endsWith("_DYE") ||
                material == Material.DIAMOND ||
                material == Material.EMERALD ||
                material == Material.LAPIS_LAZULI ||
                material == Material.QUARTZ ||
                material == Material.AMETHYST_SHARD ||
                material == Material.ECHO_SHARD ||
                material == Material.DISC_FRAGMENT_5 ||
                material == Material.NETHERITE_SCRAP ||
                material == Material.CLAY_BALL ||
                material == Material.BRICK ||
                material == Material.NETHER_BRICK ||
                material == Material.FLINT ||
                material == Material.RAW_IRON ||
                material == Material.RAW_GOLD ||
                material == Material.RAW_COPPER ||
                material == Material.GLOWSTONE_DUST ||
                material == Material.BLAZE_POWDER ||
                material == Material.BLAZE_ROD ||
                material == Material.GHAST_TEAR ||
                material == Material.MAGMA_CREAM ||
                material == Material.SLIME_BALL ||
                material == Material.HONEYCOMB ||
                material == Material.PRISMARINE_SHARD ||
                material == Material.PRISMARINE_CRYSTALS ||
                material == Material.NAUTILUS_SHELL ||
                material == Material.HEART_OF_THE_SEA ||
                material == Material.SHULKER_SHELL ||
                material == Material.PHANTOM_MEMBRANE ||
                material == Material.LEATHER ||
                material == Material.RABBIT_HIDE ||
                material == Material.STRING ||
                material == Material.FEATHER ||
                material == Material.GUNPOWDER ||
                material == Material.INK_SAC ||
                material == Material.GLOW_INK_SAC ||
                material == Material.PAPER ||
                material == Material.BOOK ||
                material == Material.EXPERIENCE_BOTTLE
    }
}
