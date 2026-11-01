package thaumcraft.port.essentia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ThaumatoriumSyncPayload(int container, String token, CompoundTag data) implements CustomPacketPayload {
    public static final Type<ThaumatoriumSyncPayload> TYPE = new Type<>(ResourceLocation.parse("thaumcraft:thaumatorium_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ThaumatoriumSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThaumatoriumSyncPayload::container,
            ByteBufCodecs.stringUtf8(36), ThaumatoriumSyncPayload::token,
            ByteBufCodecs.COMPOUND_TAG, ThaumatoriumSyncPayload::data, ThaumatoriumSyncPayload::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
