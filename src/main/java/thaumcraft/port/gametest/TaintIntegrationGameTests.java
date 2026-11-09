package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class TaintIntegrationGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void taintDamagePreservesMagicArmorBypassAndOriginalDeathMessage(GameTestHelper h){
        var cow=EntityType.COW.create(h.getLevel());cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(100);cow.setHealth(10);
        WorldEntities.TAINT.get().applyEffectTick(cow,0);h.assertValueEqual(cow.getHealth(),9f,"Taint damage must bypass ordinary armor");
        var source=cow.getLastDamageSource();h.assertTrue(source!=null&&source.is(FluxEffect.TAINT_DAMAGE),"Taint reused the generic magic death message");
        h.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC),"Taint damage classifications");
        h.assertValueEqual(source.getMsgId(),"taint","Original taint death-message key");h.assertValueEqual(source.getFoodExhaustion(),0f,"Bypass armor damage has no food exhaustion");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void taintFeaturesHaveSourceAspectsLightAndMaterialProperties(GameTestHelper h){
        var feature=WorldContent.block("taint_feature");var geyser=WorldContent.block("taint_geyser");var pos=h.absolutePos(new BlockPos(3,3,3));
        h.assertValueEqual(AspectTags.forItem(new ItemStack(feature)),Map.of(Aspect.AURA,5,Aspect.BEAST,5,Aspect.FLUX,10),"Feature source aspects");
        h.assertValueEqual(AspectTags.forItem(new ItemStack(geyser)),Map.of(Aspect.AURA,5,Aspect.WATER,5,Aspect.FLUX,10),"Geyser source aspects");
        h.assertValueEqual(feature.defaultBlockState().getLightEmission(h.getLevel(),pos),9,"Feature light level");
        h.assertValueEqual(geyser.defaultBlockState().getDestroySpeed(h.getLevel(),pos),10f,"Geyser hardness");
        h.assertValueEqual(geyser.getExplosionResistance(),60f,"Old resistance100 converts to modern60");
        h.assertTrue(WorldContent.block("taint_log").defaultBlockState().is(BlockTags.LOGS),"Taintwood must support leaves and brace crust");
        h.assertTrue(WorldContent.block("taint_log").defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE),"Taintwood axe tool missing");
        h.assertTrue(WorldEntities.SEED.get().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER)&&WorldEntities.PRIME.get().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER),"Seed underwater breathing classification");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void riftSeedEventTransfersBoostAndPollutionOnlyAfterSuccessfulSpawn(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,40,8));var rift=WorldEntities.RIFT.get().create(h.getLevel());rift.setPos(pos.getX(),pos.getY(),pos.getZ());
        var aura=AuraSystem.get(h.getLevel(),pos);float before=aura.flux();int size=rift.size();
        h.assertTrue(rift.trySpawnTaintSeed(pos),"Clear rift event did not create prime seed");
        var seeds=h.getLevel().getEntitiesOfClass(TaintSeed.class,new net.minecraft.world.phys.AABB(pos).inflate(3));
        h.assertValueEqual(seeds.size(),1,"Prime seed duplicated");h.assertTrue(seeds.getFirst().prime(),"Rift created ordinary seed");h.assertValueEqual(seeds.getFirst().boost,size,"Rift size was not transferred as boost");
        h.assertTrue(rift.isRemoved(),"Successful seed event retained rift");h.assertValueEqual(aura.flux(),before+size/2,"Source integer pollution transfer");
        h.assertFalse(rift.trySpawnTaintSeed(pos.above(3)),"Removed rift created another seed");seeds.forEach(TaintSeed::discard);aura.drain(true,aura.flux()-before,false);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void riftSeedRejectsNearbySeedsFluidsAndDisabledSpreadWithoutConsumption(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,40,8));var rift=WorldEntities.RIFT.get().create(h.getLevel());rift.setPos(pos.getX(),pos.getY(),pos.getZ());
        var neighbour=WorldEntities.SEED.get().create(h.getLevel());neighbour.setPos(pos.getX()+2,pos.getY(),pos.getZ());neighbour.setNoAi(true);h.getLevel().addFreshEntity(neighbour);
        var aura=AuraSystem.get(h.getLevel(),pos);float before=aura.flux();
        h.assertFalse(rift.trySpawnTaintSeed(pos),"Seed overlap was ignored");neighbour.discard();
        h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());h.assertFalse(rift.trySpawnTaintSeed(pos),"Seed spawned in liquid");h.getLevel().removeBlock(pos,false);
        boolean configured=WorldConfig.TAINT.get();try{WorldConfig.TAINT.set(false);h.assertFalse(rift.trySpawnTaintSeed(pos),"Disabled taint created seed");}finally{WorldConfig.TAINT.set(configured);}
        h.assertFalse(rift.isRemoved(),"Failed seed attempt consumed rift");h.assertValueEqual(aura.flux(),before,"Failed seed attempt polluted aura");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void bottledTaintSkipsEveryTaintedMobFamily(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(7,3,7));var family=java.util.List.of(thaumcraft.port.mob.MobContent.CRAWLER.get(),thaumcraft.port.mob.MobContent.SWARM.get(),thaumcraft.port.mob.MobContent.TAINTACLE.get(),thaumcraft.port.boss.BossContent.GIANT_TAINTACLE.get());
        var mobs=new java.util.ArrayList<net.minecraft.world.entity.Mob>();
        for(var type:family){var mob=type.create(h.getLevel());mob.setPos(pos.getX(),pos.getY(),pos.getZ());mob.setNoAi(true);mob.setNoGravity(true);h.getLevel().addFreshEntity(mob);mobs.add(mob);}
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(pos.getX()+2,pos.getY(),pos.getZ());cow.setNoAi(true);h.getLevel().addFreshEntity(cow);
        var bottle=thaumcraft.port.alchemy.liquid.AlchemyLiquids.BOTTLE_PROJECTILE.get().create(h.getLevel());bottle.setPos(pos.getX(),pos.getY(),pos.getZ());bottle.splash();
        h.assertTrue(cow.hasEffect(WorldEntities.TAINT),"Bottled taint no longer affects ordinary creatures");
        for(var mob:mobs){h.assertFalse(mob.hasEffect(WorldEntities.TAINT),"Bottled taint infected "+BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));mob.discard();}cow.discard();h.succeed();
    }
    private TaintIntegrationGameTests(){}
}
