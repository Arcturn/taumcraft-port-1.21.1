package thaumcraft.port.gametest;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.boss.*;
import thaumcraft.port.infusion.bauble.InfusionBaubles;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CultistBossGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void praetorEquipsRealGearKeepsTitleAndRegeneratesCultists(GameTestHelper h){
        var leader=h.spawn(BossContent.CULTIST_LEADER.get(),new BlockPos(3,3,3));leader.setNoGravity(true);
        leader.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(leader.blockPosition()),MobSpawnType.SPAWN_EGG,null);
        leader.tick();
        h.assertTrue(leader.getItemBySlot(EquipmentSlot.HEAD).is(BossGear.PRAETOR_HELM.get())&&leader.getItemBySlot(EquipmentSlot.CHEST).is(BossGear.PRAETOR_CHEST.get())&&leader.getItemBySlot(EquipmentSlot.LEGS).is(BossGear.PRAETOR_LEGS.get())&&leader.getItemBySlot(EquipmentSlot.FEET).is(BossGear.CRIMSON_BOOTS.get()),"Praetor armour is missing");
        h.assertValueEqual(leader.getArmorValue(),18,"Praetor real equipment protection");h.assertValueEqual(leader.getItemBySlot(EquipmentSlot.HEAD).getMaxDamage(),330,"Praetor durability");
        var ally=h.spawn(WorldEntities.KNIGHT.get(),new BlockPos(5,3,3));ally.setNoAi(true);ally.setNoGravity(true);ally.setHealth(5);
        leader.tick();h.assertTrue(ally.hasEffect(MobEffects.REGENERATION),"Leader did not give regeneration to nearby cultist");h.assertValueEqual(ally.getEffect(MobEffects.REGENERATION).getAmplifier(),1,"Cultist aura must be regeneration II");
        var tag=new CompoundTag();leader.addAdditionalSaveData(tag);var loaded=BossContent.CULTIST_LEADER.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);h.assertValueEqual(loaded.getTitle(),leader.getTitle(),"Leader title changed on reload");
        h.assertTrue(leader.isAlliedTo(ally)&&!leader.canAttackType(WorldEntities.CLERIC.get()),"Leader attacked its own faction");leader.discard();ally.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void praetorFiresRedHomingOrbsAndDropsBagWithoutPearl(GameTestHelper h){
        var leader=h.spawn(BossContent.CULTIST_LEADER.get(),new BlockPos(3,4,3));leader.setNoAi(true);leader.setNoGravity(true);
        var target=h.spawn(EntityType.COW,new BlockPos(6,4,3));target.setNoAi(true);leader.performRangedAttack(target,1);
        var orbs=h.getLevel().getEntitiesOfClass(GolemOrb.class,leader.getBoundingBox().inflate(4));
        h.assertTrue(orbs.stream().anyMatch(orb->orb.isRed()&&orb.getOwner()==leader&&orb.getTarget()==target),"Leader did not launch a red homing projectile");
        leader.setHealth(0);leader.die(h.getLevel().damageSources().genericKill());var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,leader.getBoundingBox().inflate(3));
        h.assertValueEqual(drops.stream().filter(drop->drop.getItem().is(BossLoot.RARE.get())).mapToInt(drop->drop.getItem().getCount()).sum(),1,"Leader rarebag drop");
        h.assertFalse(drops.stream().anyMatch(drop->drop.getItem().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primordial_pearl")))) ,"Leader incorrectly inherited an eldritch boss pearl");
        drops.forEach(Entity::discard);orbs.forEach(Entity::discard);target.discard();leader.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void portalPersistsProgressAndStopsRegeneratingAtTwelfthStage(GameTestHelper h){
        var portal=BossContent.CULTIST_PORTAL_GREATER.get().create(h.getLevel());portal.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3))));portal.setHealth(250);
        progress(portal,11,50);portal.tick();h.assertValueEqual(portal.getHealth(),251f,"Early portal regeneration");h.assertValueEqual(portal.getStageCounter(),49,"Portal timer");
        var tag=new CompoundTag();portal.addAdditionalSaveData(tag);var loaded=BossContent.CULTIST_PORTAL_GREATER.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);
        h.assertValueEqual(loaded.getStage(),11,"Portal stage lost on reload");h.assertValueEqual(loaded.getStageCounter(),49,"Portal countdown reset on reload");
        progress(portal,12,50);portal.tick();h.assertValueEqual(portal.getHealth(),251f,"Portal healed after twelfth stage");
        var origin=portal.position();portal.move(MoverType.SELF,new Vec3(2,3,4));h.assertValueEqual(portal.position(),origin,"Portal moved");
        h.assertFalse(portal.addEffect(new MobEffectInstance(MobEffects.POISON,100)),"Portal accepted potion");h.assertTrue(portal.fireImmune(),"Portal lost fire immunity");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void greaterPortalSpawnsOneLeaderThenPaysHealthForMinions(GameTestHelper h){
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"portal-test"));
        var portal=h.spawn(BossContent.CULTIST_PORTAL_GREATER.get(),new BlockPos(3,3,3));player.setPos(portal.getX()+2,portal.getY(),portal.getZ());h.getLevel().addNewPlayer(player);
        try{
            progress(portal,12,0);portal.tick();var area=portal.getBoundingBox().inflate(4);var leaders=h.getLevel().getEntitiesOfClass(CultistLeader.class,area);
            h.assertValueEqual(leaders.size(),1,"Stage twelve must spawn exactly one praetor");h.assertValueEqual(portal.getStage(),13,"Portal did not advance past boss stage");
            float health=portal.getHealth();progress(portal,13,0);portal.tick();var minions=h.getLevel().getEntitiesOfClass(Cultist.class,area);
            h.assertTrue(!minions.isEmpty(),"Portal stopped spawning after the leader");h.assertTrue(portal.getHealth()<health,"Post-leader minion did not consume portal health");
            h.assertTrue(leaders.getFirst().hasRestriction()&&leaders.getFirst().getRestrictRadius()==32,"Summoned leader is not bound to encounter");
            leaders.forEach(Entity::discard);minions.forEach(Entity::discard);
        }finally{portal.discard();h.getLevel().removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void portalRitualPlacesCrimsonBannersWithoutReplacingSolids(GameTestHelper h){
        var portal=BossContent.CULTIST_PORTAL_GREATER.get().create(h.getLevel());portal.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(12,3,12))));
        var original=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            for(var direction:Direction.Plane.HORIZONTAL){var pos=portal.blockPosition().offset(-direction.getStepZ()*6,0,direction.getStepX()*6);original.put(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlockAndUpdate(pos,direction==Direction.NORTH?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
            progress(portal,0,161);portal.tick();
            for(var direction:Direction.Plane.HORIZONTAL){var pos=portal.blockPosition().offset(-direction.getStepZ()*6,0,direction.getStepX()*6);h.assertTrue(h.getLevel().getBlockState(pos).is(direction==Direction.NORTH?Blocks.STONE:InfusionBaubles.CRIMSON_BANNER.get()),"Portal ritual overwrote a solid or omitted a crimson banner");}
        }finally{original.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void treasureBagsConsumeOnceAndGenerateEightToTwelveRealStacks(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3))));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BossLoot.RARE.get(),2));
        var area=player.getBoundingBox().inflate(2);h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard);
        BossLoot.RARE.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        h.assertValueEqual(player.getMainHandItem().getCount(),1,"Treasure bag consumption");h.assertTrue(drops.size()>=8&&drops.size()<=12,"Bag did not roll original eight to twelve rewards");h.assertTrue(drops.stream().allMatch(drop->!drop.getItem().isEmpty()),"Bag produced empty reward");
        var random=RandomSource.create(83);boolean enchanted=false,gear=false;
        for(int i=0;i<1200;i++){var stack=BossLoot.generateLoot(h.getLevel(),2,random);h.assertFalse(stack.isEmpty(),"Weighted reward is empty");enchanted|=stack.is(Items.ENCHANTED_BOOK);gear|=stack.getItem() instanceof ArmorItem||stack.getItem() instanceof SwordItem;}
        h.assertTrue(enchanted&&gear,"Weighted pool omitted enchanted books or rare gear");drops.forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void crimsonBladeRepairsAndAppliesItsTwoOriginalDebuffs(GameTestHelper h){
        var wielder=h.makeMockPlayer(GameType.SURVIVAL);var victim=EntityType.COW.create(h.getLevel());var sword=new ItemStack(BossGear.CRIMSON_BLADE.get());sword.setDamageValue(10);wielder.tickCount=20;
        BossGear.CRIMSON_BLADE.get().inventoryTick(sword,h.getLevel(),wielder,0,true);h.assertValueEqual(sword.getDamageValue(),9,"Crimson blade self-repair");
        BossGear.CRIMSON_BLADE.get().hurtEnemy(sword,victim,wielder);h.assertTrue(victim.hasEffect(MobEffects.WEAKNESS)&&victim.hasEffect(MobEffects.HUNGER),"Crimson blade missing sap effects");h.assertValueEqual(victim.getEffect(MobEffects.HUNGER).getDuration(),120,"Crimson hunger duration");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void greaterPortalContactAndDeathExplosionKeepItsPearlSafe(GameTestHelper h){
        var portal=h.spawn(BossContent.CULTIST_PORTAL_GREATER.get(),new BlockPos(3,3,3));var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(portal.position());portal.playerTouch(player);
        h.assertValueEqual(player.getHealth(),12f,"Greater portal contact must deal eight magic damage");
        var cow=h.spawn(EntityType.COW,new BlockPos(4,3,3));cow.setNoAi(true);cow.setNoGravity(true);float health=cow.getHealth();
        portal.setHealth(0);portal.die(h.getLevel().damageSources().genericKill());var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,portal.getBoundingBox().inflate(3),drop->drop.getItem().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primordial_pearl"))));
        h.assertTrue(cow.getHealth()<health,"Portal death explosion did not fire after zero health");
        h.assertValueEqual(drops.size(),1,"Greater portal should drop exactly one pearl");h.assertTrue(drops.getFirst() instanceof thaumcraft.port.machine.SpecialItemEntity,"Portal pearl lost its floating/explosion-resistant entity");
        h.assertTrue(drops.getFirst().getItem().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primordial_pearl"))),"Portal drop is not a primordial pearl");
        drops.forEach(Entity::discard);cow.discard();portal.discard();h.succeed();
    }
    private static void progress(CultistPortalGreater portal,int stage,int delay){var tag=new CompoundTag();portal.addAdditionalSaveData(tag);tag.putInt("stage",stage);tag.putInt("stagecounter",delay);portal.readAdditionalSaveData(tag);}
    private CultistBossGameTests(){}
}
