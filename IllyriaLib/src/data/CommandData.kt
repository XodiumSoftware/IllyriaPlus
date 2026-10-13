package org.xodium.illyrialib.data

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack

/**
 * Represents the data structure for a command definition, including its builder, description and aliases.
 *
 * @property builder The [LiteralArgumentBuilder] used to define the command logic and structure.
 * @property description A description of the command, providing context or information about its purpose.
 * @property aliases A list of aliases that can be used as alternative command triggers. Defaults to an empty list if no aliases are provided.
 */
data class CommandData(
    val builder: LiteralArgumentBuilder<CommandSourceStack>,
    val description: String,
    val aliases: List<String> = emptyList(),
)
