@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.player

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.enchantments.spells.*
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.pdcs.ItemStackPDC.selectedSpell
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling spell casting within the system. */
internal object SpellMechanic : MechanicInterface {
    private val SPELL_MAP: Map<Enchantment, (PlayerInteractEvent) -> Unit> by lazy {
        mapOf(
            FrostbindEnchantment.get() to { FrostbindEnchantment.on(it) },
            InfernoEnchantment.get() to { InfernoEnchantment.on(it) },
            QuakeEnchantment.get() to { QuakeEnchantment.on(it) },
            SkysunderEnchantment.get() to { SkysunderEnchantment.on(it) },
            TempestEnchantment.get() to { TempestEnchantment.on(it) },
            VoidpullEnchantment.get() to { VoidpullEnchantment.on(it) },
            WitherbrandEnchantment.get() to { WitherbrandEnchantment.on(it) },
        )
    }

    /** Spell wand interaction message strings. */
    private object Messages {
        const val SELECTED_SPELL: String = "<spellbite>Current Spell > <white><spell></white></gradient>"
    }

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.BLAZE_ROD)
                .setName(MM.deserialize("<mango>Spell System</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Wand</yellow> <firewatch>></gradient> <white>Blaze rod with enchantments</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Cycle</yellow> <firewatch>></gradient> <white>Right-click to switch spells</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Cast</yellow> <firewatch>></gradient> " +
                            "<white>Left-click to cast selected spell</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.ADMIN

    @EventHandler
    fun on(event: PlayerInteractEvent) {
        handleWandInteract(event)
    }

    @EventHandler
    fun on(event: PlayerItemHeldEvent) {
        handleWandHeld(event)
    }

    /**
     * Handles player interactions with a spell wand.
     *
     * @param event The PlayerInteractEvent triggered by the player.
     */
    private fun handleWandInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return

        if (item.type != Material.BLAZE_ROD) return
        if (getSpellsOnWand(item).isEmpty()) return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                event.isCancelled = true
                cycleSpell(item)?.let { showSelectedSpell(event.player, it) }
            }

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                SPELL_MAP[getSelectedSpell(item) ?: return]?.invoke(event)
            }

            else -> {}
        }
    }

    /**
     * Handles spell wand display when the player changes held items.
     *
     * @param event The PlayerItemHeldEvent triggered by the player.
     */
    private fun handleWandHeld(event: PlayerItemHeldEvent) {
        val item = event.player.inventory.getItem(event.newSlot) ?: return

        if (item.type != Material.BLAZE_ROD) return
        if (getSpellsOnWand(item).isEmpty()) return

        getSelectedSpell(item)?.let { showSelectedSpell(event.player, getSpellName(it)) }
    }

    /** Gets the list of spells on a wand item. */
    private fun getSpellsOnWand(item: ItemStack): List<Enchantment> = item.enchantments.keys.filter { it in SPELL_MAP }

    /**
     * Gets the display name for a spell enchantment.
     * Derives the name from the enchantment key.
     */
    private fun getSpellName(spell: Enchantment): String =
        spell.key
            .toString()
            .substringAfterLast(':')
            .removeSuffix("_enchantment")
            .replaceFirstChar { it.uppercase() }

    /** Gets the spell key string for storage. */
    private fun getSpellKey(spell: Enchantment): String = spell.key.toString()

    /** Shows the selected spell name in the player's action bar. */
    private fun showSelectedSpell(
        player: Player,
        spellName: String,
    ) {
        player.sendActionBar(MM.deserialize(Messages.SELECTED_SPELL, Placeholder.unparsed("spell", spellName)))
    }

    /** Cycles to the next spell, updates the item's selected spell, and returns its name. */
    private fun cycleSpell(item: ItemStack): String? =
        getSpellsOnWand(item).takeIf { it.isNotEmpty() }?.let { spells ->
            val nextSpell = spells[(spells.indexOfFirst { getSpellKey(it) == item.selectedSpell } + 1) % spells.size]

            item.selectedSpell = getSpellKey(nextSpell)
            getSpellName(nextSpell)
        }

    /** Gets the currently selected spell for the item. */
    private fun getSelectedSpell(item: ItemStack): Enchantment? =
        getSpellsOnWand(item).takeIf { it.isNotEmpty() }?.let { spells ->
            spells.find { getSpellKey(it) == item.selectedSpell } ?: spells.first()
        }
}
