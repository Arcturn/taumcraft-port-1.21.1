package thaumcraft.port.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.world.Wisp;

/** Original wisp zap uses entity IDs so endpoints match the client's current positions. */
public record WispZapPayload(int source,int target) implements CustomPacketPayload {
    public static final Type<WispZapPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:wisp_zap"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WispZapPayload> STREAM_CODEC=new StreamCodec<>() {
        public WispZapPayload decode(RegistryFriendlyByteBuf b){return new WispZapPayload(b.readVarInt(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,WispZapPayload p){b.writeVarInt(p.source);b.writeVarInt(p.target);}
    };
    public static void send(ServerLevel level,Wisp source,LivingEntity target){
        PacketDistributor.sendToPlayersNear(level,null,source.getX(),source.getY(),source.getZ(),32,new WispZapPayload(source.getId(),target.getId()));
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
