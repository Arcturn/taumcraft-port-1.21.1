package thaumcraft.port.world;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Azanor's greatwood (two branching crowns) and silverwood algorithms, adapted to dynamic world height. */
public final class MagicTrees {
    private final WorldGenLevel world;
    private final RandomSource random;
    private final Block log, leaves;
    private boolean strictGreatwoodLines;
    private int trunk=2;private double attenuation=.618,slope=.38;
    private final Set<BlockPos> logs=new HashSet<>(), foliage=new HashSet<>();
    private MagicTrees(WorldGenLevel w,RandomSource r,boolean silver){this(w,r,WorldContent.block("log_"+(silver?"silverwood":"greatwood")),WorldContent.block("leaves_"+(silver?"silverwood":"greatwood")));}
    private MagicTrees(WorldGenLevel w,RandomSource r,Block log,Block leaves){world=w;random=r;this.log=log;this.leaves=leaves;}
    public static boolean bigMagic(WorldGenLevel w,RandomSource r,BlockPos p){
        MagicTrees tree=new MagicTrees(w,RandomSource.create(r.nextLong()),Blocks.OAK_LOG,Blocks.OAK_LEAVES);
        tree.trunk=1;tree.attenuation=.6618;tree.slope=.381;tree.width=1.25;
        return tree.great(p,r,false);
    }
    private boolean replaceable(BlockPos p){var s=world.getBlockState(p);return s.isAir()||s.is(BlockTags.LEAVES)||s.is(BlockTags.REPLACEABLE_BY_TREES)||s.is(BlockTags.SAPLINGS);}
    private boolean soil(BlockPos p){return world.getBlockState(p).is(BlockTags.DIRT)||world.getBlockState(p).is(Blocks.FARMLAND);}
    private void put(BlockPos p,BlockState s){
        if(world.isOutsideBuildHeight(p)||!world.ensureCanWrite(p)||!replaceable(p))return;
        world.setBlock(p,s,19);
        if(s.is(log))logs.add(p.immutable());else if(s.is(leaves))foliage.add(p.immutable());
    }
    private void log(BlockPos p,Direction.Axis axis){put(p,log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis));}
    private void leaf(BlockPos p){put(p,leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE,7));}
    private void finish(){
        // Vanilla distance propagation, needed because worldgen suppresses neighbour notifications.
        Set<BlockPos> frontier=new HashSet<>(logs);Set<BlockPos> visited=new HashSet<>(logs);
        for(int distance=1;distance<=6;distance++){
            Set<BlockPos> next=new HashSet<>();
            for(var p:frontier)for(var d:Direction.values()){
                var q=p.relative(d);if(!foliage.contains(q)||!visited.add(q))continue;
                if(world.getBlockState(q).is(leaves))world.setBlock(q,world.getBlockState(q).setValue(LeavesBlock.DISTANCE,distance),19);
                next.add(q);
            }
            frontier=next;
        }
    }
    public static boolean silverwood(WorldGenLevel w,RandomSource r,BlockPos origin,boolean natural,boolean forest){
        return new MagicTrees(w,r,true).silver(origin,natural,forest);
    }
    private boolean silver(BlockPos p,boolean natural,boolean forest){
        int height=(forest?8:7)+random.nextInt(forest?5:4);
        if(p.getY()<world.getMinBuildHeight()+1||p.getY()+height+5>=world.getMaxBuildHeight()||!soil(p.below()))return false;
        for(int y=0;y<=height+1;y++){
            int radius=y==0?0:y>=height-1?3:1;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(!replaceable(p.offset(x,y,z)))return false;
        }
        world.setBlock(p.below(),Blocks.DIRT.defaultBlockState(),19);
        int end=height+3+random.nextInt(3);
        for(int y=height-5;y<=end;y++)for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            int dy=y-Math.clamp(y,height-3,height);
            if(x*x+dy*dy+z*z<10+random.nextInt(8))leaf(p.offset(x,y,z));
        }
        for(int y=0;y<height;y++){
            log(p.above(y),Direction.Axis.Y);
            for(var d:Direction.Plane.HORIZONTAL)log(p.above(y).relative(d),Direction.Axis.Y);
        }
        log(p.above(height),Direction.Axis.Y);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            log(p.offset(x,0,z),Direction.Axis.Y);
            if(random.nextInt(3)!=0)log(p.offset(x,1,z),Direction.Axis.Y);
            log(p.offset(x,height-4,z),Direction.Axis.Y);
            if(random.nextInt(3)==0)log(p.offset(x,height-5,z),Direction.Axis.Y);
        }
        for(var d:Direction.Plane.HORIZONTAL){
            log(p.relative(d,2),d.getAxis());
            log(p.relative(d,2).below(),Direction.Axis.Y);
            log(p.relative(d,2).above(height-4),d.getAxis());
        }
        finish();
        if(natural)VegetationFeature.flowers(world,random,p,WorldContent.block("shimmerleaf"),64);
        return true;
    }
    public static boolean greatwood(WorldGenLevel w,RandomSource r,BlockPos p,boolean spiders){
        MagicTrees tree=new MagicTrees(w,RandomSource.create(r.nextLong()),false);
        tree.strictGreatwoodLines=true;
        return tree.great(p,r,spiders);
    }
    private int limit,height;
    private BlockPos base;
    private double width=1.2;
    private final List<int[]> nodes=new ArrayList<>();
    private boolean great(BlockPos p,RandomSource outer,boolean spiders){
        base=p;limit=11+random.nextInt(11);
        // Both crowns together can reach almost twice the first crown's height.
        if(p.getY()<world.getMinBuildHeight()+2||p.getY()+limit*2+5>=world.getMaxBuildHeight())return false;
        for(int x=0;x<trunk;x++)for(int z=0;z<trunk;z++){
            if(!soil(p.offset(x,-1,z)))return false;
            int blocked=checkLine(p.offset(x,0,z),p.offset(x,limit-1,z));
            if(blocked>=0){if(blocked<6)return false;limit=blocked;}
        }
        crown();if(trunk==2){width=1.66;base=p.above(height);crown();}finish();
        if(spiders){
            world.setBlock(p.below(),Blocks.SPAWNER.defaultBlockState(),19);
            if(world.getBlockEntity(p.below()) instanceof SpawnerBlockEntity spawner)spawner.setEntityId(EntityType.CAVE_SPIDER,random);
            for(int i=0;i<50;i++){
                var q=p.offset(outer.nextInt(14)-7,outer.nextInt(10),outer.nextInt(14)-7);
                if(!world.isEmptyBlock(q))continue;
                for(var d:Direction.values())if(world.getBlockState(q.relative(d)).is(log)||world.getBlockState(q.relative(d)).is(leaves)){
                    world.setBlock(q,Blocks.COBWEB.defaultBlockState(),19);break;
                }
            }
            var chest=p.below(2);world.setBlock(chest,Blocks.CHEST.defaultBlockState(),19);
            if(world.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity c)c.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON,random.nextLong());
        }
        return true;
    }
    private void crown(){
        height=(int)(limit*attenuation);if(height>=limit)height=limit-1;
        int tries=Math.max(1,(int)(1.382+Math.pow(.9*limit/13.0,2)));
        int y=base.getY()+limit-4,top=base.getY()+height,layer=y-base.getY();
        nodes.clear();nodes.add(new int[]{base.getX(),y--,base.getZ(),top});
        while(layer>=0){
            double radius=layerSize(layer);
            if(radius>=0)for(int i=0;i<tries;i++){
                double spread=width*radius*(random.nextFloat()+.328),angle=random.nextFloat()*2*Math.PI;
                int x=(int)Math.floor(spread*Math.sin(angle)+base.getX()+.5),z=(int)Math.floor(spread*Math.cos(angle)+base.getZ()+.5);
                var leaf=new BlockPos(x,y,z);if(checkLine(leaf,leaf.above(4))!=-1)continue;
                double branchSlope=Math.sqrt(Math.pow(base.getX()-x,2)+Math.pow(base.getZ()-z,2))*slope;
                int attach=y-branchSlope>top?top:(int)(y-branchSlope);
                if(checkLine(new BlockPos(base.getX(),attach,base.getZ()),leaf)!=-1)continue;
                nodes.add(new int[]{x,y,z,attach});
            }
            y--;layer--;
        }
        for(var n:nodes)for(int dy=0;dy<4;dy++){
            double radius=dy==0||dy==3?2:3;int max=(int)(radius+.618);
            for(int x=-max;x<=max;x++)for(int z=-max;z<=max;z++)
                if(Math.pow(Math.abs(x)+.5,2)+Math.pow(Math.abs(z)+.5,2)<=radius*radius)leaf(new BlockPos(n[0]+x,n[1]+dy,n[2]+z));
        }
        for(var n:nodes)if(n[3]-base.getY()>=limit*.2)line(new BlockPos(base.getX(),n[3],base.getZ()),new BlockPos(n[0],n[1],n[2]));
        for(int x=0;x<trunk;x++)for(int z=0;z<trunk;z++)line(base.offset(x,0,z),base.offset(x,height,z));
    }
    private double layerSize(int layer){
        if(layer<limit*.3)return -1.618;
        double half=limit/2.0,d=half-layer;
        return (d==0?half:Math.abs(d)>=half?0:Math.sqrt(half*half-d*d))*.5;
    }
    private static List<BlockPos> linePoints(BlockPos a,BlockPos b,boolean round){
        int dx=b.getX()-a.getX(),dy=b.getY()-a.getY(),dz=b.getZ()-a.getZ();int n=Math.max(Math.abs(dx),Math.max(Math.abs(dy),Math.abs(dz)));
        List<BlockPos> points=new ArrayList<>();if(n==0){points.add(a);return points;}
        double bias=round?.5:0;
        for(int i=0;i<=n;i++)points.add(new BlockPos((int)Math.floor(a.getX()+dx*(double)i/n+bias),(int)Math.floor(a.getY()+dy*(double)i/n+bias),(int)Math.floor(a.getZ()+dz*(double)i/n+bias)));
        return points;
    }
    private boolean lineReplaceable(BlockPos p){
        // TC6 greatwood only accepts air and its own leaves along trunks/branches.
        // Big magic oaks use the broader vanilla replacement rule instead.
        if(!strictGreatwoodLines)return replaceable(p);
        var state=world.getBlockState(p);
        return state.isAir()||state.is(leaves);
    }
    private int checkLine(BlockPos a,BlockPos b){int i=0;for(var p:linePoints(a,b,false)){if(!lineReplaceable(p))return i;i++;}return -1;}
    private void line(BlockPos a,BlockPos b){for(var p:linePoints(a,b,true)){
        int x=Math.abs(p.getX()-a.getX()),z=Math.abs(p.getZ()-a.getZ());
        log(p,Math.max(x,z)>0?(x>=z?Direction.Axis.X:Direction.Axis.Z):Direction.Axis.Y);
    }}
}
