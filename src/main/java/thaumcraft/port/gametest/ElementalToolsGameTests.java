package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.infusion.tools.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ElementalToolsGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void sourceStatsAndInnateInfusionsSurviveItemSave(GameTestHelper helper) {
        var tools=Map.of(InfusionTools.AXE.get(),Map.of("burrowing",1,"collector",1),
                InfusionTools.PICK.get(),Map.of("refining",1,"sounding",2),InfusionTools.SHOVEL.get(),Map.of("destructive",1),
                InfusionTools.SWORD.get(),Map.of("arcing",2));
        for (var entry:tools.entrySet()) {
            var stack=new ItemStack(entry.getKey());
            helper.assertValueEqual(stack.getMaxDamage(),1500,"Elemental durability differs from TC6");
            helper.assertValueEqual(entry.getKey().getEnchantmentValue(),18,"Elemental enchantability differs from TC6");
            var thaumium=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:ingot_thaumium"));
            helper.assertTrue(entry.getKey().isValidRepairItem(stack,new ItemStack(thaumium)),"Thaumium must repair elemental tools");
            helper.assertFalse(entry.getKey().isValidRepairItem(stack,new ItemStack(Items.IRON_INGOT)),"Iron must not repair elemental tools");
            helper.assertValueEqual(InfusionAugments.enchantments(stack),entry.getValue(),"Crafted item lost its inherent infusions");
            var loaded=ItemStack.parseOptional(helper.getLevel().registryAccess(),(CompoundTag)stack.save(helper.getLevel().registryAccess()));
            helper.assertValueEqual(InfusionAugments.enchantments(loaded),entry.getValue(),"Save/load lost inherent infusions");
        }
        var hoe=new ItemStack(InfusionTools.HOE.get());
        helper.assertValueEqual(hoe.getMaxDamage(),1500,"Hoe durability differs from elemental material");
        helper.assertValueEqual(hoe.getItem().getEnchantmentValue(),5,"Hoe has its own original enchantability");
        helper.assertTrue(hoe.is(ItemTags.HOES),"Modern hoe tag missing");
        var boots=new ItemStack(InfusionTools.BOOTS.get());
        helper.assertValueEqual(boots.getMaxDamage(),350,"Traveller durability differs from source");
        helper.assertValueEqual(InfusionTools.BOOTS.get().getDefense(),1,"Traveller defense differs from source");
        helper.assertValueEqual(InfusionTools.BOOTS.get().getToughness(),1f,"Traveller toughness differs from source");
        helper.assertTrue(InfusionTools.BOOTS.get().isValidRepairItem(boots,new ItemStack(Items.LEATHER)),"Leather must repair boots");
        helper.assertFalse(InfusionTools.BOOTS.get().isValidRepairItem(boots,new ItemStack(Items.IRON_INGOT)),"Iron must not repair boots");
        helper.assertTrue(boots.is(ItemTags.FOOT_ARMOR),"Traveller boots need modern armor enchantments");
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void streamAxeAttractsItemsWithoutSpendingDurability(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();
        var center=Vec3.atCenterOf(helper.absolutePos(new BlockPos(12,3,12)));player.setPos(center);
        var axe=new ItemStack(InfusionTools.AXE.get());player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        var near=new ItemEntity(level,center.x+3,center.y,center.z,new ItemStack(Items.DIAMOND));
        var outside=new ItemEntity(level,center.x+11,center.y,center.z,new ItemStack(Items.DIAMOND));
        near.setDeltaMovement(Vec3.ZERO);outside.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(near);level.addFreshEntity(outside);
        InfusionTools.AXE.get().use(level,player,InteractionHand.MAIN_HAND);
        InfusionTools.AXE.get().onUseTick(level,player,axe,72000);
        helper.assertTrue(player.isUsingItem(),"Axe right click did not start continuous use");
        helper.assertTrue(near.getDeltaMovement().x<0,"Stream axe did not attract dropped item");
        helper.assertTrue(Math.abs(near.getDeltaMovement().x)<=.25&&Math.abs(near.getDeltaMovement().y)<=.25,"Magnet speed exceeds source cap");
        helper.assertValueEqual(outside.getDeltaMovement(),Vec3.ZERO,"Magnet reached outside its ten block cube");
        helper.assertValueEqual(axe.getDamageValue(),0,"Attraction must not damage the axe");
        near.discard();outside.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void corePickIgnitesForTwoSecondsAndStillAllowsMelee(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var pick=new ItemStack(InfusionTools.PICK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,pick);var target=helper.spawn(EntityType.COW,new BlockPos(5,2,5));
        helper.assertFalse(InfusionTools.PICK.get().onLeftClickEntity(pick,player,target),"Pick's flame ability must not cancel ordinary melee");
        helper.assertValueEqual(target.getRemainingFireTicks(),40,"Core pick must ignite for the original two seconds");
        target.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void zephyrLiftsRepelsAndSpendsOneDurabilityPerSecond(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();
        var origin=helper.absolutePos(new BlockPos(12,3,12));player.setPos(Vec3.atCenterOf(origin));
        var sword=new ItemStack(InfusionTools.SWORD.get());player.setItemInHand(InteractionHand.MAIN_HAND,sword);
        var cow=helper.spawn(EntityType.COW,new BlockPos(13,3,12));cow.setDeltaMovement(Vec3.ZERO);
        player.setDeltaMovement(0,-.6,0);player.fallDistance=12;
        InfusionTools.SWORD.get().use(level,player,InteractionHand.MAIN_HAND);
        InfusionTools.SWORD.get().onUseTick(level,player,sword,72000);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y-(-.6/1.2f+.08f))<.00001,"Zephyr fall braking/lift differs from TC6");
        helper.assertTrue(Math.abs(player.fallDistance-10)<.0001,"Zephyr did not reduce fall distance");
        helper.assertTrue(cow.getDeltaMovement().x>0,"Zephyr vortex did not repel nearby creature");
        helper.assertValueEqual(sword.getDamageValue(),1,"Vortex must spend one durability at activation");
        InfusionTools.SWORD.get().onUseTick(level,player,sword,71999);
        helper.assertValueEqual(sword.getDamageValue(),1,"Vortex spent durability between second boundaries");
        InfusionTools.SWORD.get().onUseTick(level,player,sword,71980);
        helper.assertValueEqual(sword.getDamageValue(),2,"Vortex did not spend its next second of durability");
        player.setDeltaMovement(0,.49,0);InfusionTools.SWORD.get().onUseTick(level,player,sword,71979);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y-.2f)<.00001,"Vortex vertical speed reset missing");
        cow.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void hoeTillsNineBlocksAndGrowsCropsWithoutBonemeal(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();
        var center=helper.absolutePos(new BlockPos(5,2,5));var hoe=new ItemStack(InfusionTools.HOE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
        for (int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) level.setBlockAndUpdate(center.offset(x,0,z),Blocks.DIRT.defaultBlockState());
        InfusionTools.HOE.get().useOn(context(player,center,Direction.UP));
        for (int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) helper.assertTrue(level.getBlockState(center.offset(x,0,z)).is(Blocks.FARMLAND),"Hoe missed a block in its 3x3 area");
        helper.assertValueEqual(hoe.getDamageValue(),9,"Hoe must spend durability for each tilled block");
        level.setBlockAndUpdate(center.above(),Blocks.WHEAT.defaultBlockState());
        InfusionTools.HOE.get().useOn(context(player,center.above(),Direction.UP));
        helper.assertTrue(level.getBlockState(center.above()).getValue(CropBlock.AGE)>0,"Hoe failed to fertilize wheat without an inventory payment");
        helper.assertValueEqual(hoe.getDamageValue(),12,"Fertilization must cost exactly three durability");
        var single=center.offset(5,0,0);level.setBlockAndUpdate(single,Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(single.east(),Blocks.DIRT.defaultBlockState());
        player.setShiftKeyDown(true);InfusionTools.HOE.get().useOn(context(player,single,Direction.UP));
        helper.assertTrue(level.getBlockState(single).is(Blocks.FARMLAND),"Sneaking must retain ordinary hoe operation");
        helper.assertTrue(level.getBlockState(single.east()).is(Blocks.DIRT),"Sneaking must suppress area tilling");
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void earthmoverCopiesStatePaysPerBlockAndCyclesOnlyHeldShovel(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();
        var source=helper.absolutePos(new BlockPos(5,2,5));
        var state=Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X);
        level.setBlockAndUpdate(source,state);var shovel=new ItemStack(InfusionTools.SHOVEL.get());player.setItemInHand(InteractionHand.MAIN_HAND,shovel);
        player.getInventory().setItem(1,new ItemStack(Items.QUARTZ_PILLAR,12));
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        for (var pos:ElementalShovelItem.plane(shovel,source,Direction.UP,player.getDirection()))
            helper.assertValueEqual(level.getBlockState(pos),state,"Architect placement did not preserve pillar orientation");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),3,"Architect placement did not pay nine inventory blocks");
        helper.assertValueEqual(shovel.getDamageValue(),9,"Architect placement did not pay nine durability");
        ElementalToolPayload.handle(player);
        helper.assertValueEqual(ElementalShovelItem.orientation(shovel),1,"G key packet did not cycle orientation");
        helper.assertValueEqual(InfusionAugments.getEnchantment(shovel,"destructive"),1,"Orientation change overwrote inherent infusion");
        var loaded=ItemStack.parseOptional(level.registryAccess(),(CompoundTag)shovel.save(level.registryAccess()));
        helper.assertValueEqual(ElementalShovelItem.orientation(loaded),1,"Architect orientation was not persistent");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));player.setItemInHand(InteractionHand.OFF_HAND,shovel);
        ElementalToolPayload.handle(player);helper.assertValueEqual(ElementalShovelItem.orientation(shovel),1,"Packet modified an offhand shovel");
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void earthmoverGrassFallbackAndCanceledPlacementCannotDuplicateItems(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();var source=helper.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(source,Blocks.GRASS_BLOCK.defaultBlockState());var shovel=new ItemStack(InfusionTools.SHOVEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,shovel);player.getInventory().setItem(1,new ItemStack(Items.DIRT,9));
        java.util.function.Consumer<BlockEvent.EntityPlaceEvent> cancel=event -> {
            if (event.getEntity()==player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP)); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(level.getBlockState(source.above()).isAir(),"Canceled architect event left a block behind");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),9,"Canceled placement consumed inventory");
        helper.assertValueEqual(shovel.getDamageValue(),0,"Canceled placement consumed durability");
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        for (var pos:ElementalShovelItem.plane(shovel,source,Direction.UP,player.getDirection())) helper.assertTrue(level.getBlockState(pos).is(Blocks.DIRT),"Grass source must use ordinary dirt when grass inventory is absent");
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(),"Grass fallback failed to consume dirt");
        var chest=source.offset(7,0,0);level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());player.getInventory().setItem(1,new ItemStack(Items.CHEST,9));
        InfusionTools.SHOVEL.get().useOn(context(player,chest,Direction.UP));
        helper.assertTrue(level.getBlockState(chest.above()).isAir(),"Earthmover must never copy block entities");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),9,"Rejected block entity consumed items");
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void travellerChargeMovementJumpAndUnchargedFallProtection(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var boots=new ItemStack(InfusionTools.BOOTS.get());
        player.setItemSlot(EquipmentSlot.FEET,boots);ItemCharge.add(boots,player,240);player.zza=1;player.setYRot(0);player.setOnGround(true);player.tickCount=20;
        player.setDeltaMovement(Vec3.ZERO);TravellerBootsItem.tickBoots(player);
        helper.assertValueEqual(ItemCharge.get(boots),239,"Boots must initially spend one charge");
        helper.assertValueEqual(boots.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("energy"),60,"Boots did not store sixty energy seconds");
        helper.assertTrue(Math.abs(player.getDeltaMovement().z-.05)<.00001,"Ground movement bonus differs from TC6");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT)-1)<.00001,"Charged boots must step up one block");
        player.setShiftKeyDown(true);player.tickCount=21;TravellerBootsItem.tickBoots(player);
        helper.assertTrue(player.getAttributeValue(Attributes.STEP_HEIGHT)<1,"Sneaking must restore normal step height");
        player.setShiftKeyDown(false);player.setDeltaMovement(Vec3.ZERO);
        NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(player));
        helper.assertTrue(Math.abs(player.getDeltaMovement().y-.275)<.00001,"Charged boots jump bonus missing");
        // Force exact second boundaries; movement ticks above deliberately used a non-second tick.
        for(int i=0;i<60;i++) { player.tickCount=(i+2)*20;TravellerBootsItem.tickBoots(player); }
        helper.assertValueEqual(ItemCharge.get(boots),239,"Boots spent a charge before its sixty energy seconds expired");
        player.tickCount=1240;TravellerBootsItem.tickBoots(player);helper.assertValueEqual(ItemCharge.get(boots),238,"Boots failed to renew energy after sixty seconds");
        var restored=ItemStack.parseOptional(helper.getLevel().registryAccess(),(CompoundTag)boots.save(helper.getLevel().registryAccess()));
        helper.assertValueEqual(ItemCharge.get(restored),238,"Boot charge did not survive item save");
        ItemCharge.consume(boots,player,238);player.setDeltaMovement(Vec3.ZERO);NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(player));
        helper.assertValueEqual(player.getDeltaMovement(),Vec3.ZERO,"Uncharged boots still boosted jump");
        var fall=new LivingIncomingDamageEvent(player,new DamageContainer(player.damageSources().fall(),8));NeoForge.EVENT_BUS.post(fall);
        helper.assertValueEqual(fall.getAmount(),3f,"Original fall reduction must work even without charge");
        var smallFall=new LivingIncomingDamageEvent(player,new DamageContainer(player.damageSources().fall(),3));NeoForge.EVENT_BUS.post(smallFall);
        helper.assertTrue(smallFall.isCanceled(),"Boots must cancel reduced fall damage below one");
        player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);TravellerBootsItem.tickBoots(player);
        helper.assertTrue(player.getAttributeValue(Attributes.STEP_HEIGHT)<1,"Unequipping boots leaked step height");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void earthmoverCannotDuplicateSlabsWaterOrStoredResources(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);var level=helper.getLevel();var shovel=new ItemStack(InfusionTools.SHOVEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,shovel);
        var source=helper.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(source,Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE));
        player.getInventory().setItem(1,new ItemStack(Items.STONE_SLAB,17));
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        long copies=ElementalShovelItem.plane(shovel,source,Direction.UP,player.getDirection()).stream().filter(p -> level.getBlockState(p).is(Blocks.STONE_SLAB)).count();
        helper.assertValueEqual(copies,8L,"Double slabs must cost two slabs for each placed block");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),1,"An odd remaining slab must not buy another double slab");
        source=helper.absolutePos(new BlockPos(12,2,5));
        level.setBlockAndUpdate(source,Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));
        var water=source.above().offset(-1,0,-1);level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
        player.getInventory().setItem(1,new ItemStack(Items.STONE_SLAB,9));
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        for (var pos:ElementalShovelItem.plane(shovel,source,Direction.UP,player.getDirection())) {
            helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE_SLAB),"Water handling prevented valid slab placement");
            helper.assertValueEqual(level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),pos.equals(water),"Architect cloned water from its source instead of preserving target fluid");
        }
        source=helper.absolutePos(new BlockPos(5,2,12));
        level.setBlockAndUpdate(source,Blocks.COMPOSTER.defaultBlockState().setValue(net.minecraft.world.level.block.ComposterBlock.LEVEL,8));
        player.getInventory().setItem(1,new ItemStack(Items.COMPOSTER,9));
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        helper.assertTrue(level.getBlockState(source.above()).isAir(),"Empty composters bought ready bonemeal");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),9,"Rejected stored resource consumed material");
        source=helper.absolutePos(new BlockPos(12,2,12));
        level.setBlockAndUpdate(source,Blocks.POTTED_DANDELION.defaultBlockState());player.getInventory().setItem(1,new ItemStack(Items.FLOWER_POT,9));
        InfusionTools.SHOVEL.get().useOn(context(player,source,Direction.UP));
        helper.assertTrue(level.getBlockState(source.above()).isAir(),"Empty flowerpots duplicated the source's flower");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(),9,"Rejected filled flowerpot consumed material");
        helper.succeed();
    }
    private static UseOnContext context(net.minecraft.world.entity.player.Player player,BlockPos pos,Direction face) {
        return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
    }
    private ElementalToolsGameTests() {}
}
