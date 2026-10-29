package thaumcraft.port.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** A map-node open acknowledges RESEARCH/PAGE only; clients cannot assign flags or rewards. */
public record BookReadPayload(String key) implements CustomPacketPayload {
    public static final Type<BookReadPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:book_read"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BookReadPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.stringUtf8(128),BookReadPayload::key,BookReadPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
