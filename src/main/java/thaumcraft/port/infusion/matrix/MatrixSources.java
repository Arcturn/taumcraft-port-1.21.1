package thaumcraft.port.infusion.matrix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.essentia.EssentiaSource;
import thaumcraft.port.essentia.RemoteEssentia;

/** EssentiaHandler's null-direction, range-12 source search (including its 200-tick retry). */
public final class MatrixSources {
    private List<BlockPos> sources;
    private long retryAt;
    public RemoteEssentia.Target providing(Level level,BlockPos origin,Aspect aspect){
        for(BlockPos pos:candidates(level,origin)){
            EssentiaSource source=source(level,pos);
            if(source==null){failed(level);return null;}
            if(!source.blocked()&&source.takeRemote(aspect,1,true)==1)return new RemoteEssentia.Target(pos,source);
        }
        failed(level);return null;
    }
    private List<BlockPos> candidates(Level level,BlockPos origin){
        if(sources!=null)return sources;
        if(level.getGameTime()<retryAt)return List.of();
        List<BlockPos> found=new ArrayList<>();
        for(int x=(origin.getX()-12)>>4;x<=(origin.getX()+12)>>4;x++)for(int z=(origin.getZ()-12)>>4;z<=(origin.getZ()+12)>>4;z++){
            var chunk=level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
            if(!(chunk instanceof LevelChunk loaded))continue;
            for(BlockPos pos:List.copyOf(loaded.getBlockEntitiesPos()))if(inRange(origin,pos)&&source(level,pos)!=null)found.add(pos.immutable());
        }
        found.sort(Comparator.<BlockPos>comparingDouble(origin::distSqr).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getY));
        return sources=List.copyOf(found);
    }
    private void failed(Level level){if(sources!=null){sources=null;retryAt=level.getGameTime()+200;}}
    private static EssentiaSource source(Level level,BlockPos pos){return level.hasChunkAt(pos)?EssentiaContent.SOURCE.getCapability(level,pos,null,null,null):null;}
    public static boolean inRange(BlockPos origin,BlockPos pos){return !origin.equals(pos)&&Math.abs(pos.getX()-origin.getX())<=12&&Math.abs(pos.getZ()-origin.getZ())<=12&&pos.getY()-origin.getY()>=-12&&pos.getY()-origin.getY()<12;}
}
