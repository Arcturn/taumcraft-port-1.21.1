package thaumcraft.port.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.item.GolemPlacerItem;
import thaumcraft.port.golem.part.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemEntityGameTests {
    private static GolemEntity golem(GameTestHelper helper, GolemProperties properties) {
        var golem = GolemContent.GOLEM.get().create(helper.getLevel());
        golem.setValidSpawn(); golem.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        golem.setProperties(properties); golem.initializePlaced(); return golem;
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void materialTraitsSetOriginalAttributes(GameTestHelper helper) {
        var wood = golem(helper, new GolemProperties());
        helper.assertValueEqual(wood.getMaxHealth(), 16f, "Wood health differs from BETA26 base10+material6");
        helper.assertTrue(Math.abs(wood.getGolemMoveSpeed()-1.2f)<.00001, "Light movement bonus missing");
        var props = new GolemProperties(); props.setMaterial(GolemMaterial.getMaterials()[1]); props.setArms(GolemArm.getArms()[2]);
        var iron = golem(helper, props);
        helper.assertValueEqual(iron.getMaxHealth(), 30f, "Iron health differs");
        helper.assertValueEqual(iron.getAttributeValue(Attributes.ATTACK_DAMAGE), 4.5, "Brutal claws damage differs");
        helper.assertValueEqual(iron.getArmorValue(), 8, "Iron armor differs");
        helper.assertFalse(iron.hurt(helper.getLevel().damageSources().inFire(), 100), "Fireproof iron took fire damage");
        helper.assertFalse(wood.hurt(helper.getLevel().damageSources().cactus(), 2), "Original golem cactus immunity missing");
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void carryingRespectsComponentsStackLimitsAndHaulerCompaction(GameTestHelper helper) {
        var props = new GolemProperties(); props.setAddon(GolemAddon.getAddons()[3]); var golem = golem(helper, props);
        var named = new ItemStack(Items.DIAMOND, 60); named.set(DataComponents.CUSTOM_NAME, Component.literal("Reserved"));
        helper.assertTrue(golem.holdItem(named).isEmpty(), "First carried stack rejected");
        helper.assertValueEqual(named.getCount(), 60, "Carrying aliased the incoming inventory stack");
        helper.assertTrue(golem.holdItem(new ItemStack(Items.DIAMOND, 64)).isEmpty(), "Hauler second slot missing");
        helper.assertValueEqual(golem.canCarryAmount(new ItemStack(Items.DIAMOND)), 0, "Named diamonds merged with plain diamonds");
        var extra = named.copyWithCount(10); var remainder = golem.holdItem(extra);
        helper.assertValueEqual(remainder.getCount(), 6, "Overflow count differs");
        var removed = golem.dropItem(named.copyWithCount(64)); helper.assertValueEqual(removed.getCount(),64,"Wrong named stack removed");
        helper.assertTrue(golem.getOffhandItem().isEmpty() && golem.getMainHandItem().getCount()==64
                && !golem.getMainHandItem().has(DataComponents.CUSTOM_NAME), "Hauler did not compact its remaining stack");
        var normal = golem(helper, new GolemProperties());
        var tools = normal.holdItem(new ItemStack(Items.DIAMOND_PICKAXE,2));
        helper.assertValueEqual(normal.getMainHandItem().getCount(),1,"Nonstackable tool exceeded its stack limit");
        helper.assertValueEqual(tools.getCount(),1,"Nonstackable remainder was lost"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void smartRanksUseQuadraticThresholdAndCap(GameTestHelper helper) {
        var props = new GolemProperties(); props.setHead(GolemHead.getHeads()[1]); var golem = golem(helper, props);
        golem.addRankXp(999); helper.assertValueEqual(golem.getProperties().getRank(),0,"Rank gained early");
        golem.addRankXp(1); helper.assertValueEqual(golem.getProperties().getRank(),1,"First SMART rank missing");
        helper.assertValueEqual(golem.nextRankXp(),4000,"Second rank cost must be4*1000");
        golem.addRankXp(4007); helper.assertValueEqual(golem.getProperties().getRank(),2,"Second rank missing");
        helper.assertValueEqual(golem.getRankXp(),7,"Rank XP remainder lost");
        var plain = golem(helper,new GolemProperties()); plain.addRankXp(10000);
        helper.assertValueEqual(plain.getRankXp(),0,"Non-SMART golem gained XP");
        props.setRank(10); golem.setProperties(props); golem.setRankXp(0); golem.addRankXp(200000);
        helper.assertValueEqual(golem.getProperties().getRank(),10,"Rank exceeded10");helper.assertValueEqual(golem.getRankXp(),0,"Max rank accumulated XP");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void ownershipHomeColorRankAndHandsSurviveSave(GameTestHelper helper) {
        var props = new GolemProperties();props.setHead(GolemHead.getHeads()[1]);props.setAddon(GolemAddon.getAddons()[3]);props.setRank(3);
        var golem = golem(helper,props);var owner=UUID.randomUUID();golem.setOwnerUUID(owner);golem.setRankXp(99);golem.setGolemColor((byte)5);
        var home=helper.absolutePos(new BlockPos(3,2,3));golem.setHomePosition(home);
        golem.holdItem(new ItemStack(Items.DIAMOND,64));golem.holdItem(new ItemStack(Items.EMERALD,3));
        var tag=new CompoundTag();golem.saveWithoutId(tag);var restored=GolemContent.GOLEM.get().create(helper.getLevel());restored.load(tag);
        helper.assertTrue(owner.equals(restored.getOwnerUUID()),"Owner lost");helper.assertValueEqual(restored.getHomePosition(),home,"Home lost");
        helper.assertValueEqual(restored.getProperties().toLong(),props.toLong(),"Packed properties lost");helper.assertValueEqual(restored.getRankXp(),99,"Rank XP lost");
        helper.assertValueEqual(restored.getGolemColor(),(byte)5,"Color lost");helper.assertValueEqual(restored.getMainHandItem().getCount(),64,"Main cargo lost");
        helper.assertValueEqual(restored.getOffhandItem().getCount(),3,"Offhand cargo lost");
        var packed=new ItemStack(GolemContent.GOLEM_ITEM.get());GolemPlacerItem.setProperties(packed,props,99);
        var loaded=ItemStack.parseOptional(helper.getLevel().registryAccess(),(CompoundTag)packed.save(helper.getLevel().registryAccess()));
        helper.assertValueEqual(GolemPlacerItem.getProperties(loaded).toLong(),props.toLong(),"Placer lost configuration");
        helper.assertValueEqual(GolemPlacerItem.getRankXp(loaded),99,"Placer lost XP");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void ownerDefenseRetainsOnlyLivingTargetsWithinFollowRange(GameTestHelper helper) {
        var props = new GolemProperties(); props.setArms(GolemArm.getArms()[2]); var worker = golem(helper, props);
        var target = net.minecraft.world.entity.EntityType.COW.create(helper.getLevel()); target.setPos(worker.position().add(3,0,0));
        var defense = new thaumcraft.port.golem.ai.GolemOwnerTargetGoal(worker,true); worker.setTarget(target);
        helper.assertTrue(defense.canContinueToUse(), "Owner defense dropped its living nearby target immediately");
        target.setPos(worker.position().add(80,0,0));
        helper.assertFalse(defense.canContinueToUse(), "Owner defense kept a target beyond follow range");
        defense.stop(); helper.assertTrue(worker.getTarget()==null&&!worker.isInCombat(), "Owner defense did not clear its expired combat target");
        target.setPos(worker.position().add(3,0,0)); worker.setTarget(target); target.discard();
        helper.assertFalse(defense.canContinueToUse(), "Owner defense retained removed target"); defense.stop(); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void armSwingProgressAdvancesAndFinishesForWorkerMobs(GameTestHelper helper) {
        var worker = golem(helper, new GolemProperties()); worker.setNoAi(true); worker.swingArm();
        for (int tick = 0; tick < 3; tick++) worker.aiStep();
        helper.assertTrue(worker.attackAnim > 0 && worker.attackAnim < 1, "Worker swing packet never advanced attack animation");
        for (int tick = 0; tick < 10; tick++) worker.aiStep();
        helper.assertValueEqual(worker.attackAnim, 0f, "Worker arm swing did not finish"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void mobilityVariantsKeepTheirDistinctNavigators(GameTestHelper helper) {
        var props=new GolemProperties();props.setLegs(GolemLeg.getLegs()[1]);var roller=golem(helper,props);
        helper.assertValueEqual(roller.getAttributeValue(Attributes.STEP_HEIGHT),.5,"Roller step height differs");
        helper.assertTrue(Math.abs(roller.getGolemMoveSpeed()-1.45f)<.00001,"Roller speed missing");
        props=new GolemProperties();props.setLegs(GolemLeg.getLegs()[2]);var climber=golem(helper,props);
        helper.assertTrue(climber.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WallClimberNavigation,"Climber navigation missing");
        climber.setBesideClimbableBlock(true);helper.assertTrue(climber.onClimbable(),"Climbing flag ignored");
        props=new GolemProperties();props.setLegs(GolemLeg.getLegs()[3]);var flyer=golem(helper,props);
        helper.assertTrue(flyer.getNavigation() instanceof thaumcraft.port.golem.ai.GolemFlyingNavigation&&flyer.isNoGravity(),"Flyer is using ground movement");
        helper.assertFalse(flyer.causeFallDamage(50,1,helper.getLevel().damageSources().fall()),"Flyer took fall damage");
        helper.succeed();
    }
    private GolemEntityGameTests() { }
}
