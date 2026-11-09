package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.auromancy.effect.FocusEffectContent;
import thaumcraft.port.boss.BossContent;
import thaumcraft.port.boss.GolemOrb;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class EldritchBossGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void golemLethalHitOpensOneProtectedHealingPhaseAndPersists(GameTestHelper h){
        var golem=BossContent.ELDRITCH_GOLEM.get().create(h.getLevel());golem.setNoAi(true);golem.setNoGravity(true);golem.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))));
        h.assertValueEqual(golem.getMaxHealth(),400f,"Golem health");h.assertValueEqual(golem.getArmorValue(),6,"Golem armour");golem.setHealth(30);
        h.assertFalse(golem.hurt(h.getLevel().damageSources().generic(),31),"Transitioning hit was not cancelled");h.assertTrue(golem.isHeadless(),"Raw lethal hit did not break the head before the shared damage cap");
        h.assertValueEqual(golem.getHealth(),30f,"Head loss consumed health");h.assertValueEqual(golem.getSpawnTimer(),100,"Head loss recovery duration");h.assertTrue(Math.abs(golem.getEyeHeight()-3.33f)<.001,"Broken golem eye height");
        h.assertFalse(golem.hurt(h.getLevel().damageSources().generic(),1),"Recovery phase accepted damage");golem.tick();h.assertTrue(golem.getHealth()>=32,"Golem did not heal two health during recovery");
        var saved=new CompoundTag();golem.addAdditionalSaveData(saved);var loaded=BossContent.ELDRITCH_GOLEM.get().create(h.getLevel());loaded.readAdditionalSaveData(saved);
        h.assertTrue(loaded.isHeadless(),"Head returned after reload");h.assertValueEqual(loaded.getSpawnTimer(),99,"Recovery restarted or disappeared after reload");h.assertValueEqual(loaded.getBeamCharge(),golem.getBeamCharge(),"Cannon charge was not persisted");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void golemChargesFor150TicksThenSpends15To19PerShot(GameTestHelper h){
        var golem=BossContent.ELDRITCH_GOLEM.get().create(h.getLevel());golem.setNoAi(true);golem.setNoGravity(true);golem.setHeadless(true);golem.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,2,2))));
        for(int i=0;i<149;i++)golem.tick();h.assertTrue(golem.isChargingBeam(),"Golem fired before a complete recharge");h.assertValueEqual(golem.getBeamCharge(),149,"Charge rate");
        golem.tick();h.assertFalse(golem.isChargingBeam(),"Golem never leaves its charge phase");h.assertValueEqual(golem.getBeamCharge(),150,"Charge maximum");
        var cow=h.spawn(EntityType.COW,new BlockPos(5,2,2));cow.setNoAi(true);cow.setNoGravity(true);golem.performRangedAttack(cow,1);
        h.assertTrue(golem.getBeamCharge()>=131&&golem.getBeamCharge()<=135,"Shot did not consume 15–19 charge");
        var orbs=h.getLevel().getEntitiesOfClass(GolemOrb.class,golem.getBoundingBox().inflate(5));h.assertValueEqual(orbs.size(),1,"Golem did not create exactly one shot");h.assertFalse(orbs.getFirst().isRed(),"Golem shot used the cultist colour");h.assertTrue(orbs.getFirst().getTarget()==cow,"Homing shot lost target");orbs.forEach(GolemOrb::discard);cow.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void golemMeleeUsesThreeQuarterDamageAndTenTickSwingCooldown(GameTestHelper h){
        var golem=BossContent.ELDRITCH_GOLEM.get().create(h.getLevel());golem.setNoAi(true);golem.setNoGravity(true);golem.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))));
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);victim.setPos(golem.position().add(1,0,0));
        h.assertTrue(golem.doHurtTarget(victim),"Golem melee failed");h.assertValueEqual(victim.getHealth(),2.5f,"Golem strike must use 75 percent of attack attribute");h.assertValueEqual(golem.getAttackTimer(),10,"Melee swing timer");
        h.assertFalse(golem.doHurtTarget(victim),"Golem attacked twice in the same swing");h.assertTrue(victim.getDeltaMovement().y>=.2,"Strike omitted upward impulse");
        for(int i=0;i<10;i++)golem.aiStep();h.assertValueEqual(golem.getAttackTimer(),0,"Melee cooldown did not expire");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void wardenHasSourceShieldAndRegeneratesOnlyOutsideHitCooldown(GameTestHelper h){
        var warden=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());warden.setNoAi(true);warden.setNoGravity(true);warden.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))));
        warden.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(warden.blockPosition()),MobSpawnType.SPAWN_EGG,null);
        h.assertValueEqual(warden.getAbsorptionAmount(),264f,"Warden starting shield is not 66 percent of base health");h.assertValueEqual(warden.getSpawnTimer(),150,"Warden emergence duration");h.assertValueEqual(warden.getArmorValue(),4,"Warden armour");
        warden.setAbsorptionAmount(100);warden.tickCount=25;warden.invulnerableTime=0;warden.tick();h.assertValueEqual(warden.getAbsorptionAmount(),101f,"Shield did not regenerate on its 25-tick cadence");
        warden.tickCount=50;warden.invulnerableTime=20;warden.tick();h.assertValueEqual(warden.getAbsorptionAmount(),101f,"Shield regenerated while hit resistance remained");
        warden.setTitle(999);h.assertValueEqual(warden.getTitleIndex(),14,"Invalid title was not safely clamped");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void wardenShieldBreakTriggersOneFrenzyAndPlacesSapRings(GameTestHelper h){
        var warden=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());warden.setNoAi(true);warden.setNoGravity(true);var center=h.absolutePos(new BlockPos(2,2,2));warden.setPos(Vec3.atBottomCenterOf(center));
        var ring=center.offset(3,0,0);h.getLevel().setBlockAndUpdate(ring.below(),Blocks.STONE.defaultBlockState());h.getLevel().removeBlock(ring,false);
        warden.setAbsorptionAmount(1);h.assertTrue(warden.hurt(h.getLevel().damageSources().magic(),2),"Shield-breaking hit rejected");
        h.assertTrue(warden.isFieldFrenzy(),"Empty shield failed to trigger frenzy");h.assertValueEqual(warden.getFieldFrenzyCounter(),150,"Frenzy duration");h.assertFalse(warden.hurt(h.getLevel().damageSources().generic(),10),"Frenzy must be invulnerable");
        for(int i=0;i<31;i++)warden.aiStep();h.assertTrue(h.getLevel().getBlockState(ring).is(FocusEffectContent.SAP.get()),"First expanding sap ring was not placed on a supported air block");
        var saved=new CompoundTag();warden.addAdditionalSaveData(saved);var loaded=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());loaded.readAdditionalSaveData(saved);
        h.assertTrue(loaded.isFieldFrenzy(),"Reload forgot that frenzy had already triggered");h.assertValueEqual(loaded.getFieldFrenzyCounter(),119,"Frenzy progress was not saved");
        for(int i=0;i<119;i++)warden.aiStep();warden.invulnerableTime=0;warden.setAbsorptionAmount(0);warden.hurt(h.getLevel().damageSources().magic(),1);h.assertValueEqual(warden.getFieldFrenzyCounter(),0,"Second shield break repeated a one-time frenzy");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void wardenSonicAttackAddsBothDebuffsWarpAndKnockback(GameTestHelper h){
        var warden=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());warden.setNoAi(true);warden.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))));
        var player=GameTestPlayers.create(h);player.setPos(warden.position().add(0,0,2));var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());int previous=state.temporaryWarp();
        warden.performSonicAttack(player);h.assertTrue(player.hasEffect(MobEffects.WITHER)&&player.hasEffect(MobEffects.WEAKNESS),"Sonic attack omitted its paired debuffs");
        h.assertValueEqual(player.getEffect(MobEffects.WITHER).getDuration(),400,"Sonic wither duration");h.assertTrue(state.temporaryWarp()-previous>=3&&state.temporaryWarp()-previous<=5,"Warden sonic warp must be 3–5 temporary points");h.assertTrue(player.getDeltaMovement().horizontalDistanceSqr()>1,"Sonic push missing");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=60)
    public static void golemOrbHomesDeflectsAndDealsMagicDamageOnCollision(GameTestHelper h){
        var owner=h.spawn(BossContent.ELDRITCH_GOLEM.get(),new BlockPos(1,2,3));owner.setNoAi(true);owner.setNoGravity(true);
        var victim=h.spawn(EntityType.COW,new BlockPos(5,2,3));victim.setNoAi(true);victim.setNoGravity(true);victim.getAttribute(Attributes.ARMOR).setBaseValue(30);
        var orb=BossContent.GOLEM_ORB.get().create(h.getLevel());orb.setOwner(owner);orb.setTarget(victim);orb.setPos(victim.position().add(-1,.7,0));orb.setDeltaMovement(.25,0,0);h.getLevel().addFreshEntity(orb);
        var saved=new CompoundTag();orb.addAdditionalSaveData(saved);var reloaded=BossContent.GOLEM_ORB.get().create(h.getLevel());reloaded.readAdditionalSaveData(saved);h.assertTrue(reloaded.getTarget()==victim,"Orb target UUID failed to survive reload");
        var deflector=GameTestPlayers.create(h);deflector.setYRot(90);deflector.setXRot(0);h.assertTrue(reloaded.hurt(h.getLevel().damageSources().playerAttack(deflector),1),"Orb was not deflectable");h.assertTrue(reloaded.getDeltaMovement().x<-.8,"Deflection did not follow attacker's look vector");h.assertTrue(reloaded.getTarget()==victim,"Deflection incorrectly retargeted the homing orb");
        // GameTest chunks may contain entities outside the server's active entity-ticking range.
        // Drive the real projectile movement/raycast path explicitly, as a server tick does.
        for(int i=0;i<10&&!orb.isRemoved();i++){++orb.tickCount;orb.tick();}
        h.assertTrue(orb.isRemoved(),"Orb did not collide with target; orb="+orb.position()+", target="+victim.position());h.assertValueEqual(victim.getHealth(),4f,"Orb damage must be 60 percent magic, bypassing armour");owner.discard();victim.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void redOrbLivesLongerAndSavedAgeCannotResetItsLifetime(GameTestHelper h){
        var normal=BossContent.GOLEM_ORB.get().create(h.getLevel());var red=BossContent.GOLEM_ORB.get().create(h.getLevel());normal.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,3,2))));red.setPos(normal.position());red.setRed(true);
        normal.tickCount=160;red.tickCount=160;++normal.tickCount;normal.tick();++red.tickCount;red.tick();h.assertTrue(normal.isRemoved(),"Normal orb exceeded160 ticks");h.assertFalse(red.isRemoved(),"Red orb lost its longer lifetime");
        red.tickCount=240;var tag=new CompoundTag();red.addAdditionalSaveData(tag);var loaded=BossContent.GOLEM_ORB.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);loaded.setPos(red.position());h.assertTrue(loaded.isRed(),"Red orb colour lost on reload");++loaded.tickCount;loaded.tick();h.assertTrue(loaded.isRemoved(),"Reload reset the red orb's age");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void wardenReturnsToPersistedHomeWithoutEnteringSolidBlocks(GameTestHelper h){
        var warden=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());warden.setNoAi(true);warden.setNoGravity(true);var home=h.absolutePos(new BlockPos(3,2,3));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(home.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        warden.setPos(Vec3.atBottomCenterOf(home.offset(4,0,0)));warden.restrictTo(home,24);var tag=new CompoundTag();warden.addAdditionalSaveData(tag);
        var loaded=BossContent.ELDRITCH_WARDEN.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);loaded.setPos(warden.position());
        h.assertTrue(loaded.teleportHome(),"Warden failed to return to a clear supported home");h.assertTrue(loaded.position().distanceTo(Vec3.atBottomCenterOf(home))<.2,"Home teleport changed the encounter center");
        h.assertTrue(h.getLevel().noCollision(loaded),"Home teleport placed the warden inside solid terrain");
        h.assertFalse(loaded.hurt(h.getLevel().damageSources().wither(),5),"Warden accepted wither damage");h.assertFalse(loaded.hurt(h.getLevel().damageSources().drown(),5),"Warden accepted drowning damage");h.succeed();
    }
    private EldritchBossGameTests(){}
}
