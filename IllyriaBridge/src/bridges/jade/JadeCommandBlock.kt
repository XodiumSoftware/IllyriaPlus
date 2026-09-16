package org.xodium.illyriabridge.bridges.jade

import net.minecraft.nbt.CompoundTag
import org.bukkit.block.Block
import org.bukkit.block.CommandBlock

/**
 * Provides a command block's command to Jade, truncated to 40 characters. Only op players
 * can trigger it — the client suppresses the request for anyone without gamemaster blocks access.
 */
internal object JadeCommandBlock : JadeBlockProvider {
    private const val MAX_LENGTH = 40

    override val key: String = "minecraft:command_block"

    override fun write(
        block: Block,
        tag: CompoundTag,
    ): Boolean {
        val commandBlock = block.state as? CommandBlock ?: return false
        var command = commandBlock.command
        if (command.length > MAX_LENGTH) command = command.take(MAX_LENGTH - 3) + "..."
        tag.putByteArray(key, stringPayload(command))
        return true
    }
}
