package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType

/**
 * Represents a contract for an advancement contributed to the IllyriaPlus advancement tab.
 *
 * Implementations are Kotlin `object` singletons that describe a single advancement (display,
 * parent, criteria). The root of the tab is owned by the advancements registrar in the main
 * class.
 */
internal interface AdvancementInterface {
    /**
     * Stable identifier for this advancement within the IllyriaPlus tab namespace (no
     * namespace prefix — the tab supplies it). Derived from the class name by default.
     */
    val key: String get() = this::class.simpleName!!.removeSuffix("Advancement").lowercase()

    /**
     * The [AdvancementDisplay] shown for this advancement in the tab.
     *
     * Implementations should set sensible defaults: `showToast = true`, `announceChat = true`,
     * and grid coordinates that place the advancement under its parent row.
     */
    val display: AdvancementDisplay

    /**
     * The parent advancement this one hangs off of. Defaults to the tab root. Override to
     * build chains.
     */
    val parent: BaseAdvancement? get() = null

    /**
     * The maximum progress required to complete this advancement. `1` means single-shot.
     */
    val maxProgression: Int get() = 1

    /**
     * The frame type used if [display] is not overridden.
     */
    val frame: AdvancementFrameType get() = AdvancementFrameType.TASK

    /**
     * Builds (or returns the cached) [BaseAdvancement] registered with the tab.
     *
     * @param root The tab's [RootAdvancement], used as default parent when [parent] is null.
     * @return The constructed [BaseAdvancement] ready to be passed to
     *   `AdvancementTab.registerAdvancements(root, setOf(...))`.
     */
    fun build(root: RootAdvancement): BaseAdvancement =
        BaseAdvancement(
            key,
            display,
            parent ?: root,
            maxProgression,
        )
}
