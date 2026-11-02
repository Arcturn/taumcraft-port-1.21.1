package thaumcraft.port.auromancy.effect;

import java.util.List;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common-side FX dispatch contains no client imports or static client class references. */
public final class FocusEffectVisuals {
    @FunctionalInterface public interface ParticleSink { void spawn(String key,Level level,Vec3 position,Vec3 velocity); }
    public static ParticleSink particles=(key,level,position,velocity)->{};
    public static Consumer<Payload> receiver=payload->{};
    public static Consumer<HoleBlockEntity> holeAmbient=hole->{};
    public static BiConsumer<Level,BlockPos> sapAmbient=(level,pos)->{};
    public static void particle(String key,Level level,Vec3 position,Vec3 velocity){if(level.isClientSide)particles.spawn(key,level,position,velocity);}
    public static void impact(Level level,Vec3 position,String... keys){send(level,new Payload(position,List.of(keys),0,0,false,false));}
    public static void bamf(Level level,Vec3 position,int colour,boolean sound,boolean flair){send(level,new Payload(position,List.of(),1,colour,sound,flair));}
    private static void send(Level world,Payload payload){if(world instanceof ServerLevel level)PacketDistributor.sendToPlayersNear(level,null,payload.position.x,payload.position.y,payload.position.z,64,payload);}
    public static void registerPayloads(RegisterPayloadHandlersEvent event){event.registrar("9").playToClient(Payload.TYPE,Payload.STREAM_CODEC,(payload,context)->context.enqueueWork(()->{if(payload.valid())receiver.accept(payload);}));}
    public record Payload(Vec3 position,List<String> keys,int kind,int colour,boolean sound,boolean flair) implements CustomPacketPayload {
        public static final Type<Payload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:focus_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Payload> STREAM_CODEC=new StreamCodec<>() {
            public Payload decode(RegistryFriendlyByteBuf buf){Vec3 pos=new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble());int size=buf.readVarInt();if(size<0||size>128)throw new IllegalArgumentException("Invalid focus impact count");var keys=new java.util.ArrayList<String>(size);for(int i=0;i<size;i++)keys.add(buf.readUtf(64));return new Payload(pos,List.copyOf(keys),buf.readVarInt(),buf.readInt(),buf.readBoolean(),buf.readBoolean());}
            public void encode(RegistryFriendlyByteBuf buf,Payload payload){buf.writeDouble(payload.position.x);buf.writeDouble(payload.position.y);buf.writeDouble(payload.position.z);buf.writeVarInt(payload.keys.size());for(String key:payload.keys)buf.writeUtf(key,64);buf.writeVarInt(payload.kind);buf.writeInt(payload.colour);buf.writeBoolean(payload.sound);buf.writeBoolean(payload.flair);}
        };
        public boolean valid(){return Double.isFinite(position.x)&&Double.isFinite(position.y)&&Double.isFinite(position.z)&&keys.size()<=128&&(kind==1||kind==0&&!keys.isEmpty());}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private FocusEffectVisuals(){}
}
