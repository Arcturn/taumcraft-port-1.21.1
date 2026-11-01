package thaumcraft.port.infusion.armor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record InfusionFxPayload(int kind,BlockPos pos,int source,int target) implements CustomPacketPayload {
    public static final Type<InfusionFxPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:infusion_equipment_fx"));
    public static java.util.function.Consumer<InfusionFxPayload> client=packet->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,InfusionFxPayload> CODEC=new StreamCodec<>(){
        public InfusionFxPayload decode(RegistryFriendlyByteBuf b){return new InfusionFxPayload(b.readUnsignedByte(),b.readBlockPos(),b.readVarInt(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,InfusionFxPayload p){b.writeByte(p.kind);b.writeBlockPos(p.pos);b.writeVarInt(p.source);b.writeVarInt(p.target);}
    };
    public static void register(IEventBus bus){bus.addListener((RegisterPayloadHandlersEvent event)->event.registrar("9").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->client.accept(p))));}
    public static void sounding(ServerPlayer player,BlockPos pos,int rank){PacketDistributor.sendToPlayer(player,new InfusionFxPayload(0,pos,Math.clamp(rank,1,4),0));}
    public static void shield(ServerPlayer player,net.minecraft.world.damagesource.DamageSource source){
        int target=source.getEntity()==null?-1:source.getEntity().getId();
        if(source.is(net.minecraft.world.damagesource.DamageTypes.FALL))target=-2;
        if(source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK))target=-3;
        near(player.serverLevel(),player,new InfusionFxPayload(1,BlockPos.ZERO,player.getId(),target));
    }
    public static void arc(ServerLevel level,Entity source,Entity target){near(level,source,new InfusionFxPayload(2,BlockPos.ZERO,source.getId(),target.getId()));}
    private static void near(ServerLevel level,Entity source,InfusionFxPayload payload){PacketDistributor.sendToPlayersNear(level,null,source.getX(),source.getY(),source.getZ(),64,payload);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
