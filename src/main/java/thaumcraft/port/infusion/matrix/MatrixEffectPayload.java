package thaumcraft.port.infusion.matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** BETA26 source absorption and instability arcs, issued only by the server. */
public record MatrixEffectPayload(BlockPos matrix,Vec3 source,int entity,int kind) implements CustomPacketPayload {
    public static final int SOURCE=0,ARC=1,BURST=2;
    public static final Type<MatrixEffectPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:matrix_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MatrixEffectPayload> STREAM_CODEC=new StreamCodec<>(){
        public MatrixEffectPayload decode(RegistryFriendlyByteBuf b){return new MatrixEffectPayload(b.readBlockPos(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,MatrixEffectPayload p){b.writeBlockPos(p.matrix);b.writeDouble(p.source.x);b.writeDouble(p.source.y);b.writeDouble(p.source.z);b.writeVarInt(p.entity);b.writeVarInt(p.kind);}
    };
    public static void send(ServerLevel level,BlockPos matrix,Vec3 source,int entity,int kind){PacketDistributor.sendToPlayersNear(level,null,matrix.getX()+.5,matrix.getY()+.5,matrix.getZ()+.5,32,new MatrixEffectPayload(matrix,source,entity,kind));}
    public boolean valid(){return kind>=0&&kind<=2&&Double.isFinite(source.x)&&Double.isFinite(source.y)&&Double.isFinite(source.z)&&Vec3.atCenterOf(matrix).distanceToSqr(source)<=2048;}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
