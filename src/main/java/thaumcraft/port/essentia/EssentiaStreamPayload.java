package thaumcraft.port.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.aspect.Aspect;

/** Server-only notification: clients cannot request or mutate an essentia transfer. */
public record EssentiaStreamPayload(BlockPos from,BlockPos to,int color) implements CustomPacketPayload {
    public static final Type<EssentiaStreamPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:essentia_stream"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EssentiaStreamPayload> STREAM_CODEC=new StreamCodec<>(){
        public EssentiaStreamPayload decode(RegistryFriendlyByteBuf buffer){return new EssentiaStreamPayload(buffer.readBlockPos(),buffer.readBlockPos(),buffer.readInt());}
        public void encode(RegistryFriendlyByteBuf buffer,EssentiaStreamPayload payload){buffer.writeBlockPos(payload.from);buffer.writeBlockPos(payload.to);buffer.writeInt(payload.color);}
    };
    public static void send(ServerLevel level,BlockPos from,BlockPos to,Aspect aspect){PacketDistributor.sendToPlayersNear(level,null,to.getX()+.5,to.getY()+.5,to.getZ()+.5,32,new EssentiaStreamPayload(from,to,aspect.color()));}
    public boolean valid(){return from.distSqr(to)>0&&from.distSqr(to)<=3*16*16;}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
