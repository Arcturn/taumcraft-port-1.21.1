package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.mob.*;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class TaintedMobGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void crawlerNeedsRealFibreAttachmentAndRespectsLiquidAndMobGriefing(GameTestHelper h){
        var level=h.getLevel();var crawler=h.spawn(MobContent.CRAWLER.get(),new BlockPos(4,3,4));crawler.setNoAi(true);
        var pos=crawler.blockPosition();var rule=level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);boolean old=rule.get();
        try{
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());rule.set(true,level.getServer());crawler.leaveFibre();
            var fibre=level.getBlockState(pos);h.assertTrue(fibre.is(WorldContent.block("taint_fibre"))&&fibre.getValue(CrystalClusterBlock.FACES.get(Direction.DOWN)),"Crawler fibre did not attach to its solid floor");
            level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());crawler.leaveFibre();h.assertTrue(level.getBlockState(pos).is(Blocks.WATER),"Crawler replaced liquid with fibre");
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());rule.set(false,level.getServer());crawler.leaveFibre();h.assertTrue(level.isEmptyBlock(pos),"Crawler ignored mobGriefing");
            rule.set(true,level.getServer());level.setBlockAndUpdate(pos.below(),WorldContent.block("taint_soil").defaultBlockState());crawler.leaveFibre();h.assertTrue(level.isEmptyBlock(pos),"Tainted floor was treated as a valid fibre attachment");
        }finally{rule.set(old,level.getServer());crawler.discard();}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void taintedFactionHealsFromFluxAndRejectsFriendlyTargets(GameTestHelper h){
        var crawler=h.spawn(MobContent.CRAWLER.get(),new BlockPos(3,3,3));var tentacle=h.spawn(MobContent.TAINTACLE.get(),new BlockPos(5,3,3));var seed=h.spawn(WorldEntities.SEED.get(),new BlockPos(8,3,3));var slime=h.spawn(WorldEntities.SLIME.get(),new BlockPos(10,3,3));
        crawler.setNoAi(true);tentacle.setNoAi(true);seed.setNoAi(true);slime.setNoAi(true);slime.setSize(2,true);
        crawler.setHealth(3);WorldEntities.TAINT.get().applyEffectTick(crawler,0);h.assertValueEqual(crawler.getHealth(),4f,"Flux taint damaged its crawler faction");
        slime.setHealth(2);WorldEntities.TAINT.get().applyEffectTick(slime,0);h.assertValueEqual(slime.getHealth(),3f,"Flux taint damaged the existing thaumic slime");
        crawler.setTarget(seed);seed.setTarget(tentacle);tentacle.setTarget(slime);slime.setTarget(crawler);
        h.assertTrue(crawler.getTarget()==null&&seed.getTarget()==null&&tentacle.getTarget()==null&&slime.getTarget()==null,"Tainted mob accepted a friendly target");
        h.assertTrue(crawler.isAlliedTo(seed)&&seed.isAlliedTo(crawler)&&tentacle.isAlliedTo(slime)&&slime.isAlliedTo(tentacle),"Taint faction alliance was not symmetric");
        h.assertFalse(crawler.canAttackType(WorldEntities.SLIME.get()),"Taint faction type filter omitted thaumic slime");
        h.assertFalse(crawler.doHurtTarget(seed)||tentacle.doHurtTarget(crawler),"A direct attack harmed another tainted mob");
        crawler.discard();tentacle.discard();seed.discard();slime.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void tentacleIsRootedCanFallAndSummonsOnlyOverTaintedTerrain(GameTestHelper h){
        var tentacle=h.spawn(MobContent.TAINTACLE.get(),new BlockPos(3,4,3));tentacle.setNoAi(true);var original=tentacle.position();
        tentacle.move(MoverType.SELF,new Vec3(2,1,3));h.assertValueEqual(tentacle.position(),original,"Rooted tentacle moved horizontally or rose");
        tentacle.move(MoverType.SELF,new Vec3(0,-.25,0));h.assertTrue(tentacle.getY()<original.y,"Tentacle could not fall downward");
        var victim=h.spawn(EntityType.COW,new BlockPos(10,3,10));victim.setNoAi(true);h.getLevel().setBlockAndUpdate(victim.blockPosition().below(),Blocks.STONE.defaultBlockState());
        h.assertTrue(tentacle.spawnTentacles(victim)==null,"Tentacle summoned on ordinary terrain");
        h.getLevel().setBlockAndUpdate(victim.blockPosition().below(),WorldContent.block("taint_soil").defaultBlockState());var small=tentacle.spawnTentacles(victim);
        h.assertTrue(small!=null,"Tentacle failed to summon on tainted terrain");h.assertValueEqual(small.getMaxHealth(),5f,"Small tentacle health differs from source");h.assertValueEqual(small.getAttributeValue(Attributes.ATTACK_DAMAGE),2d,"Small tentacle attack differs from source");
        h.assertTrue(small.spawnTentacles(victim)==null,"Small tentacle recursively summoned another small tentacle");small.discard();tentacle.discard();victim.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void existingSeedSummonsNewSmallTentaclesAtItsTwentyTickInterval(GameTestHelper h){
        var seed=h.spawn(WorldEntities.SEED.get(),new BlockPos(3,3,3));seed.setNoAi(true);seed.setNoGravity(true);
        var victim=h.spawn(EntityType.COW,new BlockPos(10,3,3));victim.setNoAi(true);victim.setNoGravity(true);seed.setTarget(victim);
        h.getLevel().setBlockAndUpdate(victim.blockPosition().below(),Blocks.STONE.defaultBlockState());h.assertTrue(seed.spawnTentacles(victim)==null,"Seed summoned on ordinary terrain");
        h.getLevel().setBlockAndUpdate(victim.blockPosition().below(),WorldContent.block("taint_soil").defaultBlockState());
        seed.tickCount=19;seed.tick();h.assertTrue(h.getLevel().getEntitiesOfClass(TaintacleSmall.class,victim.getBoundingBox().inflate(3)).isEmpty(),"Seed summoned before its twenty-tick interval");
        seed.tickCount=20;seed.tick();var minions=h.getLevel().getEntitiesOfClass(TaintacleSmall.class,victim.getBoundingBox().inflate(3));h.assertValueEqual(minions.size(),1,"Existing seed did not summon the new small tentacle at twenty ticks");
        for(var minion:minions)minion.discard();seed.discard();victim.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fatalTentacleDecayBypassesRecentHitsWithoutDroppingACrystal(GameTestHelper h){
        var tentacle=h.spawn(MobContent.TAINTACLE.get(),new BlockPos(3,4,3));tentacle.setNoAi(true);tentacle.setNoGravity(true);
        tentacle.setHealth(1);tentacle.invulnerableTime=20;tentacle.tickCount=20;tentacle.tick();
        h.assertFalse(tentacle.isAlive(),"Recent hit immunity postponed off-taint decay");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,tentacle.getBoundingBox().inflate(2)).isEmpty(),"Off-taint decay dropped a crystal instead of using source damageEntity semantics");
        tentacle.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void smallTentacleLifetimePersistsAndExpiresWithoutLoot(GameTestHelper h){
        var small=h.spawn(MobContent.TAINTACLE_SMALL.get(),new BlockPos(3,3,3));small.setNoAi(true);var tag=new CompoundTag();small.addAdditionalSaveData(tag);tag.putInt("Lifetime",0);small.readAdditionalSaveData(tag);
        h.assertValueEqual(small.remainingLifetime(),0,"Small tentacle lifetime was reset on load");small.invulnerableTime=20;small.tick();h.assertFalse(small.isAlive(),"Expired small tentacle survived its magic damage");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,small.getBoundingBox().inflate(2)).isEmpty(),"Small tentacle dropped loot");
        h.assertFalse(small.checkSpawnRules(h.getLevel(),MobSpawnType.NATURAL),"Small tentacle can spawn naturally");small.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void swarmAttackHasWeaknessCooldownAndPreservesVictimVelocity(GameTestHelper h){
        var swarm=h.spawn(MobContent.SWARM.get(),new BlockPos(3,3,3));swarm.setNoAi(true);swarm.setIsSummoned(true);var victim=h.spawn(EntityType.COW,new BlockPos(4,3,3));victim.setNoAi(true);
        var velocity=new Vec3(.2,.3,-.1);victim.setDeltaMovement(velocity);h.assertTrue(swarm.attackEntity(victim),"Swarm failed to attack overlapping close target");
        h.assertTrue(victim.hasEffect(MobEffects.WEAKNESS)&&victim.getEffect(MobEffects.WEAKNESS).getDuration()==100,"Swarm bite did not apply source weakness for five seconds");
        h.assertValueEqual(victim.getDeltaMovement(),velocity,"Swarm attack applied knockback");h.assertFalse(victim.hurtMarked,"Swarm left victim velocity marked for synchronization");h.assertValueEqual(victim.lastHurtByPlayerTime,100,"Summoned swarm did not mark victim for player-attributed loot");
        float health=victim.getHealth();h.assertFalse(swarm.attackEntity(victim),"Swarm ignored its attack cooldown");h.assertValueEqual(victim.getHealth(),health,"Cooldown attack damaged victim");swarm.discard();victim.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void summonedSwarmDecaysWithoutTargetAndRetainsDamageBonus(GameTestHelper h){
        var swarm=h.spawn(MobContent.SWARM.get(),new BlockPos(3,3,3));swarm.setNoAi(true);swarm.setIsSummoned(true);swarm.setDamageBonus(4);
        var tag=new CompoundTag();swarm.addAdditionalSaveData(tag);var copy=MobContent.SWARM.get().create(h.getLevel());copy.readAdditionalSaveData(tag);
        h.assertTrue(copy.getIsSummoned(),"Summoned swarm flag was lost on save");h.assertValueEqual(copy.getAttributeValue(Attributes.ATTACK_DAMAGE),6d,"Swarm damage bonus was lost on save");
        swarm.updateSwarm();h.assertValueEqual(swarm.getHealth(),25f,"Targetless summoned swarm did not decay by five health");h.assertFalse(swarm.causeFallDamage(100,1,h.getLevel().damageSources().fall()),"Flying swarm suffered fall damage");h.assertFalse(swarm.isPushable(),"Swarm accepted ordinary push collision");swarm.discard();h.succeed();
    }
    private TaintedMobGameTests(){}
}
