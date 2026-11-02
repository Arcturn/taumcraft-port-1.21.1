package thaumcraft.port.essentia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import thaumcraft.port.aspect.Aspect;

/** BETA26 EssentiaHandler search order and retry rules, without loading absent chunks. */
public final class RemoteEssentia {
    public static final int RANGE=16,RETRY_TICKS=200;
    private List<BlockPos> sources;
    private long retryAt;
    private Direction cachedFacing;
    private final int range;
    private final java.util.function.Predicate<EssentiaSource> allowed;
    public RemoteEssentia(){this(RANGE,source->true);}
    public RemoteEssentia(int range,java.util.function.Predicate<EssentiaSource> allowed){if(range<1||range>RANGE)throw new IllegalArgumentException("range");this.range=range;this.allowed=java.util.Objects.requireNonNull(allowed);}
    public record Target(BlockPos pos,EssentiaSource source){}

    public Target accepting(Level level,BlockPos origin,Direction facing,Aspect aspect){
        Target empty=null;
        for(BlockPos pos:candidates(level,origin,facing)){
            EssentiaSource source=source(level,pos);
            if(source==null){failed(level);return null;}
            if(source.blocked()||source.addRemote(aspect,1,true)!=1)continue;
            if(source.storedAmount()>0)return new Target(pos,source);
            if(empty==null)empty=new Target(pos,source);
        }
        if(empty==null)failed(level);
        return empty;
    }
    public Target providing(Level level,BlockPos origin,Direction facing,Aspect aspect){
        for(BlockPos pos:candidates(level,origin,facing)){
            EssentiaSource source=source(level,pos);
            if(source==null){failed(level);return null;}
            if(!source.blocked()&&source.takeRemote(aspect,1,true)==1)return new Target(pos,source);
        }
        failed(level);return null;
    }
    private List<BlockPos> candidates(Level level,BlockPos origin,Direction facing){
        if(cachedFacing!=facing){sources=null;retryAt=0;cachedFacing=facing;}
        if(sources!=null)return sources;
        if(level.getGameTime()<retryAt)return List.of();
        List<BlockPos> found=new ArrayList<>();
        for(int cx=(origin.getX()-range)>>4;cx<=(origin.getX()+range)>>4;cx++)
            for(int cz=(origin.getZ()-range)>>4;cz<=(origin.getZ()+range)>>4;cz++){
                var chunk=level.getChunkSource().getChunk(cx,cz,ChunkStatus.FULL,false);
                if(!(chunk instanceof LevelChunk loaded))continue;
                // Include saved, not-yet-materialized block entities in an already loaded chunk.
                // The copied position set also stays stable if a capability lookup materializes one.
                for(BlockPos pos:List.copyOf(loaded.getBlockEntitiesPos()))
                    if(inRange(origin,pos,facing,range)&&source(level,pos)!=null)found.add(pos.immutable());
            }
        found.sort(Comparator.<BlockPos>comparingDouble(origin::distSqr)
                .thenComparingInt(p->axisA(origin,p,facing)).thenComparingInt(p->axisB(origin,p,facing))
                .thenComparingInt(p->depth(origin,p,facing)));
        sources=List.copyOf(found);
        return sources;
    }
    private void failed(Level level){
        // Failed probes while waiting must not slide the retry deadline forward forever.
        if(sources!=null){sources=null;retryAt=level.getGameTime()+RETRY_TICKS;}
    }
    private EssentiaSource source(Level level,BlockPos pos){var source=level.hasChunkAt(pos)?EssentiaContent.SOURCE.getCapability(level,pos,null,null,null):null;return source!=null&&allowed.test(source)?source:null;}
    public static boolean inRange(BlockPos origin,BlockPos pos,Direction facing){
        return inRange(origin,pos,facing,RANGE);
    }
    public static boolean inRange(BlockPos origin,BlockPos pos,Direction facing,int range){
        int forward=depth(origin,pos,facing);
        return !origin.equals(pos)&&forward>=0&&forward<range&&Math.abs(axisA(origin,pos,facing))<=range&&Math.abs(axisB(origin,pos,facing))<=range;
    }
    private static int depth(BlockPos o,BlockPos p,Direction f){return (p.getX()-o.getX())*f.getStepX()+(p.getY()-o.getY())*f.getStepY()+(p.getZ()-o.getZ())*f.getStepZ();}
    private static int axisA(BlockPos o,BlockPos p,Direction f){return f.getAxis()==Direction.Axis.X?p.getY()-o.getY():p.getX()-o.getX();}
    private static int axisB(BlockPos o,BlockPos p,Direction f){return f.getAxis()==Direction.Axis.Z?p.getY()-o.getY():p.getZ()-o.getZ();}
}
