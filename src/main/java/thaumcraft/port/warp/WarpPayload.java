package thaumcraft.port.warp;

import java.util.function.IntConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-confirmed, player-private hallucinations. Never accepts client gameplay changes. */
public record WarpPayload(int kind) implements CustomPacketPayload {
    public static final int HEARTBEAT = 0, MIST = 1, SHORT_MIST = 2, HISS = 3, EXPLOSION = 4;
    public static final Type<WarpPayload> TYPE = new Type<>(ResourceLocation.parse("thaumcraft:warp_event"));
    // Distribution-safe bridge: only client setup installs a renderer/sound receiver.
    public static IntConsumer receiver = kind -> {};
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpPayload> STREAM_CODEC = new StreamCodec<>() {
        public WarpPayload decode(RegistryFriendlyByteBuf buffer) { return new WarpPayload(buffer.readVarInt()); }
        public void encode(RegistryFriendlyByteBuf buffer, WarpPayload payload) { buffer.writeVarInt(payload.kind); }
    };
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("9").playToClient(TYPE, STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> { if (payload.valid()) receiver.accept(payload.kind); }));
    }
    public static void send(ServerPlayer player, int kind) {
        if (kind >= HEARTBEAT && kind <= EXPLOSION) PacketDistributor.sendToPlayer(player, new WarpPayload(kind));
    }
    public boolean valid() { return kind >= HEARTBEAT && kind <= EXPLOSION; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
