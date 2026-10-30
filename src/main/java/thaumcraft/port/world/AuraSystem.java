package thaumcraft.port.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Persistent per-chunk aura. Only server-thread ticks, no forced chunk loads. */
public final class AuraSystem {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,"thaumcraft");
    public static final Supplier<AttachmentType<AuraState>> AURA=TYPES.register("aura",()->AttachmentType.builder(AuraState::new).serialize(AuraCodec.CODEC).build());
    private static final Map<ServerLevel,Set<Long>> LOADED=new ConcurrentHashMap<>();
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        NeoForge.EVENT_BUS.addListener(AuraSystem::load);
        NeoForge.EVENT_BUS.addListener(AuraSystem::unload);
        NeoForge.EVENT_BUS.addListener(AuraSystem::close);
        NeoForge.EVENT_BUS.addListener(AuraSystem::tick);
    }
    private static void load(ChunkEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel l && e.getChunk() instanceof LevelChunk)
            LOADED.computeIfAbsent(l,k->ConcurrentHashMap.newKeySet()).add(e.getChunk().getPos().toLong());
    }
    private static void unload(ChunkEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel l) { var set=LOADED.get(l); if(set!=null)set.remove(e.getChunk().getPos().toLong()); }
    }
    private static void close(LevelEvent.Unload e) { if(e.getLevel() instanceof ServerLevel l)LOADED.remove(l); }
    public static AuraState get(ServerLevel level, BlockPos pos) {
        LevelChunk chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null ? null : get(level,chunk);
    }
    private static AuraState get(ServerLevel level, LevelChunk chunk) {
        var holder=(IAttachmentHolder)chunk;
        AuraState aura=holder.getData(AURA);
        if(aura.base()<0) {
            int x=chunk.getPos().getMiddleBlockX(),z=chunk.getPos().getMiddleBlockZ();
            var source=level.getChunkSource();
            double life=0;
            for(int[] d:new int[][]{{0,0},{16,0},{-16,0},{0,16},{0,-16}})
                life+=BiomeAffinity.of(source.getGenerator().getBiomeSource().getNoiseBiome((x+d[0])>>2,12,(z+d[1])>>2,source.randomState().sampler())).life();
            aura=AuraState.generate(level.getSeed(),chunk.getPos().x,chunk.getPos().z,life/5);
            holder.setData(AURA,aura); chunk.setUnsaved(true);
        }
        return aura;
    }
    public static void dirty(ServerLevel level, BlockPos pos) {
        var c=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4); if(c!=null)c.setUnsaved(true);
    }
    public static void add(ServerLevel l,BlockPos p,float amount,boolean flux) {
        if(!WorldConfig.AURA.get())return;
        var a=get(l,p); if(a==null)return;
        if(flux)a.addFlux(amount);else a.addVis(amount); dirty(l,p);
    }
    /** TC6 pollution optionally emits smoke independently of the stored flux amount. */
    public static void pollute(ServerLevel level,BlockPos pos,float amount,boolean showEffect) {
        if(!(amount>0))return;
        add(level,pos,amount,true);
        if(showEffect)thaumcraft.port.network.PollutionFxPayload.send(level,pos,amount);
    }
    public static float drain(ServerLevel l,BlockPos p,float amount,boolean flux,boolean simulate) {
        if(!WorldConfig.AURA.get())return 0;
        var a=get(l,p); if(a==null)return 0;
        float n=a.drain(flux,amount,simulate);if(!simulate&&n>0)dirty(l,p);return n;
    }
    private static void tick(LevelTickEvent.Post e) {
        if(!(e.getLevel() instanceof ServerLevel l)||!WorldConfig.AURA.get()||l.getGameTime()%20!=0)return;
        var positions=LOADED.get(l);if(positions==null)return;
        // Sorting avoids hash-map iteration making diffusion order depend on unload/reload history.
        long[] sorted=positions.stream().mapToLong(Long::longValue).sorted().toArray();
        for(long key:sorted) {
            var p=new ChunkPos(key);var chunk=l.getChunkSource().getChunkNow(p.x,p.z);if(chunk==null)continue;
            var a=get(l,chunk);var neighbours=new ArrayList<AuraState>(4);var chunks=new ArrayList<LevelChunk>(5);chunks.add(chunk);
            var dirs=new ArrayList<>(List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST));
            Collections.shuffle(dirs,new Random(l.getSeed()^key^l.getGameTime()));
            for(var d:dirs){var c=l.getChunkSource().getChunkNow(p.x+d.getStepX(),p.z+d.getStepZ());if(c!=null){neighbours.add(get(l,c));chunks.add(c);}}
            var states=new ArrayList<AuraState>();states.add(a);states.addAll(neighbours);
            float[] before=new float[states.size()*2];for(int i=0;i<states.size();i++){before[i*2]=states.get(i).vis();before[i*2+1]=states.get(i).flux();}
            boolean rift=a.step(neighbours,l.getMoonPhase(),l.random.nextFloat(),l.random.nextFloat());
            for(int i=0;i<states.size();i++)if(states.get(i).vis()!=before[i*2]||states.get(i).flux()!=before[i*2+1])chunks.get(i).setUnsaved(true);
            if(rift&&WorldConfig.RIFTS.get()) FluxRift.tryCreate(l,p);
        }
    }
    private AuraSystem(){}
}
