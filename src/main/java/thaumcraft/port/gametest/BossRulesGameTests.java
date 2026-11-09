package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.boss.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class BossRulesGameTests {
    private static final class TestBoss extends ThaumcraftBoss {
        TestBoss(Level level){super(BossContent.ELDRITCH_GOLEM.get(),level);setNoAi(true);setNoGravity(true);}
        void spawning(int ticks){setSpawnTimer(ticks);}
    }
    private static final class TestGiant extends GiantTaintacle {
        TestGiant(Level level){super(BossContent.GIANT_TAINTACLE.get(),level);setNoAi(true);setNoGravity(true);}
        void loot(){dropCustomDeathLoot((net.minecraft.server.level.ServerLevel)level(),damageSources().generic(),true);}
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void threatSwitchesToTheActualStrongerAttacker(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(3,3,3));var boss=new TestBoss(h.getLevel());boss.setPos(origin.getX(),origin.getY(),origin.getZ());
        var first=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(3,3,4));var second=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(4,3,4));first.setNoAi(true);second.setNoAi(true);
        boss.setTarget(first);boss.hurt(boss.damageSources().mobAttack(first),5);boss.invulnerableTime=0;boss.hurt(boss.damageSources().mobAttack(second),34);boss.tickCount=19;boss.tick();
        h.assertTrue(boss.getTarget()==second,"Threat lookup retained the old attacker instead of the stronger one");
        second.discard();boss.tickCount=39;boss.tick();h.assertTrue(boss.getTarget()==null||boss.getTarget()!=second,"Removed threat target remained active");first.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void bossCapsBurstDamageAndEnragesWithTheSourceEffects(GameTestHelper h){
        var boss=new TestBoss(h.getLevel());boss.getAttribute(Attributes.ARMOR).setBaseValue(0);boss.setHealth(boss.getMaxHealth());float health=boss.getHealth();
        h.assertTrue(boss.hurt(boss.damageSources().generic(),90),"Boss rejected ordinary damage");
        h.assertValueEqual(boss.getHealth(),health-35,"Boss burst damage cap");h.assertValueEqual(boss.getAnger(),200,"Boss enrage duration");
        h.assertValueEqual(boss.getEffect(MobEffects.REGENERATION).getAmplifier(),6,"Regeneration strength");
        h.assertValueEqual(boss.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier(),9,"Attack strength");
        h.assertValueEqual(boss.getEffect(MobEffects.DIG_SPEED).getAmplifier(),2,"Source third effect is haste");
        boss.setAnger(50);boss.invulnerableTime=0;boss.hurt(boss.damageSources().generic(),60);h.assertValueEqual(boss.getAnger(),50,"Repeated burst refreshed active enrage");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void bossSpawnProtectionAndRegenerationExpireAtExpectedTicks(GameTestHelper h){
        var boss=new TestBoss(h.getLevel());boss.spawning(2);float hp=boss.getHealth();
        h.assertFalse(boss.hurt(boss.damageSources().generic(),10),"Spawning boss took damage");h.assertFalse(boss.isPushable(),"Spawning boss was pushable");
        boss.tick();boss.tick();h.assertValueEqual(boss.getSpawnTimer(),0,"Spawn timer stalled");h.assertValueEqual(boss.getHealth(),hp,"Protected health changed");
        boss.setHealth(hp-10);boss.tickCount=29;boss.tick();h.assertValueEqual(boss.getHealth(),hp-9,"Source passive regeneration");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void bossHomeSurvivesReloadWithoutDistanceDespawn(GameTestHelper h){
        var boss=new TestBoss(h.getLevel());var home=h.absolutePos(new BlockPos(3,3,3));boss.setPos(home.getX(),home.getY(),home.getZ());boss.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(home),MobSpawnType.SPAWN_EGG,null);
        var data=new CompoundTag();boss.addAdditionalSaveData(data);var loaded=new TestBoss(h.getLevel());loaded.readAdditionalSaveData(data);
        h.assertValueEqual(loaded.getRestrictCenter(),home,"Saved encounter home");h.assertValueEqual(loaded.getRestrictRadius(),24f,"Saved encounter radius");h.assertFalse(loaded.removeWhenFarAway(200000),"Boss distance despawn");h.assertTrue(loaded.requiresCustomPersistence(),"Boss custom persistence");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void giantKeepsOrdinaryChampionBonusesAndCannotSpawnNaturally(GameTestHelper h){
        var giant=new TestGiant(h.getLevel());var pos=h.absolutePos(new BlockPos(3,3,3));giant.setPos(pos.getX(),pos.getY(),pos.getZ());
        h.assertValueEqual(giant.getMaxHealth(),175f,"Giant source base health");giant.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(pos),MobSpawnType.SPAWN_EGG,null);
        h.assertValueEqual(giant.getMaxHealth(),275f,"Giant lost ordinary champion +100HP");h.assertTrue(giant.getAttributeValue(Attributes.ATTACK_DAMAGE)>=27,"Giant lost ordinary champion triple attack");h.assertFalse(giant.checkSpawnRules(h.getLevel(),MobSpawnType.NATURAL),"Giant natural spawning");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void onlyTheLastNearbyGiantDropsAPrimordialPearl(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,3,3));var first=new TestGiant(h.getLevel());var second=new TestGiant(h.getLevel());first.setPos(pos.getX(),pos.getY(),pos.getZ());second.setPos(pos.getX()+2,pos.getY(),pos.getZ());h.getLevel().addFreshEntity(first);h.getLevel().addFreshEntity(second);
        var bounds=first.getBoundingBox().inflate(8);first.loot();h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,bounds).isEmpty(),"Giant duplicated encounter pearl while a peer remained");
        second.discard();first.loot();var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,bounds);h.assertValueEqual(drops.size(),1,"Last giant pearl count");h.assertTrue(drops.getFirst().getItem().is(ThaumcraftBoss.primordialPearl().getItem()),"Last giant dropped wrong item");drops.forEach(ItemEntity::discard);first.discard();h.succeed();
    }
    private BossRulesGameTests(){}
}
