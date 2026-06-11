package org.xodium.illyriaplus.mechanics.player

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.enchantments.spells.SpellEnchantmentInterface
import org.xodium.illyriaplus.managers.CooldownManager
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.pdcs.ItemStackPDC.selectedSpell

/** Represents a mechanic handling spell casting within the system. */
internal object SpellMechanic : MechanicInterface {
    private val SPELL_MAP: Map<Enchantment, SpellEnchantmentInterface> by lazy {
        instance.enchantments.spells.associateBy { it.get() }
    }

    /** Spell wand interaction message strings. */
    private object Messages {
        const val SELECTED_SPELL: String = "<spellbite>Current Spell > <white><spell></white></gradient>"
    }

    @EventHandler
    fun on(event: PlayerInteractEvent) = handleWandInteract(event)

    @EventHandler
    fun on(event: PlayerItemHeldEvent) = handleWandHeld(event)

    @EventHandler
    fun on(event: PlayerSwapHandItemsEvent) {
        val player = event.player

        if (CooldownManager.isCasting(player)) CooldownManager.interruptCast(player)
    }

    @EventHandler
    fun on(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        if (CooldownManager.isCasting(player)) CooldownManager.interruptCast(player)
    }

    /**
     * Handles player interactions with a spell wand.
     *
     * @param event The PlayerInteractEvent triggered by the player.
     */
    private fun handleWandInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (item.type != Material.BLAZE_ROD) return
        if (getSpellsOnWand(item).isEmpty()) return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                event.isCancelled = true
                cycleSpell(item)?.let { showSelectedSpell(player, it) }
            }

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                val spell = getSelectedSpell(item) ?: return

                if (player.gameMode != GameMode.CREATIVE && CooldownManager.isOnCooldown(player, spell)) {
                    event.isCancelled = true
                    CooldownManager.notifyCooldown(player, spell)
                    return
                }
                if (CooldownManager.isCasting(player)) {
                    event.isCancelled = true
                    return
                }

                if (player.gameMode != GameMode.CREATIVE && spell.castDelay > 0) {
                    event.isCancelled = true
                    CooldownManager.startCast(player, spell.castDelay) {
                        if (!player.isOnline) return@startCast

                        val currentItem = player.inventory.itemInMainHand

                        if (currentItem.type != Material.BLAZE_ROD) return@startCast

                        val currentSpell = getSelectedSpell(currentItem) ?: return@startCast

                        if (currentSpell != spell) return@startCast

                        spell.cast(event)

                        if (player.gameMode != GameMode.CREATIVE) CooldownManager.startCooldowns(player, spell)
                    }
                } else {
                    event.isCancelled = true
                    spell.cast(event)

                    if (player.gameMode != GameMode.CREATIVE) CooldownManager.startCooldowns(player, spell)
                }
            }

            else -> {}
        }
    }

    /**
     * Handles spell wand display when the player changes held items,
     * and interrupts active casts on item swap.
     *
     * @param event The PlayerItemHeldEvent triggered by the player.
     */
    private fun handleWandHeld(event: PlayerItemHeldEvent) {
        val player = event.player

        if (CooldownManager.isCasting(player)) {
            CooldownManager.interruptCast(player)
            return
        }

        val item = player.inventory.getItem(event.newSlot) ?: return

        if (item.type != Material.BLAZE_ROD) return
        if (getSpellsOnWand(item).isEmpty()) return

        getSelectedSpell(item)?.let { showSelectedSpell(player, getSpellName(it)) }
    }

    /** Gets the list of spells on a wand item. */
    private fun getSpellsOnWand(item: ItemStack): List<Enchantment> = item.enchantments.keys.filter { it in SPELL_MAP }

    /**
     * Gets the display name for a spell enchantment.
     * Derives the name from the enchantment key.
     */
    private fun getSpellName(spell: SpellEnchantmentInterface): String =
        spell.spellKey
            .substringAfterLast(':')
            .removeSuffix("_enchantment")
            .replaceFirstChar { it.uppercase() }

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
            val spellList = spells.mapNotNull { SPELL_MAP[it] }
            val currentKey = item.selectedSpell
            val currentIndex = spellList.indexOfFirst { it.spellKey == currentKey }
            val nextSpell = spellList[(currentIndex + 1) % spellList.size]

            item.selectedSpell = nextSpell.spellKey
            getSpellName(nextSpell)
        }

    /** Gets the currently selected spell for the item. */
    private fun getSelectedSpell(item: ItemStack): SpellEnchantmentInterface? =
        getSpellsOnWand(item).takeIf { it.isNotEmpty() }?.let { spells ->
            val found = spells.find { SPELL_MAP[it]?.spellKey == item.selectedSpell }

            (found ?: spells.first()).let { SPELL_MAP[it] }
        }
}
