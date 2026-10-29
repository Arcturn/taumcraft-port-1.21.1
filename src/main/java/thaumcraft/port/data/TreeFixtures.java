package thaumcraft.port.data;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.world.MagicTrees;
import thaumcraft.port.world.WorldContent;

/** Actual tree algorithms against a bounded in-memory block volume, not a running server. */
final class TreeFixtures implements InvocationHandler {
    private final Map<BlockPos,BlockState> blocks=new HashMap<>();
    private int writes;
    private final WorldGenLevel level=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},this);
    private BlockState state(BlockPos p){return blocks.getOrDefault(p,p.getY()==0?Blocks.GRASS_BLOCK.defaultBlockState():p.getY()<0?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState());}
    private boolean inside(BlockPos p){return p.getY()>=-64&&p.getY()<320&&Math.abs(p.getX())<=48&&Math.abs(p.getZ())<=48;}
    @Override public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{
        return switch(method.getName()){
            case "getBlockState"->state((BlockPos)args[0]);
            case "getFluidState"->state((BlockPos)args[0]).getFluidState();
            case "isEmptyBlock"->state((BlockPos)args[0]).isAir();
            case "getMinBuildHeight"->-64;
            case "getMaxBuildHeight"->320;
            case "getHeight"->args==null||args.length==0?384:1;
            case "isOutsideBuildHeight"->args[0] instanceof BlockPos p?(p.getY()<-64||p.getY()>=320):((int)args[0]<-64||(int)args[0]>=320);
            case "ensureCanWrite"->inside((BlockPos)args[0]);
            case "hasChunk","hasChunkAt","hasChunksAt"->true;
            case "setBlock"->{var p=((BlockPos)args[0]).immutable();if(!inside(p))throw new AssertionError("Tree wrote outside fixture: "+p);blocks.put(p,(BlockState)args[1]);writes++;yield true;}
            case "getBlockEntity"->null;
            case "getSeed"->0L;
            case "toString"->"Bounded tree fixture";
            default->{if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new UnsupportedOperationException(method.toString());}
        };
    }
    private static boolean grow(int kind,TreeFixtures fixture,long seed){
        var random=RandomSource.create(seed);var origin=new BlockPos(0,1,0);
        return switch(kind){case 0->MagicTrees.greatwood(fixture.level,random,origin,false);case 1->MagicTrees.silverwood(fixture.level,random,origin,true,true);default->MagicTrees.bigMagic(fixture.level,random,origin);};
    }
    static int validate(ResourceManager resources){
        var tags=new TagLoader<Holder<Block>>(id->BuiltInRegistries.BLOCK.getHolder(id),"tags/block").loadAndBuild(resources);
        Map<TagKey<Block>,List<Holder<Block>>> bound=new HashMap<>();tags.forEach((id,values)->bound.put(TagKey.create(Registries.BLOCK,id),List.copyOf(values)));BuiltInRegistries.BLOCK.bindTags(bound);
        int checks=0;
        for(int kind=0;kind<3;kind++)for(long seed=0;seed<20;seed++){
            var first=new TreeFixtures();var repeat=new TreeFixtures();
            if(!grow(kind,first,seed)||!grow(kind,repeat,seed))throw new AssertionError("Tree failed on valid soil: "+kind+"/"+seed);
            if(!first.blocks.equals(repeat.blocks))throw new AssertionError("Nondeterministic tree: "+kind+"/"+seed);
            long logs=first.blocks.values().stream().filter(s->s.is(BlockTags.LOGS)).count();long leaves=first.blocks.values().stream().filter(s->s.is(BlockTags.LEAVES)).count();
            if(logs<6||leaves<20)throw new AssertionError("Incomplete tree: "+kind+"/"+seed+" logs="+logs+" leaves="+leaves);
            if(first.blocks.values().stream().filter(s->s.is(BlockTags.LEAVES)).noneMatch(s->s.getValue(LeavesBlock.DISTANCE)<7))throw new AssertionError("Leaves have no supporting logs");
            checks++;
        }
        for(int kind=0;kind<3;kind++){
            var blocked=new TreeFixtures();blocked.blocks.put(new BlockPos(0,1,0),Blocks.BEDROCK.defaultBlockState());
            if(grow(kind,blocked,10)||blocked.writes!=0)throw new AssertionError("Failed tree mutated obstructed terrain: "+kind);checks++;
            var wrongSoil=new TreeFixtures();wrongSoil.blocks.put(BlockPos.ZERO,Blocks.STONE.defaultBlockState());
            if(grow(kind,wrongSoil,10)||wrongSoil.writes!=0)throw new AssertionError("Tree grew on invalid soil: "+kind);checks++;
        }
        // TC6 checkBlockLine rejects foreign foliage and plants even when vanilla
        // considers them replaceable. Every column of the 2x2 trunk is checked.
        for(Block obstacle:List.of(Blocks.OAK_LEAVES,Blocks.BIRCH_LEAVES,Blocks.SHORT_GRASS)){
            for(int x=0;x<2;x++)for(int z=0;z<2;z++){
                var blocked=new TreeFixtures();var pos=new BlockPos(x,4,z);
                blocked.blocks.put(pos,obstacle.defaultBlockState());
                var before=new HashMap<>(blocked.blocks);
                if(grow(0,blocked,10)||blocked.writes!=0||!blocked.blocks.equals(before))
                    throw new AssertionError("Greatwood grew through a low obstruction: "+obstacle+" at "+pos);
                checks++;
            }
        }
        for(int x=0;x<2;x++)for(int z=0;z<2;z++){
            var ownLeaves=new TreeFixtures();
            ownLeaves.blocks.put(new BlockPos(x,4,z),WorldContent.block("leaves_greatwood").defaultBlockState());
            if(!grow(0,ownLeaves,10))throw new AssertionError("Greatwood rejected its own foliage");
            checks++;
        }
        // Obstructions six or more blocks above the base shorten the first
        // crown rather than rejecting the location (TC6 validTreeLocation).
        for(Block obstacle:List.of(Blocks.OAK_LEAVES,Blocks.BIRCH_LEAVES))for(long seed=0;seed<2;seed++){
            var clear=new TreeFixtures();var shortened=new TreeFixtures();
            shortened.blocks.put(new BlockPos(0,7,0),obstacle.defaultBlockState());
            if(!grow(0,clear,seed)||!grow(0,shortened,seed))throw new AssertionError("High foliage rejected a valid greatwood");
            int clearTop=highestLog(clear),shortTop=highestLog(shortened);
            if(shortTop>=clearTop)throw new AssertionError("High foliage did not shorten greatwood: "+shortTop+" >= "+clearTop);
            checks++;
        }
        // The strict greatwood predicate must not change silverwoods or the
        // vanilla-leaf magic oaks which share parts of this implementation.
        for(int kind:new int[]{1,2})for(Block obstacle:List.of(Blocks.OAK_LEAVES,Blocks.BIRCH_LEAVES)){
            var fixture=new TreeFixtures();fixture.blocks.put(new BlockPos(0,4,0),obstacle.defaultBlockState());
            if(!grow(kind,fixture,10))throw new AssertionError("Unrelated tree rejected replaceable foliage: "+kind);
            checks++;
        }
        return checks;
    }
    private static int highestLog(TreeFixtures fixture){
        return fixture.blocks.entrySet().stream().filter(e->e.getValue().is(BlockTags.LOGS))
                .mapToInt(e->e.getKey().getY()).max().orElseThrow();
    }
}
