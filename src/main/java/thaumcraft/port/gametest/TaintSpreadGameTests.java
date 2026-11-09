package thaumcraft.port.gametest;

import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TaintSpreadGameTests {
    private static final class ZeroRandom extends LegacyRandomSource {ZeroRandom(){super(0);}@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return 0;}}
    private static TaintSeed seed(GameTestHelper h,BlockPos position){var seed=h.spawn(WorldEntities.SEED.get(),position);seed.setNoAi(true);seed.setNoGravity(true);return seed;}
    private static void hem(GameTestHelper h,BlockPos target){for(var side:Direction.values())h.getLevel().setBlockAndUpdate(target.relative(side),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(target.west(),WorldContent.block("taint_rock").defaultBlockState());}
    private static BlockPos growthPosition(GameTestHelper h,int roll){for(int x=2;x<20;x++)for(int z=2;z<20;z++){var p=h.absolutePos(new BlockPos(x,3,z));if(new Random(TaintFibreBlock.growthSeed(p)).nextInt(50)==roll)return p;}throw new IllegalStateException("No deterministic fibre position");}
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void sourceMaterialConversionsKeepLogAxisAndFluxCosts(GameTestHelper h){
        var level=h.getLevel();var livingSeed=seed(h,new BlockPos(8,5,8));var pos=h.absolutePos(new BlockPos(6,3,6));hem(h,pos);var aura=AuraSystem.get(level,pos);float oldFlux=aura.flux();aura.addFlux(10);
        try{
            level.setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));float before=aura.flux();
            h.assertTrue(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Hemmed log did not convert");h.assertTrue(level.getBlockState(pos).is(WorldContent.block("taint_log")),"Wrong log conversion");h.assertValueEqual(level.getBlockState(pos).getValue(TaintLogBlock.AXIS),Direction.Axis.X,"Log axis changed");h.assertValueEqual(aura.flux(),before,"Source log conversion unexpectedly consumed flux");
            for(var pair:new Object[][]{{Blocks.PUMPKIN,"taint_crust"},{Blocks.OAK_PLANKS,"taint_crust"},{Blocks.CLAY,"taint_soil"},{Blocks.GRAVEL,"taint_soil"},{Blocks.DIAMOND_ORE,"taint_rock"},{WorldContent.block("stone_porous"),"taint_rock"},{WorldContent.block("grass_ambient"),"taint_soil"}}){
                level.setBlockAndUpdate(pos,((Block)pair[0]).defaultBlockState());before=aura.flux();h.assertTrue(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Missing source material conversion: "+pair[0]);h.assertTrue(level.getBlockState(pos).is(WorldContent.block((String)pair[1])),"Wrong taint material");h.assertTrue(Math.abs(aura.flux()-(before-.01f))<.0002f,"Conversion flux cost differs from .01");
            }
            level.setBlockAndUpdate(pos,Blocks.WHITE_WOOL.defaultBlockState());h.assertFalse(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Source cloth was wrongly treated as wood");
        }finally{aura.drain(true,Float.MAX_VALUE,false);aura.addFlux(oldFlux);livingSeed.discard();}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void leavesConvertWithoutGroundAndAttachFeaturesToTaintedLogs(GameTestHelper h){
        var level=h.getLevel();var livingSeed=seed(h,new BlockPos(8,5,8));var pos=h.absolutePos(new BlockPos(6,3,6));
        try{
            for(var side:Direction.values())level.removeBlock(pos.relative(side),false);level.setBlockAndUpdate(pos,Blocks.OAK_LEAVES.defaultBlockState());
            h.assertTrue(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Unsupported leaf did not convert");h.assertTrue(level.getBlockState(pos).is(WorldContent.block("taint_fibre")),"Unsupported leaf should create fibre");
            level.setBlockAndUpdate(pos.east(),WorldContent.block("taint_log").defaultBlockState());level.setBlockAndUpdate(pos,Blocks.OAK_LEAVES.defaultBlockState());
            h.assertTrue(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Leaf by tainted log did not convert");h.assertTrue(level.getBlockState(pos).is(WorldContent.block("taint_feature")),"Leaf did not create source attachment feature");h.assertValueEqual(level.getBlockState(pos).getValue(TaintFeatureBlock.FACING),Direction.WEST,"Feature faces into its support");
        }finally{livingSeed.discard();}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void hemmingBalancesOpenFacesAndSpreadProtectsFluidsContainersAndRules(GameTestHelper h){
        var level=h.getLevel();var livingSeed=seed(h,new BlockPos(8,5,8));var pos=h.absolutePos(new BlockPos(6,3,6));hem(h,pos);
        var rule=level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);boolean old=rule.get();
        try{
            h.assertTrue(TaintSystem.hemmedByTaint(level,pos),"One tainted neighbour among solids should hem the target");level.setBlockAndUpdate(pos.west(),WorldContent.block("flux_goo").defaultBlockState());h.assertTrue(TaintSystem.hemmedByTaint(level,pos),"Flux goo must count as source taint material");level.setBlockAndUpdate(pos.west(),WorldContent.block("taint_rock").defaultBlockState());level.removeBlock(pos.east(),false);h.assertFalse(TaintSystem.hemmedByTaint(level,pos),"One open face must cancel one tainted face");level.setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());
            for(var block:new Block[]{Blocks.WATER,Blocks.CHEST,Blocks.BEDROCK,Blocks.OBSIDIAN}){level.setBlockAndUpdate(pos,block.defaultBlockState());h.assertFalse(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Spread replaced protected target "+block);h.assertTrue(level.getBlockState(pos).is(block),"Protected block changed");}
            rule.set(false,level.getServer());level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());h.assertFalse(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Spread ignored mobGriefing");
        }finally{rule.set(old,level.getServer());livingSeed.discard();}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fibresKeepSourceGrowthLightingAndThinCollisionOnTaintedSupports(GameTestHelper h){
        var level=h.getLevel();var block=(TaintFibreBlock)WorldContent.block("taint_fibre");
        for(int roll:new int[]{0,4,6,48}){
            var pos=growthPosition(h,roll);level.setBlockAndUpdate(pos.below(),WorldContent.block("taint_soil").defaultBlockState());level.setBlockAndUpdate(pos.above(),roll==48?WorldContent.block("taint_log").defaultBlockState():Blocks.AIR.defaultBlockState());
            var state=block.attached(level,pos);level.setBlockAndUpdate(pos,state);int growth=roll==0?1:roll==4?2:roll==6?3:4;
            h.assertValueEqual(state.getValue(TaintFibreBlock.GROWTH),growth,"Position-derived source growth");h.assertValueEqual(state.getLightEmission(level,pos),growth==3?12:growth==2||growth==4?6:0,"Growth light emission");
            h.assertTrue(state.getValue(CrystalClusterBlock.FACES.get(Direction.DOWN)),"Fibre did not attach to solid tainted soil");var collision=state.getCollisionShape(level,pos,CollisionContext.empty());h.assertFalse(collision.isEmpty(),"Source thin film collision missing");
            if(roll!=48)h.assertTrue(Math.abs(collision.max(Direction.Axis.Y)-.05)<.0001,"Fibre collision is not .05 high");
            h.assertTrue(state.getShape(level,pos).max(Direction.Axis.Y)>collision.min(Direction.Axis.Y),"Growth outline missing");
        }h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fibresAndLogsDecayImmediatelyButSolidsUseTheirSourceReplacement(GameTestHelper h){
        int oldRadius=WorldConfig.TAINT_SPREAD_AREA.get();WorldConfig.TAINT_SPREAD_AREA.set(8);var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));
        try{
            for(var pair:new String[][]{{"taint_fibre","air"},{"taint_log","flux_goo"},{"taint_soil","dirt"},{"taint_rock","stone_porous"},{"taint_crust","flux_goo"}}){
                level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,WorldContent.block(pair[0]).defaultBlockState());var state=level.getBlockState(pos);state.randomTick(level,pos,new ZeroRandom());
                var expected=pair[1].equals("air")?Blocks.AIR:pair[1].equals("dirt")?Blocks.DIRT:WorldContent.block(pair[1]);h.assertTrue(level.getBlockState(pos).is(expected),"Wrong seedless decay for "+pair[0]);
            }
        }finally{WorldConfig.TAINT_SPREAD_AREA.set(oldRadius);}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void livingSeedQueriesUseConfigRadiusAndStrictFringe(GameTestHelper h){
        int oldRadius=WorldConfig.TAINT_SPREAD_AREA.get();WorldConfig.TAINT_SPREAD_AREA.set(10);var livingSeed=seed(h,new BlockPos(3,3,3));var origin=livingSeed.blockPosition();
        try{h.assertTrue(TaintSystem.nearby(h.getLevel(),origin.east(10)),"Outer boundary missing");h.assertFalse(TaintSystem.atSeedEdge(h.getLevel(),origin.east(8)),"Inner fringe boundary must be strict");h.assertTrue(TaintSystem.atSeedEdge(h.getLevel(),origin.east(9)),"Seed propagation fringe missing");h.assertFalse(TaintSystem.atSeedEdge(h.getLevel(),origin.east(10)),"Outer fringe boundary must be strict");livingSeed.discard();h.assertFalse(TaintSystem.nearby(h.getLevel(),origin),"Dead or discarded seed retained a ghost aura");}
        finally{livingSeed.discard();WorldConfig.TAINT_SPREAD_AREA.set(oldRadius);}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void luminousFibreHarvestPollutesAndFortuneDropsFluxCrystal(GameTestHelper h){
        var level=h.getLevel();var pos=growthPosition(h,6);var block=(TaintFibreBlock)WorldContent.block("taint_fibre");level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());var state=block.attached(level,pos);level.setBlockAndUpdate(pos,state);
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),4);
        var drops=Block.getDrops(state,level,pos,null,null,tool);h.assertValueEqual(drops.size(),1,"Fortune IV should guarantee the growth3 crystal");h.assertTrue(drops.getFirst().is(CrystalItem.stack(Aspect.FLUX).getItem()),"Wrong growth crystal item");
        var aura=AuraSystem.get(level,pos);float before=aura.flux();state.spawnAfterBreak(level,pos,tool,true);h.assertTrue(Math.abs(aura.flux()-before-1)<.0001,"Harvest did not pollute aura by one");aura.drain(true,1,false);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void taintedSoilAtTheSeedFringeCreatesARealSeedAndConsumesFiveFlux(GameTestHelper h){
        int oldRadius=WorldConfig.TAINT_SPREAD_AREA.get();WorldConfig.TAINT_SPREAD_AREA.set(10);var level=h.getLevel();var parent=seed(h,new BlockPos(3,3,3));var pos=parent.blockPosition().east(9);
        var aura=AuraSystem.get(level,pos);float before=aura.flux();aura.drain(true,Float.MAX_VALUE,false);aura.addFlux(10);
        try{
            level.setBlockAndUpdate(pos,WorldContent.block("taint_soil").defaultBlockState());level.removeBlock(pos.above(),false);level.removeBlock(pos.above(2),false);
            h.assertTrue(TaintSystem.spreadTo(level,pos.west(),pos,new ZeroRandom()),"Tainted soil failed to propagate a seed at the fringe");
            var children=level.getEntitiesOfClass(TaintSeed.class,new net.minecraft.world.phys.AABB(pos.above()).inflate(2),seed->seed!=parent&&seed.isAlive());h.assertValueEqual(children.size(),1,"Fringe seed count");h.assertValueEqual(aura.flux(),5f,"New seed flux debit");children.forEach(TaintSeed::discard);
        }finally{parent.discard();WorldConfig.TAINT_SPREAD_AREA.set(oldRadius);aura.drain(true,Float.MAX_VALUE,false);aura.addFlux(before);}h.succeed();
    }
    private TaintSpreadGameTests(){}
}
