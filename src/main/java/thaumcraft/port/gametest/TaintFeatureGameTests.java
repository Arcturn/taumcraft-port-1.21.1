package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.mob.TaintCrawler;
import thaumcraft.port.mob.TaintSwarm;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class TaintFeatureGameTests {
    private static final class Rolls extends LegacyRandomSource {
        private final boolean decay,spawn;
        Rolls(boolean decay,boolean spawn){super(0);this.decay=decay;this.spawn=spawn;}
        @Override public int nextInt(int bound){return bound==10?(decay?0:9):0;}
        @Override public float nextFloat(){return spawn?0:.99f;}
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void swollenTaintHasSixSupportedCollisionShapesAndRotates(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));var block=WorldContent.block("taint_feature");
        for(var facing:Direction.values()){
            var support=pos.relative(facing.getOpposite());level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
            var state=block.defaultBlockState().setValue(TaintFeatureBlock.FACING,facing);h.assertTrue(state.canSurvive(level,pos),"Feature did not attach to "+facing);
            var shape=state.getShape(level,pos).bounds();var collision=state.getCollisionShape(level,pos,CollisionContext.empty()).bounds();
            h.assertValueEqual(shape,collision,"Feature collision disagrees with its selection shape");
            double thickness=switch(facing.getAxis()){case X->shape.getXsize();case Y->shape.getYsize();case Z->shape.getZsize();};h.assertValueEqual(thickness,.375d,"Source swollen taint thickness");
            for(var side:Direction.values())h.assertFalse(state.isFaceSturdy(level,pos,side),"Swollen growth exposed a solid support face");
            level.setBlockAndUpdate(pos,state);level.removeBlock(support,false);h.assertTrue(level.isEmptyBlock(pos),"Unsupported growth survived its support removal");
        }
        var north=block.defaultBlockState().setValue(TaintFeatureBlock.FACING,Direction.NORTH);
        h.assertValueEqual(north.rotate(Rotation.CLOCKWISE_90).getValue(TaintFeatureBlock.FACING),Direction.EAST,"Feature rotation lost facing");
        level.getEntitiesOfClass(TaintCrawler.class,new AABB(pos).inflate(3)).forEach(Entity::discard);h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void breakingSwollenGrowthReleasesExactlyCrawlerOrOneFlux(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));var block=WorldContent.block("taint_feature");level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var aura=AuraSystem.get(level,pos);float before=aura.flux();var area=new AABB(pos).inflate(3);int existing=level.getEntitiesOfClass(TaintCrawler.class,area).size();level.random.setSeed(813);
        for(int i=0;i<24;i++){level.setBlockAndUpdate(pos,block.defaultBlockState());level.removeBlock(pos,false);}
        var crawlers=level.getEntitiesOfClass(TaintCrawler.class,area);int born=crawlers.size()-existing;float pollution=aura.flux()-before;
        h.assertValueEqual(born+(int)pollution,24,"Growth replacement did not release exactly one source outcome");h.assertTrue(born>0&&pollution>0,"Both source break outcomes were not exercised");
        crawlers.forEach(Entity::discard);h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void seedlessFeatureAndGeyserDecayIntoGoo(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));int oldArea=WorldConfig.TAINT_SPREAD_AREA.get();
        try{
            WorldConfig.TAINT_SPREAD_AREA.set(8);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            for(String id:List.of("taint_feature","taint_geyser")){
                var state=WorldContent.block(id).defaultBlockState();level.setBlockAndUpdate(pos,state);
                h.assertFalse(TaintSystem.nearby(level,pos),"Decay fixture unexpectedly has a sustaining seed");state.randomTick(level,pos,new Rolls(true,false));
                h.assertTrue(level.getBlockState(pos).is(WorldContent.block("flux_goo")),"Seedless "+id+" failed to become goo");
            }
        }finally{WorldConfig.TAINT_SPREAD_AREA.set(oldArea);level.getEntitiesOfClass(TaintCrawler.class,new AABB(pos).inflate(3)).forEach(Entity::discard);}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void swollenGrowthBecomesGeyserOnlyOnVerticalTaintLogAndWithGriefingEnabled(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));var feature=WorldContent.block("taint_feature").defaultBlockState();var log=WorldContent.block("taint_log").defaultBlockState();
        var rule=level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);boolean old=rule.get();double oldRate=WorldConfig.TAINT_SPREAD_RATE.get();
        try{
            WorldConfig.TAINT_SPREAD_RATE.set(0d);rule.set(true,level.getServer());level.setBlockAndUpdate(pos.below(),log.setValue(BlockStateProperties.AXIS,Direction.Axis.X));level.setBlockAndUpdate(pos,feature);feature.randomTick(level,pos,new Rolls(false,false));h.assertTrue(level.getBlockState(pos).is(feature.getBlock()),"Horizontal log grew a geyser");
            level.setBlockAndUpdate(pos.below(),log.setValue(BlockStateProperties.AXIS,Direction.Axis.Y));rule.set(false,level.getServer());feature.randomTick(level,pos,new Rolls(false,false));h.assertTrue(level.getBlockState(pos).is(feature.getBlock()),"Feature evolution ignored mobGriefing");
            rule.set(true,level.getServer());feature.randomTick(level,pos,new Rolls(false,false));h.assertTrue(level.getBlockState(pos).is(WorldContent.block("taint_geyser")),"Vertical infected log did not mature into geyser");
        }finally{rule.set(old,level.getServer());WorldConfig.TAINT_SPREAD_RATE.set(oldRate);level.getEntitiesOfClass(TaintCrawler.class,new AABB(pos).inflate(3)).forEach(Entity::discard);}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void geyserAddsQuarterFluxOnlyBelowTwoAndSpawnsOneNearbySwarm(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));var state=WorldContent.block("taint_geyser").defaultBlockState();level.setBlockAndUpdate(pos,state);
        var aura=AuraSystem.get(level,pos);float originalFlux=aura.flux();aura.drain(true,Float.MAX_VALUE,false);AuraSystem.dirty(level,pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"geyser-test"));player.setPos(Vec3.atCenterOf(pos).add(2,0,0));
        var area=new AABB(Vec3.atCenterOf(pos),Vec3.atCenterOf(pos)).inflate(32);
        try{
            state.randomTick(level,pos,new Rolls(false,false));h.assertValueEqual(aura.flux(),.25f,"Geyser flux increment");
            aura.addFlux(1.75f);state.randomTick(level,pos,new Rolls(false,false));h.assertValueEqual(aura.flux(),2f,"Geyser polluted above source threshold");
            level.addNewPlayer(player);state.randomTick(level,pos,new Rolls(false,true));var swarms=level.getEntitiesOfClass(TaintSwarm.class,area);h.assertValueEqual(swarms.size(),1,"Geyser did not create exactly one nearby swarm");h.assertFalse(swarms.getFirst().getIsSummoned(),"Geyser swarm received summoned self-decay flag");
            h.assertTrue(Math.abs(swarms.getFirst().getY()-(pos.getY()+1.25))<.0001,"Source geyser emergence height");
            state.randomTick(level,pos,new Rolls(false,true));h.assertValueEqual(level.getEntitiesOfClass(TaintSwarm.class,area).size(),1,"Geyser exceeded one swarm per source neighbourhood");
        }finally{level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);level.getEntitiesOfClass(TaintSwarm.class,area).forEach(Entity::discard);aura.drain(true,Float.MAX_VALUE,false);aura.addFlux(originalFlux);AuraSystem.dirty(level,pos);}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void disabledTaintFreezesFeatureDecayAndGeyserPollution(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));boolean previous=WorldConfig.TAINT.get();level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        try{
            WorldConfig.TAINT.set(false);
            for(String id:List.of("taint_feature","taint_geyser")){
                var state=WorldContent.block(id).defaultBlockState();level.setBlockAndUpdate(pos,state);float flux=AuraSystem.get(level,pos).flux();
                state.randomTick(level,pos,new Rolls(true,false));h.assertTrue(level.getBlockState(pos).is(state.getBlock()),"Disabled taint changed "+id);h.assertValueEqual(AuraSystem.get(level,pos).flux(),flux,"Disabled geyser polluted aura");
            }
        }finally{WorldConfig.TAINT.set(previous);level.getEntitiesOfClass(TaintCrawler.class,new AABB(pos).inflate(3)).forEach(Entity::discard);}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void featureAndGeyserHaveOnlySilkTouchBlockDrops(GameTestHelper h){
        var level=h.getLevel();var tool=new ItemStack(Items.DIAMOND_PICKAXE);var silk=tool.copy();silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        for(String id:List.of("taint_feature","taint_geyser")){
            var block=WorldContent.block(id);
            for(var candidate:List.of(tool,silk)){
                var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.BLOCK_STATE,block.defaultBlockState()).withParameter(LootContextParams.TOOL,candidate).create(LootContextParamSets.BLOCK);
                var drops=level.getServer().reloadableRegistries().getLootTable(block.getLootTable()).getRandomItems(params,19);
                if(candidate==tool)h.assertTrue(drops.isEmpty(),id+" produced ordinary block loot");else h.assertTrue(drops.size()==1&&drops.getFirst().is(block.asItem()),id+" lost source silk harvest");
            }
        }
        h.succeed();
    }
    private TaintFeatureGameTests(){}
}
