package thaumcraft.port.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Small server-authoritative effect messages; kind 0=burst, 1=arc, 2=motes. */
public record WorldFxPayload(int kind, Vec3 from, Vec3 to, int color) implements CustomPacketPayload {
    public static final Type<WorldFxPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","world_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WorldFxPayload> STREAM_CODEC=new StreamCodec<>() {
        public WorldFxPayload decode(RegistryFriendlyByteBuf b) {
            return new WorldFxPayload(b.readUnsignedByte(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
                    new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readInt());
        }
        public void encode(RegistryFriendlyByteBuf b,WorldFxPayload p) {
            b.writeByte(p.kind);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);
            b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeInt(p.color);
        }
    };
    public static void send(ServerLevel level,int kind,Vec3 from,Vec3 to,int color) {
        PacketDistributor.sendToPlayersNear(level,null,from.x,from.y,from.z,48,new WorldFxPayload(kind,from,to,color));
    }
    public boolean valid() { return kind>=0&&kind<=3&&Double.isFinite(from.lengthSqr())&&Double.isFinite(to.lengthSqr())&&from.distanceToSqr(to)<=64*64; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
