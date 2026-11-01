package thaumcraft.port.essentia;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ThaumatoriumSelectPayload(int container, String token, long revision, ResourceLocation recipe) implements CustomPacketPayload {
    public static final Type<ThaumatoriumSelectPayload> TYPE = new Type<>(ResourceLocation.parse("thaumcraft:thaumatorium_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ThaumatoriumSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThaumatoriumSelectPayload::container,
            ByteBufCodecs.stringUtf8(36), ThaumatoriumSelectPayload::token,
            ByteBufCodecs.VAR_LONG, ThaumatoriumSelectPayload::revision,
            ResourceLocation.STREAM_CODEC, ThaumatoriumSelectPayload::recipe, ThaumatoriumSelectPayload::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
