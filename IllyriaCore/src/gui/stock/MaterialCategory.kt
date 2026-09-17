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
                Tag.ITEMS_MEAT.isTagged(material) ||
                Tag.ITEMS_FISHES.isTagged(material) ||
                Tag.ITEMS_EGGS.isTagged(material) ||
                Tag.ITEMS_VILLAGER_PLANTABLE_SEEDS.isTagged(material) ||
                Tag.ITEMS_CHICKEN_FOOD.isTagged(material) ||
                Tag.ITEMS_COW_FOOD.isTagged(material) ||
                Tag.ITEMS_HORSE_FOOD.isTagged(material) ||
                Tag.ITEMS_PANDA_FOOD.isTagged(material) ||
                Tag.ITEMS_PARROT_FOOD.isTagged(material) ||
                Tag.ITEMS_PIG_FOOD.isTagged(material) ||
                Tag.ITEMS_RABBIT_FOOD.isTagged(material) ||
                Tag.ITEMS_SHEEP_FOOD.isTagged(material) ||
                Tag.ITEMS_VILLAGER_PICKS_UP.isTagged(material) ||
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
                Tag.ITEMS_SPEARS.isTagged(material) ||
                Tag.ITEMS_HEAD_ARMOR.isTagged(material) ||
                Tag.ITEMS_CHEST_ARMOR.isTagged(material) ||
                Tag.ITEMS_LEG_ARMOR.isTagged(material) ||
                Tag.ITEMS_FOOT_ARMOR.isTagged(material) ||
                Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(material) ||
                Tag.ITEMS_ENCHANTABLE_WEAPON.isTagged(material) ||
                Tag.ITEMS_ARROWS.isTagged(material) ||
                material == Material.SHIELD ||
                material == Material.FISHING_ROD ||
                material == Material.SHEARS ||
                material == Material.FLINT_AND_STEEL ||
                material == Material.SPYGLASS ||
                material == Material.BRUSH

        private fun isRedstoneOrUtility(material: Material): Boolean =
            Tag.ITEMS_RAILS.isTagged(material) ||
                Tag.ITEMS_DOORS.isTagged(material) ||
                Tag.ITEMS_TRAPDOORS.isTagged(material) ||
                Tag.ITEMS_FENCE_GATES.isTagged(material) ||
                Tag.ITEMS_BUTTONS.isTagged(material) ||
                Tag.ITEMS_WOODEN_PRESSURE_PLATES.isTagged(material) ||
                Tag.ITEMS_SIGNS.isTagged(material) ||
                Tag.ITEMS_HANGING_SIGNS.isTagged(material) ||
                Tag.ITEMS_BOATS.isTagged(material) ||
                Tag.ITEMS_CHEST_BOATS.isTagged(material) ||
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
                material.name.endsWith("_PRESSURE_PLATE")

        private fun isMaterial(material: Material): Boolean =
            Tag.ITEMS_COALS.isTagged(material) ||
                Tag.ITEMS_DYES.isTagged(material) ||
                Tag.ITEMS_BEACON_PAYMENT_ITEMS.isTagged(material) ||
                Tag.ITEMS_TRIM_MATERIALS.isTagged(material) ||
                material.name.endsWith("_INGOT") ||
                material.name.endsWith("_NUGGET") ||
                material.name.endsWith("_GEM") ||
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
                material == Material.EXPERIENCE_BOTTLE ||
                material == Material.NETHERITE_SCRAP ||
                material == Material.CLAY_BALL ||
                material == Material.BRICK ||
                material == Material.NETHER_BRICK ||
                material == Material.FLINT ||
                material == Material.ECHO_SHARD ||
                material == Material.DISC_FRAGMENT_5
    }
}
