package thaumcraft.port.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Dedicated dust effects; ordinary craft/portal bursts keep their existing protocol. */
public record SalisMundusFxPayload(int phase, int playerId, InteractionHand hand,
                                  BlockPos target, Vec3 hit, List<BlockPos> blocks) implements CustomPacketPayload {
    public static final int START = 0, FINISH = 1;
    private static final int MAX_PARTS = 16;
    public static final Type<SalisMundusFxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus_fx"));

    public SalisMundusFxPayload { blocks = List.copyOf(blocks); }

    public static final StreamCodec<RegistryFriendlyByteBuf, SalisMundusFxPayload> STREAM_CODEC = new StreamCodec<>() {
        public SalisMundusFxPayload decode(RegistryFriendlyByteBuf buffer) {
            int phase = buffer.readUnsignedByte(), player = buffer.readVarInt();
            InteractionHand hand = buffer.readEnum(InteractionHand.class);
            BlockPos target = buffer.readBlockPos();
            Vec3 hit = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            int size = buffer.readVarInt();
            if (size < 1 || size > MAX_PARTS) throw new IllegalArgumentException("Invalid dust effect size");
            List<BlockPos> blocks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) blocks.add(buffer.readBlockPos());
            return new SalisMundusFxPayload(phase, player, hand, target, hit, blocks);
        }
        public void encode(RegistryFriendlyByteBuf buffer, SalisMundusFxPayload value) {
            buffer.writeByte(value.phase); buffer.writeVarInt(value.playerId); buffer.writeEnum(value.hand);
            buffer.writeBlockPos(value.target);
            buffer.writeDouble(value.hit.x); buffer.writeDouble(value.hit.y); buffer.writeDouble(value.hit.z);
            buffer.writeVarInt(value.blocks.size());
            value.blocks.forEach(buffer::writeBlockPos);
        }
    };

    public boolean valid() {
        return (phase == START || phase == FINISH) && hand != null
                && Double.isFinite(hit.lengthSqr()) && !blocks.isEmpty() && blocks.size() <= MAX_PARTS
                && target != null && Vec3.atCenterOf(target).distanceToSqr(hit) <= 4
                && blocks.stream().allMatch(pos -> Vec3.atCenterOf(pos).distanceToSqr(hit) <= 16 * 16);
    }

    public void send(ServerLevel level) {
        if (valid()) PacketDistributor.sendToPlayersNear(level, null, hit.x, hit.y, hit.z, 64, this);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
