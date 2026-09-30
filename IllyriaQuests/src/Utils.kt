@file:Suppress("Unused")

package org.xodium.illyriaquests

import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.DisplayInfo
import net.minecraft.advancements.triggers.Criterion
import net.minecraft.advancements.triggers.ImpossibleTrigger
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStackTemplate
import org.bukkit.Material
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaquests.IllyriaQuests.Companion.instance
import java.util.Optional

/** General utilities. */
internal object Utils {
    /** Quest toast utilities for fake advancement toasts via NMS packets. */
    object Toast {
        /** Frame types for the advancement toast (maps to NMS [AdvancementType]). */
        enum class Frame {
            TASK,
            GOAL,
            CHALLENGE,
        }

        private val ROOT_KEY = Identifier.fromNamespaceAndPath(instance.name.lowercase(), "root")
        private val TOAST_KEY = Identifier.fromNamespaceAndPath(instance.name.lowercase(), "toast")

        /**
         * Shows an advancement-style toast notification to this player.
         *
         * Sends a [ClientboundUpdateAdvancementsPacket] directly to the client containing a
         * root "notification" advancement and the toast itself, then immediately sends a
         * removal packet. Nothing is persisted server-side — the toast is purely packet-driven.
         *
         * @param title The toast title component.
         * @param description The toast description component.
         * @param icon The item displayed as the toast icon.
         * @param frame The toast frame type.
         */
        fun Player.showToast(
            title: Component,
            description: Component,
            icon: ItemStack,
            frame: Frame = Frame.TASK,
        ) {
            val nms = (this as CraftPlayer).handle

            val rootAdvancement =
                createAdvancement(
                    parent = null,
                    icon = ItemStack.of(Material.STONE),
                    title = Component.text("Quests"),
                    description = Component.empty(),
                    frame = AdvancementType.TASK,
                    showToast = false,
                    hidden = true,
                )

            val toastAdvancement =
                createAdvancement(
                    parent = ROOT_KEY,
                    icon = icon,
                    title = title,
                    description = description,
                    frame = AdvancementType.valueOf(frame.name),
                    showToast = true,
                    hidden = false,
                )

            val rootHolder = AdvancementHolder(ROOT_KEY, rootAdvancement)
            val toastHolder = AdvancementHolder(TOAST_KEY, toastAdvancement)

            val rootPositioned = ClientboundUpdateAdvancementsPacket.PositionedAdvancement(rootHolder, 0f, 0f)
            val toastPositioned = ClientboundUpdateAdvancementsPacket.PositionedAdvancement(toastHolder, 1f, 0f)

            val rootProgress =
                AdvancementProgress().apply {
                    update(rootAdvancement.requirements())
                    getCriterion("0")?.grant()
                }
            val toastProgress =
                AdvancementProgress().apply {
                    update(toastAdvancement.requirements())
                    getCriterion("0")?.grant()
                }

            runCatching {
                nms.connection.send(
                    ClientboundUpdateAdvancementsPacket(
                        false,
                        listOf(rootPositioned, toastPositioned),
                        emptySet(),
                        mapOf(ROOT_KEY to rootProgress, TOAST_KEY to toastProgress),
                        true,
                    ),
                )

                server.scheduler.runTaskLater(
                    instance,
                    Runnable {
                        nms.connection.send(
                            ClientboundUpdateAdvancementsPacket(
                                false,
                                emptyList(),
                                setOf(ROOT_KEY, TOAST_KEY),
                                emptyMap(),
                                true,
                            ),
                        )
                    },
                    2,
                )
            }.onFailure {
                instance.logger.warning("Failed to show toast to $name: ${it.message}")
            }
        }

        private fun createAdvancement(
            parent: Identifier?,
            icon: ItemStack,
            title: Component,
            description: Component,
            frame: AdvancementType,
            showToast: Boolean,
            hidden: Boolean,
        ): Advancement =
            Advancement(
                Optional.ofNullable(parent),
                Optional.of(
                    DisplayInfo(
                        ItemStackTemplate.fromNonEmptyStack(CraftItemStack.asNMSCopy(icon)),
                        PaperAdventure.asVanilla(title),
                        PaperAdventure.asVanilla(description),
                        Optional.empty(),
                        frame,
                        showToast,
                        false,
                        hidden,
                    ),
                ),
                AdvancementRewards.EMPTY,
                mapOf("0" to Criterion(ImpossibleTrigger(), ImpossibleTrigger.TriggerInstance())),
                AdvancementRequirements(listOf(listOf("0"))),
                false,
                Optional.empty(),
            )
    }
}
