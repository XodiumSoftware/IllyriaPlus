package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/**
 * Owns the `bending` advancement tab. Builds the root advancement, registers all
 * [org.xodium.illyriacore.advancements.AdvancementInterface] singletons declared in
 * [advancements], and shows the tab to players once the API has loaded them.
 */
internal object BendingAdvancements : Listener {
    /** Namespace of the advancement tab. */
    private const val TAB_NAME = "bending"

    /** Background texture used by the tab's root advancement. */
    private const val BACKGROUND_TEXTURE = "textures/block/blue_ice.png"

    private val advancements: List<AdvancementInterface> =
        listOf(
            BendingAdvancement,
        )

    /** The live tab instance, available after [register] has been called. */
    var tab: AdvancementTab? = null
        private set

    /**
     * Creates the tab, builds the root advancement, and registers all declared advancements.
     * No-ops if called more than once.
     */
    fun register() {
        if (tab != null) return

        val created = instance.advancementMain.createAdvancementTab(instance, TAB_NAME)

        val root =
            RootAdvancement(
                created,
                "root",
                AdvancementDisplay(
                    Material.NETHER_STAR,
                    "Choose Your Path",
                    AdvancementFrameType.TASK,
                    true,
                    true,
                    1f,
                    0f,
                    "Commit to a bending type",
                ),
                BACKGROUND_TEXTURE,
            )

        created.registerAdvancements(root, advancements.map { it.build(root) }.toSet())
        created.automaticallyShowToPlayers()

        instance.server.pluginManager.registerEvents(this, instance)
        tab = created
    }

    @EventHandler
    fun on(event: PlayerLoadingCompletedEvent) {
        tab?.updateAdvancementsToTeam(event.player)
    }
}
