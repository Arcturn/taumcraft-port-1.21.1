package thaumcraft.port.gametest;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.champion.*;
import thaumcraft.port.mob.MobContent;
import thaumcraft.port.boss.*;
import thaumcraft.port.world.WorldEntities;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class ChampionGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void championBuffsArePersistentAndAppliedOnlyOnce(GameTestHelper h){
        var mob=champion(h,3);h.assertValueEqual(mob.getMaxHealth(),120f,"Champion health bonus");h.assertValueEqual(mob.getHealth(),45f,"Source heals25 rather than filling the new health bar");
        h.assertValueEqual(mob.getAttributeValue(Attributes.ATTACK_DAMAGE),27d,"Mighty stacks both threefold damage modifiers");
        h.assertFalse(ChampionSystem.makeChampion(mob,7,true),"Second champion conversion should be rejected");
        var tag=new CompoundTag();mob.saveWithoutId(tag);var copy=EntityType.ZOMBIE.create(h.getLevel());copy.load(tag);
        h.assertValueEqual(ChampionSystem.getModifier(copy),3,"Champion modifier missing after reload");h.assertValueEqual(copy.getMaxHealth(),120f,"Reload duplicated health buff");
        h.assertValueEqual(copy.getAttributeValue(Attributes.ATTACK_DAMAGE),27d,"Reload duplicated damage buff");
        ChampionSystem.joined(new EntityJoinLevelEvent(copy,h.getLevel()));h.assertValueEqual(ChampionSystem.getModifier(copy),3,"Joining the world rerolled a saved champion");
        h.assertTrue(copy.hasCustomName(),"Champion adjective was not saved");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void normalSpawnRollRunsOnceAndUsesSourceWeights(GameTestHelper h){
        var mob=EntityType.ZOMBIE.create(h.getLevel());mob.getRandom().setSeed(12345);ChampionSystem.joined(new EntityJoinLevelEvent(mob,h.getLevel()));
        int first=ChampionSystem.getModifier(mob);h.assertTrue(first>=-1,"Spawn decision did not mark mob");
        for(int i=0;i<500;i++)ChampionSystem.joined(new EntityJoinLevelEvent(mob,h.getLevel()));h.assertValueEqual(ChampionSystem.getModifier(mob),first,"Existing ordinary mob rerolled on load");
        h.assertFalse(ChampionSystem.qualifies(0,0,Difficulty.EASY,false,true),"Easy safe-biome champion");
        h.assertTrue(ChampionSystem.qualifies(2,0,Difficulty.HARD,false,true),"Hard inclusive three-percent chance missing");
        h.assertFalse(ChampionSystem.qualifies(3,0,Difficulty.HARD,false,true),"Hard chance too high");
        h.assertTrue(ChampionSystem.qualifies(4,3,Difficulty.HARD,false,true),"Inhabited-zombie weight missing");
        h.assertFalse(ChampionSystem.qualifies(0,3,Difficulty.HARD,true,false),"Disabled ordinary champion spawned");
        h.assertValueEqual(ChampionSystem.whitelistWeight(MobContent.INHABITED_ZOMBIE.get().create(h.getLevel())),3,"Host spawn weight");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void wardedUsesBaseHealthAndWaitsOutDamageInvulnerability(GameTestHelper h){
        var warded=champion(h,5);h.assertValueEqual(warded.getAbsorptionAmount(),10f,"Warded must use half base health, not buffed health");
        h.assertValueEqual(warded.getMaxAbsorption(),10f,"Modern absorption capacity missing");warded.setAbsorptionAmount(3);warded.tickCount=25;warded.invulnerableTime=1;ChampionSystem.tickModifier(warded);
        h.assertValueEqual(warded.getAbsorptionAmount(),3f,"Shield recharged during invulnerability");warded.invulnerableTime=0;ChampionSystem.tickModifier(warded);h.assertValueEqual(warded.getAbsorptionAmount(),4f,"Shield failed to recharge one point");
        warded.setAbsorptionAmount(10);ChampionSystem.tickModifier(warded);h.assertValueEqual(warded.getAbsorptionAmount(),10f,"Shield exceeded its source cap");
        var undying=champion(h,7);undying.setHealth(5);undying.tickCount=19;ChampionSystem.tickModifier(undying);h.assertValueEqual(undying.getHealth(),5f,"Regeneration fired early");undying.tickCount=20;ChampionSystem.tickModifier(undying);h.assertValueEqual(undying.getHealth(),6f,"Regeneration rate");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void vampireHealsOnlyForAcceptedHitsAndArmoredRespectsBypass(GameTestHelper h){
        var vampire=champion(h,11);vampire.setHealth(10);var victim=h.makeMockPlayer(GameType.SURVIVAL);var source=vampire.damageSources().mobAttack(vampire);
        h.assertTrue(victim.hurt(source,8),"First attack rejected");h.assertValueEqual(vampire.getHealth(),14f,"Vampire should heal half pre-armour damage");
        victim.hurt(source,8);h.assertValueEqual(vampire.getHealth(),14f,"Rejected same-tick hit healed vampire");victim.invulnerableTime=0;victim.hurt(source,1);h.assertValueEqual(vampire.getHealth(),16f,"Vampire minimum healing should be two");
        var armored=champion(h,2);armored.getAttribute(Attributes.ARMOR).setBaseValue(0);armored.setHealth(100);
        var attacker=EntityType.COW.create(h.getLevel());armored.hurt(armored.damageSources().mobAttack(attacker),10);h.assertTrue(Math.abs(armored.getHealth()-92.4f)<.001,"Armored24percent reduction missing");
        armored.invulnerableTime=0;armored.hurt(new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),attacker),10);
        h.assertTrue(Math.abs(armored.getHealth()-82.4f)<.001,"Armored reduced armour-bypassing magic");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void offensiveModifiersAffectPlayersAndThornsCannotRecurse(GameTestHelper h){
        var victim=h.makeMockPlayer(GameType.SURVIVAL);
        for(int type:new int[]{4,8,9,10}){
            var attacker=champion(h,type);attacker.getRandom().setSeed(14);
            for(int i=0;i<32;i++){victim.setHealth(20);victim.invulnerableTime=0;victim.hurt(attacker.damageSources().mobAttack(attacker),1);}
            h.assertTrue(switch(type){case 4->victim.hasEffect(MobEffects.WITHER);case 8->victim.isOnFire();case 9->victim.hasEffect(MobEffects.HUNGER);default->victim.hasEffect(MobEffects.POISON);},"Champion attack modifier did not reach player: "+type);
            victim.removeAllEffects();victim.clearFire();
        }
        var spined=champion(h,1);var other=champion(h,1);other.setHealth(100);spined.hurt(spined.damageSources().mobAttack(other),4);
        h.assertTrue(other.getHealth()<100&&other.getHealth()>96,"Thorns missing or recursively reflected");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void taintConversionRebuildsPassiveAiAndHealsWithFluxAfterReload(GameTestHelper h){
        var cow=EntityType.COW.create(h.getLevel());ChampionSystem.joined(new EntityJoinLevelEvent(cow,h.getLevel()));
        h.assertTrue(ChampionSystem.makeTainted(cow),"Passive creature rejected taint conversion");h.assertValueEqual(ChampionSystem.getModifier(cow),13,"Tainted ID");h.assertValueEqual(cow.getMaxHealth(),35f,"Taint conversion should add25hp");
        h.assertTrue(cow.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof MeleeAttackGoal),"Converted passive retained harmless AI");
        int goals=cow.goalSelector.getAvailableGoals().size();ChampionSystem.tickModifier(cow);h.assertValueEqual(cow.goalSelector.getAvailableGoals().size(),goals,"Taint AI stacked each tick");
        cow.setHealth(10);WorldEntities.TAINT.value().applyEffectTick(cow,0);h.assertValueEqual(cow.getHealth(),11f,"Flux taint failed to heal dynamically tainted creature");
        var saved=new CompoundTag();cow.saveWithoutId(saved);var loaded=EntityType.COW.create(h.getLevel());loaded.load(saved);ChampionSystem.joined(new EntityJoinLevelEvent(loaded,h.getLevel()));ChampionSystem.tickModifier(loaded);
        h.assertTrue(loaded.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof MeleeAttackGoal),"Taint AI lost after reload");h.assertFalse(ChampionSystem.makeTainted(loaded),"Repeated taint conversion stacked buffs");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void championDropsRequirePlayerCreditAndExcludeTaintedAndFakeKills(GameTestHelper h){
        var mob=EntityType.SPIDER.create(h.getLevel());ChampionSystem.makeChampion(mob,0,false);var realPlayer=h.makeMockPlayer(GameType.SURVIVAL);var drops=new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob,mob.damageSources().playerAttack(realPlayer),drops,false));h.assertTrue(drops.isEmpty(),"Uncredited champion dropped loot");
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob,mob.damageSources().playerAttack(GameTestPlayers.create(h)),drops,true));h.assertTrue(drops.isEmpty(),"Fake player farm received champion treasure");
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob,mob.damageSources().playerAttack(realPlayer),drops,true));h.assertValueEqual(drops.size(),1,"Credited champion must drop one bag");
        h.assertTrue(drops.getFirst().getItem().getItem()==BossLoot.lootBag(0).getItem()||drops.getFirst().getItem().getItem()==BossLoot.lootBag(1).getItem(),"Unenchanted champion produced impossible bag tier");
        drops.clear();var tainted=EntityType.SPIDER.create(h.getLevel());ChampionSystem.makeChampion(tainted,13,false);NeoForge.EVENT_BUS.post(new LivingDropsEvent(tainted,tainted.damageSources().playerAttack(realPlayer),drops,true));h.assertTrue(drops.isEmpty(),"Tainted modifier incorrectly awarded champion treasure");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void infestedSpawnsCrawlerOnlyOnAcceptedDamage(GameTestHelper h){
        var mob=champion(h,12);mob.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3))));mob.getRandom().setSeed(14);
        var attacker=EntityType.COW.create(h.getLevel());var bounds=mob.getBoundingBox().inflate(2);
        for(int i=0;i<16;i++){mob.invulnerableTime=0;mob.hurt(mob.damageSources().mobAttack(attacker),.1f);}
        var crawlers=h.getLevel().getEntitiesOfClass(thaumcraft.port.mob.TaintCrawler.class,bounds);h.assertFalse(crawlers.isEmpty(),"Infested champion did not release crawlers");int count=crawlers.size();
        for(int i=0;i<32;i++)mob.hurt(mob.damageSources().mobAttack(attacker),.1f);h.assertValueEqual(h.getLevel().getEntitiesOfClass(thaumcraft.port.mob.TaintCrawler.class,bounds).size(),count,"Rejected hits released crawlers");crawlers.forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fullyBlockedShieldHitCannotTriggerVampiricHealing(GameTestHelper h){
        var vampire=champion(h,11);vampire.setHealth(10);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemSlot(EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        player.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);for(int i=0;i<6;i++)player.tick();
        player.setYRot(0);player.setXRot(0);vampire.setPos(player.position().add(0,0,2));
        h.assertTrue(player.isBlocking(),"Shield fixture has not finished raising its shield");
        float health=player.getHealth();player.hurt(player.damageSources().mobAttack(vampire),8);
        h.assertValueEqual(player.getHealth(),health,"Frontal shield fixture failed to block attack");h.assertValueEqual(vampire.getHealth(),10f,"Fully blocked hit healed vampire");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void lowHealthFluxInfectionPreservesExistingPassiveAttackDamage(GameTestHelper h){
        var wolf=EntityType.WOLF.create(h.getLevel());double before=wolf.getAttributeValue(Attributes.ATTACK_DAMAGE);wolf.setHealth(1);wolf.getRandom().setSeed(14);
        wolf.addEffect(new net.minecraft.world.effect.MobEffectInstance(WorldEntities.TAINT,200));
        for(int i=0;i<32&&ChampionSystem.getModifier(wolf)!=13;i++){wolf.invulnerableTime=0;wolf.hurt(wolf.damageSources().magic(),.001f);}
        h.assertValueEqual(ChampionSystem.getModifier(wolf),13,"Low-health flux-tainted creature did not convert naturally");
        h.assertTrue(Math.abs(wolf.getAttributeValue(Attributes.ATTACK_DAMAGE)-(before+.5))<.0001,"Conversion replaced a wolf's native attack attribute");
        var golem=EntityType.IRON_GOLEM.create(h.getLevel());double golemAttack=golem.getAttributeValue(Attributes.ATTACK_DAMAGE);ChampionSystem.makeTainted(golem);
        h.assertTrue(Math.abs(golem.getAttributeValue(Attributes.ATTACK_DAMAGE)-(golemAttack+.5))<.0001,"Conversion replaced iron-golem damage with creature dimensions");h.succeed();
    }
    private static Zombie champion(GameTestHelper h,int type){var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setNoAi(true);zombie.setNoGravity(true);h.assertTrue(ChampionSystem.makeChampion(zombie,type,false),"Fixture conversion failed");return zombie;}
    private ChampionGameTests(){}
}
