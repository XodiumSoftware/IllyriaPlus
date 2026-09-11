package org.xodium.illyriabridge.payloads

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.SkipPacketDecoderException
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeSerializer

/**
 * Payload for synchronizing recipes with Fabric clients.
 * Matches Fabric API's ClientboundRecipeSyncPayload format exactly.
 *
 * @property entries List of recipe entries grouped by serializer
 */
internal data class FabricRecipeSyncPayload(
    val entries: List<Entry>,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    /**
     * Represents a group of recipes that share the same serializer.
     *
     * @property serializer The recipe serializer used for this group
     * @property recipes List of recipe holders using this serializer
     */
    data class Entry(
        val serializer: RecipeSerializer<*>,
        val recipes: List<RecipeHolder<*>>,
    ) {
        companion object {
            /** Stream codec for encoding/decoding Entry instances */
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Entry> = StreamCodec.ofMember(Entry::write, Entry::read)

            /**
             * Reads an Entry from the given buffer.
             *
             * @param buf The buffer to read from
             * @return The decoded Entry
             */
            private fun read(buf: RegistryFriendlyByteBuf): Entry {
                val recipeSerializerId = buf.readIdentifier()
                val recipeSerializer =
                    BuiltInRegistries.RECIPE_SERIALIZER.getValue(recipeSerializerId)
                        ?: throw SkipPacketDecoderException(
                            "Tried syncing unsupported packet serializer '$recipeSerializerId'!",
                        )

                val count = buf.readVarInt()
                val recipes = ArrayList<RecipeHolder<*>>(count)

                repeat(count) {
                    val id = buf.readResourceKey(Registries.RECIPE)

                    @Suppress("UNCHECKED_CAST", "DEPRECATION")
                    val recipe =
                        (recipeSerializer.streamCodec() as StreamCodec<RegistryFriendlyByteBuf, Recipe<*>>).decode(buf)

                    recipes.add(RecipeHolder(id, recipe))
                }

                return Entry(recipeSerializer, recipes)
            }
        }

        /**
         * Writes this Entry to the given buffer.
         *
         * @param buf The buffer to write to
         */
        private fun write(buf: RegistryFriendlyByteBuf) {
            buf.writeIdentifier(BuiltInRegistries.RECIPE_SERIALIZER.getKey(this.serializer)!!)
            buf.writeVarInt(this.recipes.size)

            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val streamCodec = this.serializer.streamCodec() as StreamCodec<RegistryFriendlyByteBuf, Recipe<*>>

            for (recipe in this.recipes) {
                buf.writeResourceKey(recipe.id())
                streamCodec.encode(buf, recipe.value())
            }
        }
    }

    companion object {
        /** Stream codec for encoding/decoding FabricRecipeSyncPayload instances */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, FabricRecipeSyncPayload> =
            Entry.CODEC
                .apply(ByteBufCodecs.list())
                .map(
                    { FabricRecipeSyncPayload(it) },
                    { it.entries },
                )

        /** The payload type identifier for Fabric recipe sync */
        val TYPE: CustomPacketPayload.Type<FabricRecipeSyncPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath("fabric", "recipe_sync"))
    }
}
