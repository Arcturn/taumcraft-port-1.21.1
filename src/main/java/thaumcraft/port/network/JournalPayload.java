package thaumcraft.port.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client only. A client cannot submit or alter research progress. */
public record JournalPayload(int scannedCount, String discovered, net.minecraft.nbt.CompoundTag progress,boolean open) implements CustomPacketPayload {
    public JournalPayload(int n,String d,net.minecraft.nbt.CompoundTag p){this(n,d,p,true);}
    public static final Type<JournalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "journal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JournalPayload::scannedCount,
            ByteBufCodecs.STRING_UTF8, JournalPayload::discovered,
            ByteBufCodecs.COMPOUND_TAG, JournalPayload::progress,
            ByteBufCodecs.BOOL, JournalPayload::open,
            JournalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
