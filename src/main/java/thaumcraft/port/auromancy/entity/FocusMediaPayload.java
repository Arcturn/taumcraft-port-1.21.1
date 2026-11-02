package thaumcraft.port.auromancy.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record FocusMediaPayload(int kind,Vec3 from,Vec3 to,int color,float width,List<String> effects) implements CustomPacketPayload {
    public static final Type<FocusMediaPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:focus_medium"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FocusMediaPayload> STREAM_CODEC=new StreamCodec<>() {
        public FocusMediaPayload decode(RegistryFriendlyByteBuf b) {
            int kind=b.readVarInt();var from=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());var to=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());int color=b.readInt();float width=b.readFloat();
            int size=b.readVarInt();if(size<0||size>128)throw new IllegalArgumentException("Invalid focus effect count");var effects=new ArrayList<String>();for(int i=0;i<size;i++)effects.add(b.readUtf(128));
            return new FocusMediaPayload(kind,from,to,color,width,List.copyOf(effects));
        }
        public void encode(RegistryFriendlyByteBuf b,FocusMediaPayload p) {b.writeVarInt(p.kind);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeInt(p.color);b.writeFloat(p.width);b.writeVarInt(p.effects.size());for(var key:p.effects)b.writeUtf(key,128);}
    };
    public static void register(RegisterPayloadHandlersEvent event) {event.registrar("9").playToClient(TYPE,STREAM_CODEC,(packet,context)->context.enqueueWork(()-> {if(packet.valid())FocusMediaVisuals.receive.accept(packet);}));}
    public boolean valid() {return (kind==0||kind==1)&&Double.isFinite(from.lengthSqr())&&Double.isFinite(to.lengthSqr())&&Float.isFinite(width)&&width>=0&&width<=10000;}
    @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
