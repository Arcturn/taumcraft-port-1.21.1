package thaumcraft.port.auromancy.focal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FocalPayloads {
    public record Edit(int container,String token,long revision,long sequence,CompoundTag data) implements CustomPacketPayload {
        public static final Type<Edit> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:focal_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Edit::container,ByteBufCodecs.stringUtf8(36),Edit::token,ByteBufCodecs.VAR_LONG,Edit::revision,ByteBufCodecs.VAR_LONG,Edit::sequence,ByteBufCodecs.COMPOUND_TAG,Edit::data,Edit::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Sync(int container,String token,CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:focal_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Sync> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Sync::container,ByteBufCodecs.stringUtf8(36),Sync::token,ByteBufCodecs.COMPOUND_TAG,Sync::data,Sync::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("9");
        registrar.playToServer(Edit.TYPE,Edit.CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer player&&player.containerMenu instanceof FocalManipulatorMenu menu)menu.edit(player,payload);
        }));
        registrar.playToClient(Sync.TYPE,Sync.CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player().containerMenu instanceof FocalManipulatorMenu menu&&menu.containerId==payload.container())menu.receive(payload.token(),payload.data());
        }));
    }
    private FocalPayloads(){}
}
