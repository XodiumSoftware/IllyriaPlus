package org.xodium.illyriacore.advancements

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType
import org.bukkit.Material
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/**
 * Fluent builder for an advancement tab, its root, and the whole tree of children.
 *
 * Usage:
 * ```
 * AdvancementBuilder
 *     .tab("bending")
 *     .icon(Material.WIND_CHARGE)
 *     .title("Bending")
 *     .description("Master the elements")
 *     .background("textures/block/blue_ice.png")
 *     .root {
 *         advancement("choose_path") {
 *             icon(Material.NETHER_STAR)
 *             title("Choose Your Path")
 *             description("Commit to a bending type")
 *             at(1f, 0f)
 *         }
 *     }.register()
 * ```
 */
internal object AdvancementBuilder {
    /** Starts building a new tab with the given namespace name (no plugin prefix). */
    fun tab(name: String): TabBuilder = TabBuilder(name)

    /** Builder describing tab-level display and background. Call [root] to add children. */
    class TabBuilder internal constructor(
        private val name: String,
    ) {
        private var icon: Material = Material.BOOK
        private var title: String = name.replaceFirstChar(Char::uppercase)
        private var description: String = ""
        private var background: String = "textures/block/stone.png"
        private var rootFrame: AdvancementFrameType = AdvancementFrameType.TASK
        private var rootShowToast: Boolean = false
        private var rootAnnounceChat: Boolean = false

        /** Sets the tab's root icon. */
        fun icon(material: Material) = apply { icon = material }

        /** Sets the tab's root title. */
        fun title(value: String) = apply { title = value }

        /** Sets the tab's root description (single line; use [descriptionLines] for multiple). */
        fun description(value: String) = apply { description = value }

        /** Sets the tab's root description as multiple lines. */
        fun descriptionLines(vararg lines: String) = apply { description = lines.joinToString("\n") }

        /** Sets the tab's background texture path. */
        fun background(texture: String) = apply { background = texture }

        /** Sets the root advancement's frame type. Defaults to [AdvancementFrameType.TASK]. */
        fun rootFrame(frame: AdvancementFrameType) = apply { rootFrame = frame }

        /** Whether the root advancement shows a toast. Defaults to `false`. */
        fun rootShowToast(show: Boolean) = apply { rootShowToast = show }

        /** Whether the root advancement announces in chat. Defaults to `false`. */
        fun rootAnnounceChat(announce: Boolean) = apply { rootAnnounceChat = announce }

        /** Builds the tab's root and declares the tree of children inside [block]. */
        fun root(block: RootScope.() -> Unit): RegisteredTab {
            val tab = instance.advancementMain.createAdvancementTab(instance, name)
            val rootAdvancement =
                RootAdvancement(
                    tab,
                    "root",
                    AdvancementDisplay(
                        icon,
                        title,
                        rootFrame,
                        rootShowToast,
                        rootAnnounceChat,
                        0f,
                        0f,
                        description,
                    ),
                    background,
                )
            val scope = RootScope(rootAdvancement)
            scope.block()
            return RegisteredTab(tab, rootAdvancement, scope.children.toSet())
        }
    }

    /** Scope inside [TabBuilder.root] where top-level advancements are declared. */
    class RootScope internal constructor(
        private val rootAdvancement: RootAdvancement,
    ) {
        internal val children: MutableList<BaseAdvancement> = mutableListOf()

        /** Declares a child advancement directly under the tab root. */
        fun advancement(
            key: String,
            block: AdvancementScope.() -> Unit,
        ): BaseAdvancement {
            val scope = AdvancementScope(rootAdvancement, children)
            scope.block()
            return scope.build(key).also { children.add(it) }
        }
    }

    /** Scope inside an `advancement {}` block. Configure the advancement's display here. */
    class AdvancementScope internal constructor(
        private val parent: Advancement,
        private val sink: MutableList<BaseAdvancement>,
    ) {
        private var icon: Material = Material.BOOK
        private var title: String = "Advancement"
        private var description: String = ""
        private var frame: AdvancementFrameType = AdvancementFrameType.TASK
        private var showToast: Boolean = true
        private var announceChat: Boolean = true
        private var maxProgression: Int = 1
        private var x: Float = 0f
        private var y: Float = 0f
        private val nestedChildren = mutableListOf<Pair<String, AdvancementScope>>()

        /** Sets the icon shown for this advancement. */
        fun icon(material: Material) = apply { icon = material }

        /** Sets the title shown for this advancement. */
        fun title(value: String) = apply { title = value }

        /** Sets the description shown for this advancement (single line). */
        fun description(value: String) = apply { description = value }

        /** Sets the description shown for this advancement as multiple lines. */
        fun descriptionLines(vararg lines: String) = apply { description = lines.joinToString("\n") }

        /** Sets the frame type. Defaults to [AdvancementFrameType.TASK]. */
        fun frame(value: AdvancementFrameType) = apply { frame = value }

        /** Whether a toast is shown when this advancement is granted. Defaults to `true`. */
        fun showToast(show: Boolean) = apply { showToast = show }

        /** Whether an announcement is sent in chat when granted. Defaults to `true`. */
        fun announceChat(announce: Boolean) = apply { announceChat = announce }

        /** Sets the maximum progression required to complete. Defaults to `1`. */
        fun maxProgression(value: Int) = apply { maxProgression = value }

        /** Sets the grid position of this advancement within its tab. */
        fun at(
            x: Float,
            y: Float,
        ) = apply {
            this.x = x
            this.y = y
        }

        /**
         * Declares a nested child advancement parented to this one and returns the child's
         * scope for further nesting. The child is constructed lazily when the parent's
         * [build] runs.
         */
        fun nested(
            key: String,
            block: AdvancementScope.() -> Unit,
        ): AdvancementScope {
            val child = AdvancementScope(parent, sink)
            child.block()
            nestedChildren += key to child
            return child
        }

        /** Builds this [BaseAdvancement], then recursively builds all nested children. */
        internal fun build(
            key: String,
            parentOverride: Advancement? = null,
        ): BaseAdvancement {
            val self =
                BaseAdvancement(
                    key,
                    AdvancementDisplay(
                        icon,
                        title,
                        frame,
                        showToast,
                        announceChat,
                        x,
                        y,
                        description,
                    ),
                    parentOverride ?: parent,
                    maxProgression,
                )
            nestedChildren.forEach { (childKey, childScope) ->
                childScope.build(childKey, self).also { sink.add(it) }
            }
            return self
        }
    }

    /** Result of [TabBuilder.root]; call [register] to commit to the server. */
    class RegisteredTab internal constructor(
        private val tab: AdvancementTab,
        private val root: RootAdvancement,
        private val children: Set<BaseAdvancement>,
    ) {
        /** The live [AdvancementTab] reference, valid after [register]. */
        val advancementTab: AdvancementTab get() = tab

        /** The tab's [RootAdvancement], valid after [register]. */
        val rootAdvancement: RootAdvancement get() = root

        /** All children registered with this tab, valid after [register]. */
        val advancements: Set<BaseAdvancement> get() = children

        /**
         * Registers the root and all children in a single
         * [AdvancementTab.registerAdvancements] call and shows the tab to players.
         */
        fun register(): RegisteredTab {
            tab.registerAdvancements(root, children)
            tab.automaticallyShowToPlayers()
            return this
        }

        /** Looks up a registered child by its key. */
        operator fun get(key: String): BaseAdvancement? = children.firstOrNull { it.key.key == key }
    }
}
