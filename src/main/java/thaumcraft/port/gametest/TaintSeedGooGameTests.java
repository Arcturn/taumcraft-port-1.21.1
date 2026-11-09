package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TaintSeedGooGameTests {
    private static final class Rolls extends LegacyRandomSource {
        private final int[] ints;private final boolean result;private int index;
        Rolls(boolean result,int...ints){super(0);this.result=result;this.ints=ints;}
        @Override public int nextInt(int bound){return Math.floorMod(index<ints.length?ints[index++]:0,bound);}
        @Override public boolean nextBoolean(){return result;}
    }
    private static FluxGooBlock goo(){return (FluxGooBlock)WorldContent.block("flux_goo");}
    private static TaintSeed seed(GameTestHelper h,boolean prime,BlockPos relative){
        var seed=(prime?WorldEntities.PRIME:WorldEntities.SEED).get().create(h.getLevel());seed.setNoAi(true);seed.setNoGravity(true);
        seed.setPos(Vec3.atBottomCenterOf(h.absolutePos(relative)));return seed;
    }
    private static int units(GameTestHelper h,BlockPos pos){var state=h.getLevel().getBlockState(pos);return state.is(goo())?state.getValue(FluxGooBlock.LEVEL)+1:0;}

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void seedsAreRootedAndPrimeKeepsOriginalExperienceAndBreathing(GameTestHelper h){
        var normal=seed(h,false,new BlockPos(3,4,3));var prime=seed(h,true,new BlockPos(8,4,3));
        h.assertValueEqual(normal.getMaxHealth(),75f,"Seed health");h.assertValueEqual(prime.getMaxHealth(),150f,"Prime health");
        h.assertValueEqual(normal.getExperienceReward(h.getLevel(),null),8,"Seed XP");h.assertValueEqual(prime.getExperienceReward(h.getLevel(),null),12,"Prime XP");
        Vec3 original=normal.position();normal.move(MoverType.SELF,new Vec3(2,1,3));h.assertValueEqual(normal.position(),original,"Rooted seed moved horizontally or rose");
        normal.move(MoverType.SELF,new Vec3(0,-.25,0));h.assertTrue(normal.getY()<original.y,"Seed could not fall");
        h.assertFalse(normal.isPushable(),"Seed collision pushes");h.assertFalse(prime.removeWhenFarAway(200000),"Prime distance despawn");
        h.assertTrue(normal.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER)&&prime.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER),"Seeds lost source underwater breathing");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void seedBoostPersistsAndUnpollutedAuraCausesSourceStarvation(GameTestHelper h){
        var seed=seed(h,false,new BlockPos(3,3,3));var chunk=h.getLevel().getChunkAt(seed.blockPosition());var old=chunk.getData(AuraSystem.AURA);
        try{
            chunk.setData(AuraSystem.AURA,new AuraState(100,100,0));seed.boost=2;seed.tickCount=1;seed.tick();h.assertValueEqual(seed.boost,1,"Boost must burn every tick");h.assertValueEqual(seed.getHealth(),75f,"Boost did not bypass zero flux");
            var tag=new CompoundTag();seed.saveWithoutId(tag);var restored=seed(h,false,new BlockPos(8,3,3));restored.load(tag);h.assertValueEqual(restored.boost,1,"Seed boost lost after reload");
            seed.boost=0;seed.invulnerableTime=0;seed.tickCount=20;seed.tick();h.assertValueEqual(seed.getHealth(),74.5f,"Flux-starved seed damage");
            h.assertTrue(Math.abs(AuraSystem.get(h.getLevel(),seed.blockPosition()).flux()-.1f)<.00001,"Flux-starved seed pollution");
        }finally{chunk.setData(AuraSystem.AURA,old);}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void seedAuraUsesSourceBlockCentredCubeAndPrimeAmplifier(GameTestHelper h){
        var normal=seed(h,false,new BlockPos(8,3,8));Vec3 center=Vec3.atCenterOf(normal.blockPosition());
        var corner=EntityType.COW.create(h.getLevel());corner.setNoAi(true);corner.setNoGravity(true);corner.setPos(center.add(3.8,0,3.8));h.getLevel().addFreshEntity(corner);
        var outside=EntityType.COW.create(h.getLevel());outside.setNoAi(true);outside.setNoGravity(true);outside.setPos(center.add(4.6,0,0));h.getLevel().addFreshEntity(outside);
        try{
            normal.tickCount=20;normal.tick();h.assertTrue(corner.hasEffect(WorldEntities.TAINT),"Seed incorrectly used a sphere instead of the source cube");h.assertFalse(outside.hasEffect(WorldEntities.TAINT),"Seed aura grew by its collision width");
            var prime=seed(h,true,new BlockPos(8,3,8));prime.tickCount=20;prime.tick();var effect=outside.getEffect(WorldEntities.TAINT);
            h.assertTrue(effect!=null&&effect.getAmplifier()==1&&effect.getDuration()==100,"Prime eight-block aura and amplifier");
        }finally{corner.discard();outside.discard();}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void seedSpawnChecksExcludeWaterAndNearbySeedsInTheSourceCube(GameTestHelper h){
        var seed=seed(h,false,new BlockPos(1,3,1));var other=seed(h,true,new BlockPos(23,3,23));h.getLevel().addFreshEntity(other);
        try{h.assertFalse(seed.checkSpawnRules(h.getLevel(),MobSpawnType.MOB_SUMMONED),"Seed ignored source cube exclusion around existing prime");}finally{other.discard();}
        var pos=seed.blockPosition();h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());h.assertFalse(seed.checkSpawnObstruction(h.getLevel()),"Seed can spawn submerged");h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void normalAndPrimeSeedDeathsDropFluxCrystalsAtTheBodyCentre(GameTestHelper h){
        for(boolean prime:List.of(false,true)){
            var seed=seed(h,prime,new BlockPos(8,3,8));h.getLevel().addFreshEntity(seed);var area=seed.getBoundingBox().inflate(3);double y=seed.getY()+seed.getBbHeight()/2;
            seed.hurt(seed.damageSources().genericKill(),1000);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
            h.assertTrue(prime?drops.size()>=1&&drops.size()<=3:drops.size()==1,"Seed crystal drop count");
            for(var item:drops){h.assertTrue(thaumcraft.port.content.CrystalItem.aspect(item.getItem())==thaumcraft.port.aspect.Aspect.FLUX,"Seed dropped non-flux crystal");h.assertTrue(Math.abs(item.getY()-y)<.00001,"Seed crystal drop height");item.discard();}seed.discard();
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooHorizontalFlowConservesVolumeAndSharesAcrossAllLowerNeighbours(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));var level=h.getLevel();level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,goo().defaultBlockState());
        for(var direction:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(pos.relative(direction),Blocks.AIR.defaultBlockState());
        goo().flow(level,pos,new Rolls(false,0,1,0,1));int total=units(h,pos);h.assertTrue(total>=1&&total<=2,"Centre did not share finite goo");
        for(var direction:Direction.Plane.HORIZONTAL){int n=units(h,pos.relative(direction));h.assertTrue(n>=1&&n<=2,"Directional transfer order biased goo volume");total+=n;}
        h.assertValueEqual(total,8,"Finite goo created or lost volume during flow");h.assertValueEqual(FluxGooBlock.TICK_DELAY,30,"Source viscosity tick delay");level.removeBlock(pos,false);for(var direction:Direction.Plane.HORIZONTAL)level.removeBlock(pos.relative(direction),false);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooDisplacesSourceNonblockingDevicesButPreservesPlatesAndWater(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));var level=h.getLevel();level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        for(var direction:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(pos.relative(direction).below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,goo().defaultBlockState());level.setBlockAndUpdate(pos.north(),Blocks.TORCH.defaultBlockState());
        level.setBlockAndUpdate(pos.south(),Blocks.RAIL.defaultBlockState());level.setBlockAndUpdate(pos.west(),Blocks.REDSTONE_WIRE.defaultBlockState());level.setBlockAndUpdate(pos.east(),Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        goo().flow(level,pos,new Rolls(false));
        for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST))h.assertTrue(level.getBlockState(pos.relative(direction)).is(goo()),"Finite goo could not displace source nonblocking device");
        h.assertTrue(level.getBlockState(pos.east()).is(Blocks.STONE_PRESSURE_PLATE),"Finite goo destroyed an explicitly protected pressure plate");
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        for(var item:List.of(net.minecraft.world.item.Items.TORCH,net.minecraft.world.item.Items.RAIL,net.minecraft.world.item.Items.REDSTONE))h.assertTrue(drops.stream().anyMatch(drop->drop.getItem().is(item)),"Displaced device did not drop its item");
        level.setBlockAndUpdate(pos.east(),Blocks.WATER.defaultBlockState());level.setBlockAndUpdate(pos,goo().defaultBlockState());goo().flow(level,pos,new Rolls(false));h.assertTrue(level.getBlockState(pos.east()).is(Blocks.WATER),"Low-density goo displaced denser water");
        drops.forEach(Entity::discard);level.removeBlock(pos,false);for(var direction:Direction.Plane.HORIZONTAL)level.removeBlock(pos.relative(direction),false);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooVerticalMergeRetainsRemainderAndUpdatesCoveredSurface(GameTestHelper h){
        var top=h.absolutePos(new BlockPos(8,4,8));var bottom=top.below();var level=h.getLevel();level.setBlockAndUpdate(bottom.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(bottom,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,3));level.setBlockAndUpdate(top,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,6));
        for(var direction:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(top.relative(direction),Blocks.STONE.defaultBlockState());
        goo().flow(level,top,new Rolls(false));h.assertValueEqual(units(h,bottom),8,"Lower goo was not filled");h.assertValueEqual(units(h,top),3,"Vertical overflow lost finite remainder");
        h.assertTrue(level.getBlockState(bottom).getValue(FluxGooBlock.COVERED),"Stacked goo retained a visible column gap");level.removeBlock(top,false);
        h.assertFalse(level.getBlockState(bottom).getValue(FluxGooBlock.COVERED),"Exposed goo retained full-column height");h.assertTrue(Math.abs(level.getBlockState(bottom).getShape(level,bottom).max(Direction.Axis.Y)-8d/9d)<.00001,"Exposed source goo height differs from Forge fluid geometry");level.removeBlock(bottom,false);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooPollutesOnlyWhenItEvaporatesAndNotWhenItBecomesFibre(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));var level=h.getLevel();var chunk=level.getChunkAt(pos);var old=chunk.getData(AuraSystem.AURA);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        try{
            chunk.setData(AuraSystem.AURA,new AuraState(100,100,0));level.setBlockAndUpdate(pos,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,0));goo().advance(level,pos,new Rolls(false,0));
            h.assertTrue(level.getBlockState(pos).is(WorldContent.block("taint_fibre")),"Last goo quantum did not become fibre");h.assertValueEqual(AuraSystem.get(level,pos).flux(),0f,"Fibre conversion incorrectly polluted aura");
            level.setBlockAndUpdate(pos,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,0));goo().advance(level,pos,new Rolls(true,0));h.assertTrue(level.isEmptyBlock(pos),"Last goo quantum did not evaporate");h.assertValueEqual(AuraSystem.get(level,pos).flux(),1f,"Last quantum evaporation pollution");
            level.setBlockAndUpdate(pos,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,4));goo().advance(level,pos,new Rolls(false,1,0));h.assertValueEqual(units(h,pos),4,"Goo evaporated more than one quantum");h.assertValueEqual(AuraSystem.get(level,pos).flux(),2f,"Partial evaporation pollution");
        }finally{level.removeBlock(pos,false);chunk.setData(AuraSystem.AURA,old);}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooEmergenceConsumesItsBlockAndUsesTheSourceSlimeThreshold(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));var level=h.getLevel();level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        for(int meta:new int[]{2,6}){
            level.setBlockAndUpdate(pos,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,meta));goo().advance(level,pos,new Rolls(false,0));var slimes=level.getEntitiesOfClass(ThaumicSlime.class,new AABB(pos).inflate(2));
            h.assertTrue(level.isEmptyBlock(pos)&&slimes.size()==1,"Goo did not release exactly one slime and consume itself");h.assertValueEqual(slimes.getFirst().getSize(),meta<6?1:2,"Goo slime size threshold");slimes.forEach(Entity::discard);
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void gooContactSlowsMotionMakesExhaustionMilkImmuneAndFeedsSlimes(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));var level=h.getLevel();var state=goo().defaultBlockState();level.setBlockAndUpdate(pos,state);
        var cow=EntityType.COW.create(level);cow.setDeltaMovement(.5,.2,.4);state.entityInside(level,pos,cow);h.assertValueEqual(cow.getDeltaMovement(),new Vec3(0,.2,0),"Full goo did not stop horizontal movement");
        var effect=cow.getEffect(WorldEntities.VIS_EXHAUST);h.assertTrue(effect!=null&&effect.getDuration()==600&&effect.getAmplifier()==2&&effect.isAmbient()&&effect.isVisible(),"Source vis exhaustion flags");
        h.assertFalse(effect.getCures().contains(EffectCures.MILK),"Goo exhaustion remained milk-curable");h.assertTrue(effect.getCures().contains(EffectCures.PROTECTED_BY_TOTEM),"Goo broke the vanilla1.12 totem cure");
        var slime=WorldEntities.SLIME.get().create(level);slime.setSize(1,true);level.random.setSeed(123);state.entityInside(level,pos,slime);
        h.assertValueEqual(slime.getSize(),2,"Goo did not feed its thaumic slime");h.assertValueEqual(units(h,pos),7,"Feeding slime did not consume one quantum");level.removeBlock(pos,false);h.succeed();
    }
    private TaintSeedGooGameTests(){}
}
