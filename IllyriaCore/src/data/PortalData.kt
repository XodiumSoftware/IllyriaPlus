package org.xodium.illyriacore.data

import org.bukkit.World
import org.xodium.illyriacore.items.ItemInterface

/**
 * Represents the key requirement data for a key-gated portal type.
 *
 * @property targetEnvironment The dimension the portal leads to.
 * @property keyItem The key item required to travel through the portal.
 * @property title The MiniMessage formatted title shown on rejection.
 * @property subtitle The MiniMessage formatted subtitle shown on rejection.
 */
internal data class PortalData(
    val targetEnvironment: World.Environment,
    val keyItem: ItemInterface,
    val title: String,
    val subtitle: String,
)
