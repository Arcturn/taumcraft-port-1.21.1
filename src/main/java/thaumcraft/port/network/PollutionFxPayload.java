package thaumcraft.port.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

/** TC6's separate pollution puff packet also survives removal of the source block. */
public record PollutionFxPayload(BlockPos pos, int amount) implements CustomPacketPayload {
    public static final int MAX_PARTICLES = 40;
    public static final Type<PollutionFxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "pollution_fx"));

    public PollutionFxPayload {
        pos = pos.immutable();
        if (amount < 0 || amount > MAX_PARTICLES) throw new IllegalArgumentException("Invalid pollution particle count");
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PollutionFxPayload> STREAM_CODEC = new StreamCodec<>() {
        public PollutionFxPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PollutionFxPayload(buffer.readBlockPos(), buffer.readUnsignedByte());
        }
        public void encode(RegistryFriendlyByteBuf buffer, PollutionFxPayload value) {
            buffer.writeBlockPos(value.pos); buffer.writeByte(value.amount);
        }
    };

    public static int particleCount(float pollution) {
        if (!(pollution > 0) || !Float.isFinite(pollution)) return 0;
        // Original: at least one for positive pollution, integer truncation, at most forty.
        // Apply the cap before encoding so a large cleanup cannot overflow a signed byte.
        return Math.min(MAX_PARTICLES, Math.max(1, (int)pollution));
    }

    public static void send(ServerLevel level, BlockPos pos, float pollution) {
        int count = particleCount(pollution);
        if (count > 0) PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(),
                32, new PollutionFxPayload(pos, count));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
