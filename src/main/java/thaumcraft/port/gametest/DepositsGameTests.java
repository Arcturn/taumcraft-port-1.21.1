package thaumcraft.port.gametest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.world.CrystalClusterBlock;
import thaumcraft.port.world.WorldConfig;
import thaumcraft.port.world.WorldContent;

/** Actual registered deposits on bounded host-rock and cave fixtures; not a natural-world survey. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class DepositsGameTests {
    private static final ResourceKey<PlacedFeature> DEPOSITS=ResourceKey.create(Registries.PLACED_FEATURE,WorldContent.id("deposits"));

    @GameTest(template="empty",timeoutTicks=200)
    public static void depthAndDensityPreserveOriginalUpperOreAttempts(GameTestHelper helper){
        var saved=new Config();
        try{
            WorldConfig.ORES.set(true);WorldConfig.CRYSTALS.set(false);
            int[][] cases={{1,0,0,0,0,0},{50,9,11,9,9,10},{100,18,23,18,18,20},{200,36,45,36,36,40}};
            for(var row:cases){
                WorldConfig.DENSITY.set(row[0]);
                var plot=new Plot(helper.getLevel(),-64,320,200,Host.STONE);
                run(plot,RandomSource.create(0));
                int expected=row[1]+row[2]+row[3]+row[4]+row[5];
                helper.assertValueEqual(plot.candidates.size(),expected,"Wrong total ore candidates at density "+row[0]);
                int cursor=0;
                cursor=checkBand(helper,plot,cursor,row[1],0,51);
                cursor=checkBand(helper,plot,cursor,row[2],-64,0);
                cursor=checkBand(helper,plot,cursor,row[3],0,64);
                cursor=checkBand(helper,plot,cursor,row[4],-64,0);
                checkBand(helper,plot,cursor,row[5],176,201);
                helper.assertTrue(plot.blocks.values().stream().noneMatch(s->id(s).startsWith("deepslate_ore_")),"Negative Y selected deepslate ore in a stone host");
                System.out.println("DEPOSIT_ORE_CANDIDATES density="+row[0]+" upper_cinnabar="+row[1]+" lower_cinnabar="+row[2]+" upper_quartz="+row[3]+" lower_quartz="+row[4]+" amber="+row[5]+" total="+expected);
            }
            WorldConfig.DENSITY.set(100);
            var oldHeight=new Plot(helper.getLevel(),0,256,200,Host.STONE);
            run(oldHeight,RandomSource.create(0));
            helper.assertValueEqual(oldHeight.candidates.size(),56,"A height-zero world received modern-depth extra ores");
            helper.assertTrue(oldHeight.candidates.stream().allMatch(p->p.getY()>=0),"Legacy-height world received negative ore coordinates");
            helper.succeed();
        }finally{saved.restore();}
    }

    @GameTest(template="empty",timeoutTicks=200)
    public static void oreVariantUsesHostRockAndProtectsNonOreHosts(GameTestHelper helper){
        var saved=new Config();
        try{
            WorldConfig.ORES.set(true);WorldConfig.CRYSTALS.set(false);WorldConfig.DENSITY.set(100);
            var deep=new Plot(helper.getLevel(),-64,320,200,Host.DEEPSLATE);
            run(deep,RandomSource.create(4));
            helper.assertTrue(!deep.blocks.isEmpty(),"Solid deepslate received no ores");
            helper.assertTrue(deep.blocks.values().stream().allMatch(s->id(s).startsWith("deepslate_ore_")),"Deepslate above Y=0 generated an ordinary ore variant");
            for(String kind:List.of("amber","cinnabar","quartz"))
                helper.assertTrue(deep.blocks.values().stream().anyMatch(s->id(s).equals("deepslate_ore_"+kind)),"Missing generated deepslate "+kind);
            for(Host host:new Host[]{Host.DIRT,Host.EXISTING_ORE}){
                var protectedPlot=new Plot(helper.getLevel(),-64,320,200,host);
                helper.assertFalse(run(protectedPlot,RandomSource.create(4)),"Generator replaced protected host "+host);
                helper.assertTrue(protectedPlot.blocks.isEmpty(),"Generator changed protected terrain "+host);
            }
            var lowSurface=new Plot(helper.getLevel(),-64,320,-40,Host.DEEPSLATE);
            run(lowSurface,RandomSource.create(4));
            long deepAmber=lowSurface.blocks.values().stream().filter(s->id(s).equals("deepslate_ore_amber")).count();
            helper.assertTrue(deepAmber>0,"Deepslate amber cannot generate when the near-surface band is deepslate");
            helper.assertTrue(lowSurface.blocks.entrySet().stream().filter(e->id(e.getValue()).contains("amber"))
                    .allMatch(e->e.getKey().getY()>=-64&&e.getKey().getY()<=-40),"Amber escaped its surface-relative band");
            helper.succeed();
        }finally{saved.restore();}
    }

    @GameTest(template="empty",timeoutTicks=200)
    public static void crystalsCountSizeAndFinishTheWholeLastCluster(GameTestHelper helper){
        var saved=new Config();
        try{
            WorldConfig.ORES.set(false);WorldConfig.CRYSTALS.set(true);
            for(int density:new int[]{44,100,200}){
                WorldConfig.DENSITY.set(density);
                var random=new ClusterRandom();
                var plot=new Plot(helper.getLevel(),-64,320,64,Host.CAVE);plot.clusterRandom=random;
                helper.assertTrue(run(plot,random),"Controlled cave received no crystals");
                int upperBudget=Math.round(64*(density/100.0f));
                int lowerBudget=Math.round(64*(density/100.0f)*(63f/59));
                checkFinishedBudget(helper,plot.upperClusters,upperBudget,"upper",density);
                checkFinishedBudget(helper,plot.lowerClusters,lowerBudget,"lower",density);
                if(density==44)helper.assertTrue(plot.upperClusters.size()>=2,"Budget equality stopped the pass before exceeding it");
                if(density==100){
                    helper.assertValueEqual(plot.upperClusters.size(),3,"SIZE+1 was counted instead of SIZE, or cluster was truncated");
                    helper.assertValueEqual(plot.lowerClusters.size(),3,"Wrong extra-depth crystal budget");
                }
                if(density==200)helper.assertTrue(plot.upperClusters.size()>3&&plot.lowerClusters.size()>3,"Crystal budget stayed fixed at 64 when density doubled");
                helper.assertTrue(plot.blocks.keySet().stream().anyMatch(p->p.getY()<0),"No lower-band crystals generated");
                helper.assertTrue(plot.blocks.keySet().stream().anyMatch(p->p.getY()>=0),"Original upper-band crystals disappeared");
                helper.assertTrue(plot.blocks.values().stream().allMatch(s->s.getValue(CrystalClusterBlock.SIZE)==2&&s.getValue(CrystalClusterBlock.GEN)==1),"Generated crystal state differs from original SIZE/GEN rules");
                System.out.println("DEPOSIT_CRYSTAL_BUDGET density="+density+" upper_budget="+upperBudget+" upper_cluster_units="+plot.upperClusters+" lower_budget="+lowerBudget+" lower_cluster_units="+plot.lowerClusters);
            }
            helper.succeed();
        }finally{saved.restore();}
    }

    @GameTest(template="empty",timeoutTicks=200)
    public static void emptyWaterAndNetherNeverReceiveInvalidCrystals(GameTestHelper helper){
        var saved=new Config();
        try{
            WorldConfig.ORES.set(false);WorldConfig.CRYSTALS.set(true);WorldConfig.DENSITY.set(200);
            for(Host host:new Host[]{Host.AIR,Host.WATER}){
                var plot=new Plot(helper.getLevel(),-64,320,64,host);
                helper.assertFalse(run(plot,RandomSource.create(3)),"Crystals generated in unsupported/fluid space: "+host);
                helper.assertTrue(plot.blocks.isEmpty(),"Unsupported crystal mutated terrain");
            }
            WorldConfig.ORES.set(true);
            var nether=helper.getLevel().getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether!=null,"Nether test level was not loaded");
            var plot=new Plot(nether,-64,320,64,Host.DEEPSLATE);
            helper.assertFalse(run(plot,RandomSource.create(3)),"TC6 deposits leaked into Nether generation");
            helper.assertTrue(plot.blocks.isEmpty()&&plot.candidates.isEmpty(),"Nether generation attempted a placement");
            helper.succeed();
        }finally{saved.restore();}
    }

    private static int checkBand(GameTestHelper helper,Plot plot,int from,int count,int low,int high){
        for(var pos:plot.candidates.subList(from,from+count)){
            helper.assertTrue(pos.getY()>=low&&pos.getY()<high,"Candidate outside expected vertical band: "+pos);
            helper.assertTrue(pos.getX()>=2&&pos.getX()<=14&&pos.getZ()>=2&&pos.getZ()<=14,"Candidate left the original horizontal spread");
        }
        return from+count;
    }
    private static void checkFinishedBudget(GameTestHelper helper,Map<Integer,Integer> clusters,int budget,String label,int density){
        int sum=clusters.values().stream().mapToInt(Integer::intValue).sum();
        int last=clusters.entrySet().stream().max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        helper.assertTrue(sum>budget&&sum-last<=budget,"Wrong "+label+" stop condition at "+density+": "+clusters+" / "+budget);
        helper.assertTrue(clusters.values().stream().allMatch(n->n==26||n==28),"A final crystal cube was cut off before completion: "+clusters);
    }
    private static boolean run(Plot plot,RandomSource random){
        return plot.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(DEPOSITS).value()
                .place(plot.level,plot.server.getChunkSource().getGenerator(),random,BlockPos.ZERO);
    }
    private static String id(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();}
    private record Config(boolean ores,boolean crystals,int density){
        Config(){this(WorldConfig.ORES.get(),WorldConfig.CRYSTALS.get(),WorldConfig.DENSITY.get());}
        void restore(){WorldConfig.ORES.set(ores);WorldConfig.CRYSTALS.set(crystals);WorldConfig.DENSITY.set(density);}
    }
    private enum Host { STONE, DEEPSLATE, DIRT, EXISTING_ORE, CAVE, AIR, WATER }
    private static final class Plot implements InvocationHandler {
        final ServerLevel server;final int min,max,surface;final Host host;final Holder<Biome> biome;
        final Map<BlockPos,BlockState> blocks=new HashMap<>();
        final List<BlockPos> candidates=new ArrayList<>();
        final Map<Integer,Integer> upperClusters=new HashMap<>(),lowerClusters=new HashMap<>();
        ClusterRandom clusterRandom;
        final WorldGenLevel level=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},this);
        Plot(ServerLevel server,int min,int max,int surface,Host host){
            this.server=server;this.min=min;this.max=max;this.surface=surface;this.host=host;
            biome=server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        }
        boolean inside(BlockPos p){return p.getY()>=min&&p.getY()<max&&p.getX()>=0&&p.getX()<16&&p.getZ()>=0&&p.getZ()<16;}
        BlockState state(BlockPos p){
            if(p.getY()<min||p.getY()>=max)return Blocks.AIR.defaultBlockState();
            var placed=blocks.get(p);if(placed!=null)return placed;
            return switch(host){
                case STONE->Blocks.STONE.defaultBlockState();case DEEPSLATE->Blocks.DEEPSLATE.defaultBlockState();
                case DIRT->Blocks.DIRT.defaultBlockState();case EXISTING_ORE->Blocks.IRON_ORE.defaultBlockState();
                case AIR->Blocks.AIR.defaultBlockState();case WATER->Blocks.WATER.defaultBlockState();
                case CAVE->((p.getX()+p.getY()+p.getZ())&1)==0?Blocks.AIR.defaultBlockState():Blocks.DEEPSLATE.defaultBlockState();
            };
        }
        @Override public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{
            return switch(method.getName()){
                case "getLevel"->server;case "registryAccess"->server.registryAccess();case "getBiome","getNoiseBiome"->biome;
                case "getBlockState"->state((BlockPos)args[0]);case "getFluidState"->state((BlockPos)args[0]).getFluidState();
                case "isEmptyBlock"->state((BlockPos)args[0]).isAir();case "getMinBuildHeight"->min;case "getMaxBuildHeight"->max;
                case "getHeight"->{if(args==null||args.length==0)yield max-min;if(args[0]!=Heightmap.Types.WORLD_SURFACE_WG)throw new AssertionError("Deposits must use water-inclusive surface height");yield surface;}
                case "isOutsideBuildHeight"->args[0] instanceof BlockPos p?p.getY()<min||p.getY()>=max:(int)args[0]<min||(int)args[0]>=max;
                case "ensureCanWrite"->{var p=((BlockPos)args[0]).immutable();if(!p.equals(BlockPos.ZERO))candidates.add(p);yield inside(p);}
                case "hasChunk","hasChunkAt","hasChunksAt"->true;
                case "setBlock"->{var p=((BlockPos)args[0]).immutable();var s=(BlockState)args[1];if(!inside(p))throw new AssertionError("Out of bounds deposit write "+p);if(!state(p).getFluidState().isEmpty())throw new AssertionError("Deposit replaced fluid");blocks.put(p,s);if(clusterRandom!=null){var map=p.getY()<0?lowerClusters:upperClusters;map.merge(clusterRandom.center,s.getValue(CrystalClusterBlock.SIZE),Integer::sum);}yield true;}
                case "getBlockEntity"->null;case "getSeed"->0L;case "isClientSide"->false;case "toString"->"Bounded deposit fixture";
                default->{if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new UnsupportedOperationException(method.toString());}
            };
        }
    }
    private static final class ClusterRandom implements RandomSource {
        private final RandomSource fallback=RandomSource.create(1);int coordinates,center;
        @Override public int nextInt(int bound){
            if(bound==13){center=coordinates/2;boolean x=(coordinates++&1)==0;return (x?center%4:(center/4)%4)*3;}
            if(bound==59||bound==63)return 11;
            if(bound==3)return 1; // Pass every crystal roll, SIZE=2, and no biome-bias branch.
            if(bound==6)return 0;
            throw new AssertionError("Unexpected controlled crystal RNG bound: "+bound);
        }
        @Override public RandomSource fork(){return fallback.fork();}@Override public PositionalRandomFactory forkPositional(){return fallback.forkPositional();}
        @Override public void setSeed(long seed){fallback.setSeed(seed);}@Override public int nextInt(){return fallback.nextInt();}
        @Override public long nextLong(){return fallback.nextLong();}@Override public boolean nextBoolean(){return fallback.nextBoolean();}
        @Override public float nextFloat(){return fallback.nextFloat();}@Override public double nextDouble(){return fallback.nextDouble();}
        @Override public double nextGaussian(){return fallback.nextGaussian();}
    }
}
