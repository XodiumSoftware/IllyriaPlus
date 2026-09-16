package org.xodium.illyriabridge.bridges.jade

import io.netty.buffer.Unpooled
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player

/**
 * Resolves a registry access for codec encoding/decoding from a player or the server.
 *
 * @param player The player, or null to use the server's access
 * @return The registry access
 */
internal fun registryAccess(player: Player?) =
    player?.let { (it as CraftPlayer).handle.level().registryAccess() }
        ?: (org.bukkit.Bukkit.getServer() as CraftServer).handle.server.registryAccess()

/**
 * Creates a fresh registry-aware buffer bound to the server's registry access.
 *
 * @return The new buffer
 */
internal fun newBuffer(): RegistryFriendlyByteBuf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))

/**
 * Reads this buffer's readable bytes into a new byte array, starting at index 0.
 * The buffer's reader index is left unchanged.
 *
 * @return A byte array containing the buffer's readable bytes
 */
internal fun RegistryFriendlyByteBuf.toByteArray(): ByteArray {
    val bytes = ByteArray(readableBytes())
    getBytes(0, bytes)
    return bytes
}

/**
 * Encodes a single boolean into Jade's codec format.
 *
 * @param value The boolean to encode
 * @return The 1-byte payload
 */
internal fun boolPayload(value: Boolean): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeBoolean(value)
    return buf.toByteArray()
}

/**
 * Encodes a single int as a VarInt into Jade's codec format.
 *
 * @param value The int to encode
 * @return The encoded payload
 */
internal fun varIntPayload(value: Int): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeVarInt(value)
    return buf.toByteArray()
}

/**
 * Encodes a float into Jade's codec format (4 bytes, big-endian).
 *
 * @param value The float to encode
 * @return The encoded payload
 */
internal fun floatPayload(value: Float): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeFloat(value)
    return buf.toByteArray()
}

/**
 * Encodes a string into Jade's codec format (VarInt length + UTF-8 bytes).
 *
 * @param value The string to encode
 * @return The encoded payload
 */
internal fun stringPayload(value: String): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeUtf(value)
    return buf.toByteArray()
}

/**
 * Encodes two ints as VarInts into Jade's codec format.
 *
 * @param first The first int
 * @param second The second int
 * @return The encoded payload
 */
internal fun varIntPairPayload(
    first: Int,
    second: Int,
): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeVarInt(first)
    buf.writeVarInt(second)
    return buf.toByteArray()
}

/**
 * Encodes a single byte into Jade's codec format.
 *
 * @param value The byte to encode
 * @return The 1-byte payload
 */
internal fun bytePayload(value: Int): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeByte(value)
    return buf.toByteArray()
}

/**
 * Encodes a Bukkit item stack into Jade's codec format as the NMS optional item stack
 * (VarInt count; empty when null/air).
 *
 * @param stack The Bukkit item stack, or null for empty
 * @return The encoded payload
 */
internal fun itemStackPayload(stack: org.bukkit.inventory.ItemStack?): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    val nms = stack?.let { CraftItemStack.asNMSCopy(it) } ?: ItemStack.EMPTY
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, nms)
    return buf.toByteArray()
}

/**
 * Encodes a furnace's state into Jade's codec format: cook progress, total cook time,
 * then the 3 inventory slots (input, fuel, result) as optional item stacks.
 *
 * @param progress Current cook ticks elapsed
 * @param total Total cook ticks for the current recipe
 * @param slots The 3 inventory slots in order: input, fuel, result
 * @return The encoded payload
 */
internal fun furnacePayload(
    progress: Int,
    total: Int,
    vararg slots: org.bukkit.inventory.ItemStack?,
): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    buf.writeVarInt(progress)
    buf.writeVarInt(total)
    buf.writeVarInt(slots.size)
    slots.forEach { ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, it?.let(CraftItemStack::asNMSCopy) ?: ItemStack.EMPTY) }
    return buf.toByteArray()
}

/**
 * Encodes a plain-text chat component into Jade's codec format, using the registry-aware
 * component codec used for the animal owner provider.
 *
 * @param text The text to encode
 * @return The encoded payload
 */
internal fun componentPayload(text: String): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
    ComponentSerialization.STREAM_CODEC.encode(buf, Component.literal(text))
    return buf.toByteArray()
}

/**
 * Encodes a universal item-storage payload: a map entry of extension uid to a single view group
 * containing the given NMS item stacks. Wraps the inner stacks in ViewGroup(uid-less, data-less).
 *
 * The result goes under the "minecraft:item_storage" NBT key as a byte array.
 *
 * @param uid The extension provider uid (e.g. "minecraft:campfire")
 * @param stacks The item stacks to include in the single view group
 * @return The encoded payload
 */
internal fun itemStoragePayload(
    uid: String,
    stacks: List<net.minecraft.world.item.ItemStack>,
): ByteArray {
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))

    buf.writeUtf(uid) // extension uid
    buf.writeVarInt(1) // one view group

    // ViewGroup: list of stacks, Optional<String> id, Optional<CompoundTag> extraData
    buf.writeVarInt(stacks.size)
    stacks.forEach { ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, it) }
    buf.writeBoolean(false) // id absent
    buf.writeBoolean(false) // extraData absent

    return buf.toByteArray()
}
