package thaumcraft.port.golem.seal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.port.golem.task.TaskHandler;
import thaumcraft.port.research.ResearchStorage;

/** Dimension-local persistent seals; unloaded chunks never generate tasks. */
@EventBusSubscriber(modid="thaumcraft")
public final class SealHandler {
    public static final LinkedHashMap<String,ISeal> types=new LinkedHashMap<>();
    private static final Map<String,Supplier<? extends ISeal>> factories=new LinkedHashMap<>();
    private static final Map<Level,Map<SealPos,SealEntity>> CLIENT=Collections.synchronizedMap(new WeakHashMap<>());
    public static void registerSeal(ISeal seal){registerSeal(seal.getKey(),()->{try{return seal.getClass().getDeclaredConstructor().newInstance();}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}});}
    public static void registerSeal(String key,Supplier<? extends ISeal> factory){if(factories.containsKey(key))throw new IllegalArgumentException("Duplicate seal "+key);factories.put(key,factory);types.put(key,factory.get());}
    public static String[] getRegisteredSeals(){return types.keySet().toArray(String[]::new);}
    public static ISeal getSeal(String key){return types.get(key);}
    public static ISeal createSeal(String key){var factory=factories.get(key);return factory==null?null:factory.get();}
    public static Map<SealPos,SealEntity> getSeals(Level level){return level instanceof ServerLevel server?data(server).seals:CLIENT.computeIfAbsent(level,ignored->new ConcurrentHashMap<>());}
    public static SealEntity getSealEntity(Level level,SealPos pos){return level==null||pos==null?null:getSeals(level).get(pos);}
    public static CopyOnWriteArrayList<SealEntity> getSealsInRange(Level level,BlockPos source,double range){var out=new CopyOnWriteArrayList<SealEntity>();for(var seal:getSeals(level).values())if(seal.getSeal()!=null&&seal.getSealPos().pos.distSqr(source)<=range*range)out.add(seal);return out;}
    public static CopyOnWriteArrayList<SealEntity> getSealsInChunk(Level level,ChunkPos chunk){var out=new CopyOnWriteArrayList<SealEntity>();for(var seal:getSeals(level).values())if(new ChunkPos(seal.getSealPos().pos).equals(chunk))out.add(seal);return out;}
    public static boolean addSealEntity(Level level,BlockPos pos,Direction face,ISeal seal,Player player){
        if(level.isClientSide||seal==null||!level.hasChunkAt(pos)||!seal.canPlaceAt(level,pos,face)||!level.mayInteract(player,pos))return false;
        var entity=new SealEntity(level,new SealPos(pos,face),seal);entity.setOwner(player.getUUID().toString());return addSealEntity(level,entity);
    }
    public static boolean addSealEntity(Level level,SealEntity seal){
        if(level==null||seal==null||seal.getSeal()==null||getSeals(level).putIfAbsent(seal.getSealPos(),seal)!=null)return false;
        changed(level,seal);return true;
    }
    public static void changed(Level level,SealEntity seal){if(level instanceof ServerLevel server){data(server).setDirty();SealPayloads.send(server,seal);}}
    public static void markChunkAsDirty(Level level,BlockPos pos){if(level instanceof ServerLevel server)data(server).setDirty();}
    public static void removeSealEntity(Level level,SealPos pos,boolean quiet){
        var seal=getSeals(level).remove(pos);if(seal==null)return;
        if(level instanceof ServerLevel server){
            seal.getSeal().onRemoval(level,pos.pos,pos.face);
            if(!quiet){var item=SealPlacerItem.getSealStack(seal.getSeal().getKey());if(!item.isEmpty())server.addFreshEntity(new ItemEntity(level,pos.pos.getX()+.5+pos.face.getStepX()/1.7,pos.pos.getY()+.5+pos.face.getStepY()/1.7,pos.pos.getZ()+.5+pos.face.getStepZ()/1.7,item));}
            for(var task:TaskHandler.getTasks(level).values())if(pos.equals(task.getSealPos()))task.setSuspended(true);
            data(server).setDirty();SealPayloads.remove(server,pos);
        }
    }
    public static boolean canConfigure(Player player,SealEntity seal){return seal!=null;}
    public static boolean interact(ServerPlayer player,SealPos pos,boolean remove){
        var level=player.serverLevel();var seal=getSealEntity(level,pos);
        if(!level.hasChunkAt(pos.pos)||player.distanceToSqr(pos.pos.getCenter())>64||!canConfigure(player,seal)||!level.mayInteract(player,pos.pos))return false;
        if(remove)removeSealEntity(level,pos,false);
        else player.openMenu(new SimpleMenuProvider((id,inv,p)->new SealMenu(id,inv,seal),Component.translatable("item.thaumcraft.seal_blank")),buf->buf.writeNbt(seal.writeNBT(level.registryAccess())));
        return true;
    }
    public static void openLogistics(ServerPlayer player,BlockPos pos){openLogistics(player,pos,pos==null?null:Direction.UP);}
    public static void openLogistics(ServerPlayer player,BlockPos pos,Direction side){
        if(!ResearchStorage.get(player.getServer()).player(player.getUUID()).isResearchKnown("GOLEMLOGISTICS")||pos!=null&&player.distanceToSqr(pos.getCenter())>64)return;
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new LogisticsMenu(id,inv,pos,side),Component.translatable("research.GOLEMLOGISTICS.title")),buf->{buf.writeBoolean(pos!=null);if(pos!=null)buf.writeBlockPos(pos);buf.writeByte(side==null?0:side.ordinal());});
    }
    public static void tickSealEntities(Level level){
        if(!(level instanceof ServerLevel))return;
        for(var seal:new ArrayList<>(getSeals(level).values())){
            var pos=seal.getSealPos();if(!level.hasChunkAt(pos.pos))continue;
            if(level.getGameTime()%20==0&&!seal.getSeal().canPlaceAt(level,pos.pos,pos.face)){removeSealEntity(level,pos,false);continue;}
            seal.tickSealEntity(level);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(LevelTickEvent.Post event){tickSealEntities(event.getLevel());}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){CLIENT.remove(event.getLevel());}
    @SubscribeEvent public static void watch(ChunkWatchEvent.Watch event){SealPayloads.sendChunk(event.getPlayer(),event.getLevel(),event.getPos());}
    @SubscribeEvent public static void unwatch(ChunkWatchEvent.UnWatch event){SealPayloads.forgetChunk(event.getPlayer(),event.getLevel(),event.getPos());}
    public static void acceptClient(Level level,CompoundTag tag){
        if(level==null||!level.isClientSide)return;
        if(tag.contains("chunk")){long chunk=tag.getLong("chunk");getSeals(level).keySet().removeIf(p->new ChunkPos(p.pos).toLong()==chunk);}
        for(var value:tag.getList("seals",Tag.TAG_COMPOUND)){try{var seal=new SealEntity();seal.readNBT((CompoundTag)value,level.registryAccess());getSeals(level).put(seal.getSealPos(),seal);}catch(IllegalArgumentException ignored){}}
        if(tag.contains("remove")){var pos=BlockPos.of(tag.getLong("remove"));int face=tag.getByte("face");if(face>=0&&face<6)getSeals(level).remove(new SealPos(pos,Direction.values()[face]));}
    }
    private static Data data(ServerLevel level){return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new,Data::load,null),"thaumcraft_golem_seals");}
    private static final class Data extends SavedData {
        final Map<SealPos,SealEntity> seals=new ConcurrentHashMap<>();
        static Data load(CompoundTag tag,HolderLookup.Provider registries){var data=new Data();for(var value:tag.getList("seals",Tag.TAG_COMPOUND)){try{var seal=new SealEntity();seal.readNBT((CompoundTag)value,registries);data.seals.put(seal.getSealPos(),seal);}catch(IllegalArgumentException ignored){}}return data;}
        @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();seals.values().forEach(seal->list.add(seal.writeNBT(registries)));tag.put("seals",list);return tag;}
    }
    private SealHandler(){}
}
