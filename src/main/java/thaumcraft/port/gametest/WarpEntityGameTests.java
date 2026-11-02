package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.warp.entity.WarpEntities;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WarpEntityGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void hallucinationsArePrivateHarmlessPersistedAndExpire(GameTestHelper h){
        var spider=h.spawn(WarpEntities.MIND_SPIDER.get(),new BlockPos(3,2,3));spider.setNoAi(true);spider.setHarmless(true);spider.setViewer("observer");
        var cow=h.spawn(EntityType.COW,new BlockPos(4,2,3));cow.setNoAi(true);float hp=cow.getHealth();
        h.assertTrue(spider.visibleTo("observer")&&!spider.visibleTo("other"),"Hallucination viewer was not isolated");
        h.assertFalse(spider.doHurtTarget(cow),"Hallucination attacked");h.assertValueEqual(cow.getHealth(),hp,"Hallucination dealt damage");
        var saved=new CompoundTag();spider.addAdditionalSaveData(saved);var loaded=WarpEntities.MIND_SPIDER.get().create(h.getLevel());loaded.readAdditionalSaveData(saved);
        h.assertTrue(loaded.isHarmless()&&loaded.visibleTo("observer")&&!loaded.visibleTo("other"),"Reload lost hallucination ownership");
        spider.tickCount=1201;spider.tick();h.assertTrue(spider.isRemoved(),"Hallucination survived beyond sixty seconds");
        loaded.discard();cow.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void realMindSpidersAreVisibleHarmfulAndDoNotExpire(GameTestHelper h){
        var spider=h.spawn(WarpEntities.MIND_SPIDER.get(),new BlockPos(3,2,3));spider.setNoAi(true);
        var cow=h.spawn(EntityType.COW,new BlockPos(4,2,3));cow.setNoAi(true);float hp=cow.getHealth();
        h.assertTrue(spider.visibleTo("anyone"),"Real spider is private");h.assertTrue(spider.doHurtTarget(cow)&&cow.getHealth()<hp,"Real spider cannot damage");
        spider.tickCount=1200;spider.tick();h.assertFalse(spider.isRemoved(),"Real spider used hallucination expiry");
        h.assertValueEqual(spider.getMaxHealth(),1f,"Mind spider health differs from source");spider.discard();cow.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void guardianPreservesAttributesUndeadFactionHomeAndRangedBranches(GameTestHelper h){
        var guardian=h.spawn(WarpEntities.ELDRITCH_GUARDIAN.get(),new BlockPos(3,2,3));guardian.setNoAi(true);
        var target=h.spawn(EntityType.COW,new BlockPos(3,2,12));target.setNoAi(true);
        h.assertValueEqual(guardian.getMaxHealth(),50f,"Guardian health");h.assertValueEqual(guardian.getAttributeValue(Attributes.ATTACK_DAMAGE),7d,"Guardian damage");
        h.assertTrue(guardian.getType().is(EntityTypeTags.UNDEAD),"Guardian lacks undead classification");
        var spider=WarpEntities.MIND_SPIDER.get().create(h.getLevel());h.assertTrue(guardian.isAlliedTo(spider),"Eldritch faction lost");
        guardian.restrictTo(h.absolutePos(new BlockPos(3,2,3)),16);var tag=new CompoundTag();guardian.addAdditionalSaveData(tag);
        var loaded=WarpEntities.ELDRITCH_GUARDIAN.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.hasRestriction()&&loaded.getRestrictCenter().equals(guardian.getRestrictCenter())&&!loaded.removeWhenFarAway(10000),"Guardian home/despawn persistence");
        for(int i=0;i<100&&!target.hasEffect(MobEffects.WITHER);i++)guardian.performRangedAttack(target,1);
        h.assertTrue(target.hasEffect(MobEffects.WITHER)&&target.getEffect(MobEffects.WITHER).getDuration()==400,"Sonic attack missing source wither");
        var orbs=h.getLevel().getEntitiesOfClass(thaumcraft.port.warp.entity.EldritchOrb.class,guardian.getBoundingBox().inflate(3));
        h.assertTrue(!orbs.isEmpty()&&orbs.stream().allMatch(o->o.getOwner()==guardian&&o.isNoGravity()),"Orb branch missing owner or gravity-free flight");
        orbs.forEach(net.minecraft.world.entity.Entity::discard);guardian.discard();loaded.discard();target.discard();spider.discard();h.succeed();
    }
    private WarpEntityGameTests(){}
}
