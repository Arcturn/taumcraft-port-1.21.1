package thaumcraft.port.golem.seal;

import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SealPayloads {
    public record Sync(ResourceLocation dimension,CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Sync> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:golem_seals"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Sync> CODEC=StreamCodec.composite(ResourceLocation.STREAM_CODEC,Sync::dimension,ByteBufCodecs.COMPOUND_TAG,Sync::tag,Sync::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record MenuSync(int container,CompoundTag tag) implements CustomPacketPayload {
        public static final Type<MenuSync> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:golem_seal_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf,MenuSync> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,MenuSync::container,ByteBufCodecs.COMPOUND_TAG,MenuSync::tag,MenuSync::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record LogisticsAction(int container,int slot,int count,int row,String search,int revision) implements CustomPacketPayload {
        public static final Type<LogisticsAction> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:golem_logistics_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,LogisticsAction> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,LogisticsAction::container,ByteBufCodecs.VAR_INT,LogisticsAction::slot,ByteBufCodecs.VAR_INT,LogisticsAction::count,ByteBufCodecs.VAR_INT,LogisticsAction::row,ByteBufCodecs.stringUtf8(40),LogisticsAction::search,ByteBufCodecs.VAR_INT,LogisticsAction::revision,LogisticsAction::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("9");
        registrar.playToClient(Sync.TYPE,Sync.CODEC,(p,c)->c.enqueueWork(()->{var level=c.player().level();if(level.dimension().location().equals(p.dimension()))SealHandler.acceptClient(level,p.tag());}));
        registrar.playToClient(MenuSync.TYPE,MenuSync.CODEC,(p,c)->c.enqueueWork(()->{var menu=c.player().containerMenu;if(menu.containerId!=p.container())return;if(menu instanceof SealMenu seal)seal.receive(p.tag());else if(menu instanceof LogisticsMenu logistics)logistics.receive(p.tag());}));
        registrar.playToServer(LogisticsAction.TYPE,LogisticsAction.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player&&player.containerMenu instanceof LogisticsMenu menu&&menu.containerId==p.container())menu.action(player,p);}));
    }
    private static CompoundTag listTag(ServerLevel level,java.util.Collection<SealEntity> seals){var tag=new CompoundTag();var list=new ListTag();for(var seal:seals)list.add(seal.writeNBT(level.registryAccess()));tag.put("seals",list);return tag;}
    public static void send(ServerLevel level,SealEntity seal){var payload=new Sync(level.dimension().location(),listTag(level,java.util.List.of(seal)));PacketDistributor.sendToPlayersTrackingChunk(level,new ChunkPos(seal.getSealPos().pos),payload);}
    public static void remove(ServerLevel level,SealPos pos){var tag=new CompoundTag();tag.putLong("remove",pos.pos.asLong());tag.putByte("face",(byte)pos.face.ordinal());PacketDistributor.sendToPlayersTrackingChunk(level,new ChunkPos(pos.pos),new Sync(level.dimension().location(),tag));}
    public static void sendChunk(ServerPlayer player,ServerLevel level,ChunkPos chunk){var tag=listTag(level,SealHandler.getSealsInChunk(level,chunk));tag.putLong("chunk",chunk.toLong());PacketDistributor.sendToPlayer(player,new Sync(level.dimension().location(),tag));}
    public static void forgetChunk(ServerPlayer player,ServerLevel level,ChunkPos chunk){var tag=new CompoundTag();tag.putLong("chunk",chunk.toLong());PacketDistributor.sendToPlayer(player,new Sync(level.dimension().location(),tag));}
    public static void menu(ServerPlayer player,int id,CompoundTag tag){PacketDistributor.sendToPlayer(player,new MenuSync(id,tag));}
    private SealPayloads(){}
}
