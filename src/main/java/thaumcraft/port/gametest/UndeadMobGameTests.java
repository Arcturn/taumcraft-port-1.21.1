package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.mob.EldritchCrab;
import thaumcraft.port.mob.MobArmor;
import thaumcraft.port.mob.MobContent;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class UndeadMobGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void zombiesKeepTheirOriginalCombatAttributesAndLootTable(GameTestHelper h){
        var brainy=MobContent.BRAINY_ZOMBIE.get().create(h.getLevel());var giant=MobContent.GIANT_BRAINY_ZOMBIE.get().create(h.getLevel());var host=MobContent.INHABITED_ZOMBIE.get().create(h.getLevel());
        h.assertValueEqual(brainy.getMaxHealth(),25f,"Brainy health");h.assertValueEqual(brainy.getAttributeValue(Attributes.ATTACK_DAMAGE),5d,"Brainy damage");
        h.assertValueEqual(brainy.getArmorValue(),3,"Brainy additional armour");h.assertValueEqual(giant.getMaxHealth(),60f,"Giant health");h.assertValueEqual(host.getMaxHealth(),30f,"Host health");
        h.assertValueEqual(brainy.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE),0d,"Brainy reinforcement base chance");
        h.assertTrue(brainy.getLootTable().location().toString().equals("minecraft:entities/zombie")&&giant.getLootTable().equals(brainy.getLootTable()),"Brainy types lost inherited vanilla zombie loot");
        h.assertTrue(brainy.getType().is(EntityTypeTags.UNDEAD)&&giant.getType().is(EntityTypeTags.UNDEAD)&&host.getType().is(EntityTypeTags.UNDEAD),"Undead classification missing");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void giantAngerPreservesHitGrowthCooldownAndOriginalCollisionThreshold(GameTestHelper h){
        var giant=MobContent.GIANT_BRAINY_ZOMBIE.get().create(h.getLevel());giant.setNoAi(true);giant.setNoGravity(true);float originalWidth=giant.getBbWidth();giant.aiStep();
        h.assertValueEqual(giant.getAttributeValue(Attributes.ATTACK_DAMAGE),2d,"Initial source anger/damage changed");
        giant.hurt(h.getLevel().damageSources().generic(),0);h.assertTrue(Math.abs(giant.getAnger()-.1f)<.0001,"Even an ineffective hit should raise anger");
        giant.setAnger(.5f);giant.aiStep();h.assertValueEqual(giant.getAnger(),.5f,"Anger below one must not cool");h.assertValueEqual(giant.getBbWidth(),originalWidth,"Collision grew below the original threshold");
        giant.setAnger(1.5f);giant.aiStep();h.assertTrue(Math.abs(giant.getAnger()-1.498f)<.0001,"Giant cooldown rate");h.assertTrue(giant.getBbWidth()>originalWidth*2,"Angry giant collision did not grow");
        h.assertTrue(Math.abs(giant.getAttributeValue(Attributes.ATTACK_DAMAGE)-(7+(giant.getAnger()-1)*5))<.0001,"Anger did not adjust melee damage");
        for(int i=0;i<30;i++)giant.hurt(h.getLevel().damageSources().generic(),0);h.assertValueEqual(giant.getAnger(),2f,"Giant anger cap");
        var saved=new CompoundTag();giant.addAdditionalSaveData(saved);var loaded=MobContent.GIANT_BRAINY_ZOMBIE.get().create(h.getLevel());loaded.readAdditionalSaveData(saved);
        h.assertValueEqual(loaded.getAnger(),0f,"Source anger should be transient across reload");
        loaded.setBaby(true);loaded.setNoAi(true);loaded.setNoGravity(true);loaded.setAnger(1.5f);loaded.aiStep();
        h.assertTrue(loaded.getBbWidth()>originalWidth*2,"Angry baby giant retained the vanilla half-sized collision");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void crabShellBreaksAtHalfHealthAndPoisonIsRejected(GameTestHelper h){
        var crab=MobContent.ELDRITCH_CRAB.get().create(h.getLevel());crab.setHelm(true);h.assertValueEqual(crab.getArmorValue(),5,"Armoured crab protection");
        h.assertFalse(crab.addEffect(new MobEffectInstance(MobEffects.POISON,100)),"Arthropod accepted poison");h.assertTrue(crab.getType().is(EntityTypeTags.ARTHROPOD),"Crab lost arthropod classification");
        crab.setHealth(10);crab.hurt(h.getLevel().damageSources().generic(),0);h.assertFalse(crab.hasHelm(),"Half-health crab retained its shell");h.assertValueEqual(crab.getArmorValue(),0,"Broken shell still protects");h.assertValueEqual(crab.getAttributeValue(Attributes.MOVEMENT_SPEED),.3d,"Broken shell speed");
        var tag=new CompoundTag();crab.addAdditionalSaveData(tag);var loaded=MobContent.ELDRITCH_CRAB.get().create(h.getLevel());loaded.setHelm(true);loaded.readAdditionalSaveData(tag);h.assertFalse(loaded.hasHelm(),"Broken shell returned on reload");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void onlyAnAirborneUnshelledCrabLatchesAndBites(GameTestHelper h){
        var origin=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));var crab=MobContent.ELDRITCH_CRAB.get().create(h.getLevel());crab.setNoAi(true);crab.setNoGravity(true);
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);victim.setPos(origin);crab.setTarget(victim);crab.setPos(origin.add(0,victim.getBbHeight()*.75,0));crab.setOnGround(false);crab.setHelm(true);crab.tick();
        h.assertFalse(crab.isPassenger(),"Helmed crab latched onto its victim");h.assertValueEqual(victim.getHealth(),victim.getMaxHealth(),"Helmed passenger attack fired");
        crab.setHelm(false);crab.setPos(origin.add(0,victim.getBbHeight()*.75,0));crab.setDeltaMovement(Vec3.ZERO);crab.setOnGround(false);crab.tick();
        h.assertTrue(victim.getHealth()<victim.getMaxHealth(),"Airborne crab did not latch and bite");h.assertTrue(crab.getAttackTime()>=10&&crab.getAttackTime()<=19,"Passenger bite cooldown differs from source");
        // Source permits a 20% immediate dismount. Reattach to verify the modern attachment offset.
        crab.startRiding(victim,true);h.assertTrue(crab.getVehicle()==victim,"Crab cannot ride a non-mount mob");h.assertValueEqual(crab.getVehicleAttachmentPoint(victim).y,-.5d,"Crab is not positioned above the vehicle attachment");
        crab.stopRiding();h.assertValueEqual(crab.getRiding(),-1,"Dismount left a stale rider reference");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void hostUsesRealDamageableEquipmentAndReleasesExactlyOneCrabWithoutDrops(GameTestHelper h){
        var host=h.spawn(MobContent.INHABITED_ZOMBIE.get(),new BlockPos(3,3,3));host.setNoAi(true);host.setNoGravity(true);
        host.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MobArmor.HELM.get()));host.setItemSlot(EquipmentSlot.CHEST,new ItemStack(MobArmor.CHEST.get()));host.setItemSlot(EquipmentSlot.LEGS,new ItemStack(MobArmor.LEGS.get()));host.tick();
        h.assertValueEqual(host.getArmorValue(),15,"Host plate is not applied as real equipment");h.assertValueEqual(host.getItemBySlot(EquipmentSlot.HEAD).getMaxDamage(),198,"Original crimson helm durability changed");
        // Vanilla mobs only wear down helmets in sunlight; damage belongs to the real equipped stack.
        host.getItemBySlot(EquipmentSlot.HEAD).setDamageValue(37);
        var saved=new CompoundTag();host.addAdditionalSaveData(saved);var loaded=MobContent.INHABITED_ZOMBIE.get().create(h.getLevel());loaded.readAdditionalSaveData(saved);
        h.assertTrue(loaded.hasHeadArmor()&&loaded.hasChestArmor()&&loaded.hasLegArmor(),"Host equipment was not persisted");h.assertValueEqual(loaded.getItemBySlot(EquipmentSlot.HEAD).getDamageValue(),37,"Armour damage was not persisted");
        var area=host.getBoundingBox().inflate(4);host.invulnerableTime=0;host.hurt(h.getLevel().damageSources().genericKill(),1000);host.tick();var crabs=h.getLevel().getEntitiesOfClass(EldritchCrab.class,area);
        h.assertTrue(host.isRemoved()&&crabs.size()==1&&crabs.getFirst().hasHelm(),"Dying host failed to release exactly one shelled crab");h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Host dropped ordinary zombie loot or armour");
        host.tick();h.assertValueEqual(h.getLevel().getEntitiesOfClass(EldritchCrab.class,area).size(),1,"Repeated death tick released a second crab");crabs.forEach(EldritchCrab::discard);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void hostSpawnAlwaysEquipsTheCrimsonHelmet(GameTestHelper h){
        var host=MobContent.INHABITED_ZOMBIE.get().create(h.getLevel());host.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3))));host.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(host.blockPosition()),MobSpawnType.SPAWN_EGG,null);
        h.assertTrue(host.hasHeadArmor(),"Vanilla initialization replaced the inhabited zombie's helmet");h.succeed();
    }
    private UndeadMobGameTests(){}
}
