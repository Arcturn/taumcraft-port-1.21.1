package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.world.FallingTaint;
import thaumcraft.port.world.FluxGooBlock;
import thaumcraft.port.world.WorldContent;
import thaumcraft.port.world.WorldEntities;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FallingTaintGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fallingCrustMovesThroughTheWorldAndLandsAsExactlyOneBlock(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var floor=h.absolutePos(new BlockPos(5,1,5));var level=h.getLevel();
        level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        h.assertTrue(FallingTaint.startFall(level,source,source),"Unsupported crust did not create a falling entity");
        var entity=onlyFall(h,source);h.assertTrue(level.getBlockState(source).is(WorldContent.block("taint_crust")),"Source removed before entity accepted its first tick");
        h.assertFalse(FallingTaint.startFall(level,source,source),"One crust created duplicate pending entities");
        entity.tick();h.assertTrue(level.isEmptyBlock(source),"First falling tick did not remove its original source");
        h.assertTrue(entity.getY()<source.getY()+.5&&entity.getDeltaMovement().y<0,"Crust was teleported instead of obeying falling physics");
        settle(entity);
        h.assertTrue(entity.isRemoved(),"Falling entity did not settle");h.assertTrue(level.getBlockState(floor.above()).is(WorldContent.block("taint_crust")),"Falling crust did not become a block above its support");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(source).inflate(6)).isEmpty(),"Settled crust produced an ordinary item drop");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void sidewaysCollapseRemovesItsOriginalColumnBlockOnlyOnce(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var destination=source.east();var floor=h.absolutePos(new BlockPos(6,1,5));var level=h.getLevel();
        level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        h.assertTrue(FallingTaint.startFall(level,source,destination),"Sideways crust did not spawn");var entity=onlyFall(h,destination);
        h.assertValueEqual(entity.getStartPos(),source,"Sideways falling origin lost");h.assertValueEqual(entity.getX(),destination.getX()+.5,"Sideways destination ignored");
        entity.tick();h.assertTrue(level.isEmptyBlock(source),"Sideways fall kept its old column block");settle(entity);
        h.assertTrue(level.getBlockState(floor.above()).is(WorldContent.block("taint_crust")),"Sideways crust landed in the wrong column");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void replacingThePendingSourceCancelsTheFallWithoutDeletingTheReplacement(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var level=h.getLevel();level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());
        h.assertTrue(FallingTaint.startFall(level,source,source),"Falling fixture did not spawn");var entity=onlyFall(h,source);
        level.setBlockAndUpdate(source,Blocks.CHEST.defaultBlockState());entity.tick();
        h.assertTrue(entity.isRemoved(),"Changed source created a duplicate crust");h.assertTrue(level.getBlockState(source).is(Blocks.CHEST),"Falling tick deleted a replacement container");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void logsBraceCrustAndFiniteGooSupportsOnlyAtItsSourceThreshold(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(10,3,10));var level=h.getLevel();
        h.assertTrue(FallingTaint.canFallBelow(level,pos),"Air should allow falling");
        level.setBlockAndUpdate(pos.east(),Blocks.OAK_LOG.defaultBlockState());h.assertFalse(FallingTaint.canFallBelow(level,pos),"Nearby vanilla log did not brace crust");
        level.setBlockAndUpdate(pos.east(),WorldContent.block("taint_log").defaultBlockState());h.assertFalse(FallingTaint.canFallBelow(level,pos),"Nearby tainted log did not brace crust");level.removeBlock(pos.east(),false);
        level.setBlockAndUpdate(pos,WorldContent.block("flux_goo").defaultBlockState().setValue(FluxGooBlock.LEVEL,3));h.assertTrue(FallingTaint.canFallBelow(level,pos),"Shallow goo incorrectly supported crust");
        level.setBlockAndUpdate(pos,WorldContent.block("flux_goo").defaultBlockState().setValue(FluxGooBlock.LEVEL,4));h.assertFalse(FallingTaint.canFallBelow(level,pos),"Deep goo did not support crust at source threshold four");
        level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());h.assertTrue(FallingTaint.canFallBelow(level,pos),"Water stopped falling crust");
        level.setBlockAndUpdate(pos,Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true));h.assertFalse(FallingTaint.canFallBelow(level,pos),"Waterlogged solid support was mistaken for a liquid block");
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());h.assertFalse(FallingTaint.canFallBelow(level,pos),"Solid stone allowed falling");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fallingCrustLandsOnDeepGooWithoutCrushingNearbyCreatures(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(8,5,8));var support=h.absolutePos(new BlockPos(8,1,8));var level=h.getLevel();
        level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());level.setBlockAndUpdate(support,WorldContent.block("flux_goo").defaultBlockState().setValue(FluxGooBlock.LEVEL,4));
        var cow=h.spawn(EntityType.COW,new BlockPos(8,2,8));cow.setNoAi(true);cow.setNoGravity(true);float health=cow.getHealth();
        h.assertTrue(FallingTaint.startFall(level,source,source),"Goo landing fixture did not spawn");var entity=onlyFall(h,source);settle(entity);
        h.assertTrue(entity.isRemoved(),"Crust passed through supporting finite goo");h.assertTrue(level.getBlockState(support.above()).is(WorldContent.block("taint_crust")),"Crust failed to rest above deep goo");
        h.assertValueEqual(cow.getHealth(),health,"Falling taint applied anvil-style damage");cow.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void invalidLandingNeverReplacesAContainerOrDropsAnItem(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var obstacle=h.absolutePos(new BlockPos(5,2,5));var level=h.getLevel();
        level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());level.setBlockAndUpdate(obstacle,Blocks.CHEST.defaultBlockState());
        h.assertTrue(FallingTaint.startFall(level,source,source),"Container landing fixture did not spawn");var entity=onlyFall(h,source);settle(entity);
        h.assertTrue(entity.isRemoved(),"Invalid landing leaked a falling entity");h.assertTrue(level.getBlockState(obstacle).is(Blocks.CHEST),"Falling taint replaced a container");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(obstacle).inflate(4)).isEmpty(),"Failed landing dropped an item");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fullFallingAgeAndRemovedSourceSurviveReloadAndExpireWithoutLoot(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var level=h.getLevel();level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());
        var entity=new FallingTaint(level,source,source,WorldContent.block("taint_crust").defaultBlockState());entity.setNoGravity(true);entity.tick();entity.fallTime=400;
        var saved=new CompoundTag();entity.saveWithoutId(saved);var loaded=WorldEntities.FALLING_TAINT.get().create(level);loaded.load(saved);
        h.assertValueEqual(loaded.fallTime,400,"Fall lifetime was truncated to the source's byte range");h.assertValueEqual(loaded.getStartPos(),source,"Saved falling source position");h.assertTrue(loaded.hasRemovedSource(),"Source removal marker was not saved");h.assertValueEqual(loaded.getBlockState(),entity.getBlockState(),"Falling block state failed to round-trip");
        level.setBlockAndUpdate(source,Blocks.GOLD_BLOCK.defaultBlockState());loaded.setDeltaMovement(Vec3.ZERO);loaded.setNoGravity(true);loaded.setPos(source.getX()+.5,source.getY()+2,source.getZ()+.5);
        loaded.tick();h.assertFalse(loaded.isRemoved(),"Reload incorrectly revalidated an already-removed source");h.assertTrue(level.getBlockState(source).is(Blocks.GOLD_BLOCK),"Reload deleted a new block at the old source");
        for(int i=0;i<201&&!loaded.isRemoved();i++)loaded.tick();h.assertTrue(loaded.isRemoved(),"Reload reset the 600-tick lifetime");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(source).inflate(4)).isEmpty(),"Expired crust dropped an item");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void crustCollapseHonorsMobGriefingAndNeverLoadsDistantChunks(GameTestHelper h){
        var source=h.absolutePos(new BlockPos(5,5,5));var level=h.getLevel();level.setBlockAndUpdate(source,WorldContent.block("taint_crust").defaultBlockState());
        var rule=level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);boolean previous=rule.get();
        try{rule.set(false,level.getServer());h.assertFalse(FallingTaint.startFall(level,source,source),"Crust ignored disabled mobGriefing");h.assertTrue(level.getBlockState(source).is(WorldContent.block("taint_crust")),"Disabled fall changed its source");}
        finally{rule.set(previous,level.getServer());}
        var distant=source.offset(100000,0,100000);h.assertFalse(level.hasChunkAt(distant),"Distant chunk fixture was already loaded");
        h.assertFalse(FallingTaint.startFall(level,distant,distant),"Fall started in an unloaded chunk");h.assertFalse(level.hasChunkAt(distant),"Fall check force-loaded a distant chunk");
        h.succeed();
    }
    private static FallingTaint onlyFall(GameTestHelper h,BlockPos pos){var entities=h.getLevel().getEntitiesOfClass(FallingTaint.class,new AABB(pos).inflate(2));h.assertValueEqual(entities.size(),1,"Expected one falling crust entity");return entities.getFirst();}
    private static void settle(FallingTaint entity){for(int i=0;i<120&&!entity.isRemoved();i++)entity.tick();}
    private FallingTaintGameTests(){}
}
