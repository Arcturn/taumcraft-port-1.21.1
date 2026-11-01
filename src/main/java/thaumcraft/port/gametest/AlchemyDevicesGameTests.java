package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.alchemy.devices.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.RevealingReadout;
import thaumcraft.port.essentia.*;
import thaumcraft.port.machine.*;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AlchemyDevicesGameTests {
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void urnUsesExactVisAndTopWaterCapability(GameTestHelper helper){
        var urn=urn(helper);var aura=AuraSystem.get(helper.getLevel(),urn.getBlockPos());AuraSystem.add(helper.getLevel(),urn.getBlockPos(),2,false);float before=aura.vis();
        for(int i=0;i<5;i++)urn.tick();
        helper.assertValueEqual(urn.water(),100,"Urn must create 100 mB per five ticks");
        helper.assertTrue(Math.abs(before-aura.vis()-.1f)<.001f,"Urn did not consume exactly 0.1 vis");
        for(Direction face:Direction.values())helper.assertTrue((helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,urn.getBlockPos(),face)!=null)==(face==Direction.UP),"Urn exposed fluid on wrong face "+face);
        var fluid=helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,urn.getBlockPos(),Direction.UP);
        helper.assertValueEqual(fluid.fill(new FluidStack(Fluids.WATER,2000),FluidAction.SIMULATE),900,"Simulated urn fill ignored remaining capacity");helper.assertValueEqual(urn.water(),100,"Simulated fill changed urn");
        helper.assertValueEqual(fluid.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE),0,"Urn accepted lava");
        helper.assertValueEqual(fluid.drain(500,FluidAction.SIMULATE).getAmount(),100,"Simulated drain ignored water stock");helper.assertValueEqual(urn.water(),100,"Simulated drain changed urn");
        helper.assertValueEqual(fluid.drain(500,FluidAction.EXECUTE).getAmount(),100,"Actual drain returned wrong amount");helper.assertValueEqual(urn.water(),0,"Actual drain did not empty urn");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void urnBottleAndBucketInteractionsPreserveContainers(GameTestHelper helper){
        var urn=urn(helper);urn.fluidHandler.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);var player=player(helper,urn.getBlockPos());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));click(player,urn.getBlockPos());
        helper.assertValueEqual(urn.water(),667,"Bottle did not cost original 333 mB");
        helper.assertTrue(player.getMainHandItem().is(Items.POTION)&&player.getMainHandItem().get(DataComponents.POTION_CONTENTS).is(Potions.WATER),"Bottle did not become water potion");
        urn.fluidHandler.fill(new FluidStack(Fluids.WATER,333),FluidAction.EXECUTE);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));click(player,urn.getBlockPos());
        helper.assertValueEqual(urn.water(),0,"Bucket did not remove 1000 mB");helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET),"Empty bucket was not filled");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));click(player,urn.getBlockPos());helper.assertValueEqual(urn.water(),1000,"Water bucket could not refill urn");helper.assertTrue(player.getMainHandItem().is(Items.BUCKET),"Refill lost empty bucket");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void urnFindsFluidHandlersWithinRangeAndPersistsWater(GameTestHelper helper){
        var urn=urn(helper);helper.setBlock(CENTER.east(2),MachineContent.CRUCIBLE.get());helper.setBlock(CENTER.east(3),MachineContent.CRUCIBLE.get());
        var inside=(CrucibleEntity)helper.getLevel().getBlockEntity(urn.getBlockPos().east(2));var outside=(CrucibleEntity)helper.getLevel().getBlockEntity(urn.getBlockPos().east(3));
        for(int i=0;i<375;i++)urn.tick();
        helper.assertTrue(inside.state.water()>0,"Urn never refilled nearby crucible through the real fluid capability");helper.assertValueEqual(inside.state.water()%25,0,"Urn did not transfer in 25 mB batches");helper.assertValueEqual(outside.state.water(),0,"Urn exceeded its two-block horizontal range");
        var saved=urn.saveWithoutMetadata(helper.getLevel().registryAccess());var restored=new EverfullUrnEntity(urn.getBlockPos(),urn.getBlockState());restored.loadWithComponents(saved,helper.getLevel().registryAccess());helper.assertValueEqual(restored.water(),urn.water(),"Urn water changed across save/load");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void urnFillsThreeCauldronLevelsAndRespectsVerticalRange(GameTestHelper helper){
        var urn=urn(helper);helper.setBlock(CENTER.east(),Blocks.CAULDRON);helper.setBlock(CENTER.above(2),Blocks.CAULDRON);
        for(int i=0;i<375;i++)urn.tick();
        var state=helper.getLevel().getBlockState(urn.getBlockPos().east());helper.assertTrue(state.is(Blocks.WATER_CAULDRON),"Urn did not convert empty cauldron to water");helper.assertValueEqual(state.getValue(LayeredCauldronBlock.LEVEL),3,"Urn did not fill all three cauldron levels");helper.assertTrue(helper.getLevel().getBlockState(urn.getBlockPos().above(2)).is(Blocks.CAULDRON),"Urn exceeded its one-block vertical range");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void sugarKeepsOriginalCaneAspectsWhileHoneyRemainsADatapackFallback(GameTestHelper helper){
        var honey=new ShapelessRecipe("",CraftingBookCategory.MISC,new ItemStack(Items.SUGAR,3),NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.HONEY_BOTTLE)));
        var cane=new ShapelessRecipe("",CraftingBookCategory.MISC,new ItemStack(Items.SUGAR),NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.SUGAR_CANE)));
        var level=helper.getLevel();
        var withBoth=new RecipeAspects.Calculator(level.registryAccess(),level.potionBrewing(),List.of(honey,cane));
        helper.assertValueEqual(withBoth.get(new ItemStack(Items.SUGAR)),Map.of(Aspect.DESIRE,1,Aspect.ENERGY,1,Aspect.PLANT,3,Aspect.WATER,2,Aspect.AIR,1),"Modern honey route overwrote original sugar cane aspects");
        var honeyOnly=new RecipeAspects.Calculator(level.registryAccess(),level.potionBrewing(),List.of(honey));
        helper.assertValueEqual(honeyOnly.get(new ItemStack(Items.SUGAR)),Map.of(Aspect.DESIRE,1,Aspect.ENERGY,1,Aspect.LIFE,2),"Honey-only datapack fallback ignored returned bottle or output count");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void sprayerValidatesPotionAndPullsCompleteCostFromRealJar(GameTestHelper helper){
        var sprayer=sprayer(helper);var template=PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS);
        for(var potion:List.of(Potions.WATER,Potions.MUNDANE,Potions.THICK,Potions.AWKWARD))helper.assertFalse(PotionSprayerEntity.validPotion(PotionContents.createItemStack(Items.POTION,potion)),"Sprayer accepted unfinished potion "+potion);
        for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION))helper.assertTrue(PotionSprayerEntity.validPotion(PotionContents.createItemStack(item,Potions.SWIFTNESS)),"Sprayer rejected valid potion form");
        helper.assertValueEqual(RecipeAspects.get(new ItemStack(Items.SUGAR),helper.getLevel()),Map.of(Aspect.DESIRE,1,Aspect.ENERGY,1,Aspect.PLANT,3,Aspect.WATER,2,Aspect.AIR,1),"Modern honey recipe replaced original cane-derived brewing reagent aspects");
        sprayer.setItem(0,template);var expected=Map.of(Aspect.AIR,1,Aspect.ALCHEMY,4,Aspect.WATER,1,Aspect.DESIRE,1,Aspect.PLANT,2,Aspect.ENERGY,1,Aspect.FLUX,1);
        helper.assertValueEqual(sprayer.store.recipe(),expected,"Sprayer cost differs from original potion-aspect derivation");for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION))helper.assertValueEqual(PotionSprayerEntity.potionRecipe(PotionContents.createItemStack(item,Potions.SWIFTNESS),helper.getLevel()),expected,"Container form incorrectly changed sprayer recipe cost");
        helper.setBlock(CENTER.below(),EssentiaContent.JAR.get());var jar=(JarEntity)helper.getLevel().getBlockEntity(sprayer.getBlockPos().below());
        for(var entry:expected.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Aspect::displayName))).toList()){
            jar.tank.restore(entry.getKey(),entry.getValue(),null);
            for(int i=0;i<entry.getValue()*5;i++)sprayer.tick();
            helper.assertValueEqual(jar.tank.amount(),0,"Sprayer failed to pull required aspect "+entry.getKey());
        }
        for(int i=0;i<5;i++)sprayer.tick();
        helper.assertValueEqual(sprayer.store.charges(),1,"Paid essentia did not create one charge");helper.assertTrue(sprayer.store.progress().isEmpty(),"Charge retained paid essentia");helper.assertTrue(ItemStack.isSameItemSameComponents(template,sprayer.getItem(0)),"Charging consumed or changed potion template");
        helper.assertTrue(helper.getLevel().getCapability(EssentiaContent.CAPABILITY,sprayer.getBlockPos(),Direction.EAST)==null,"Front nozzle exposed essentia input");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void sprayerPullsAboveItsBodyButNeverThroughTheNozzleOrPastEightCharges(GameTestHelper helper){
        var sprayer=sprayer(helper);sprayer.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS));var needed=sprayer.store.needed();
        helper.setBlock(CENTER.above().north(),EssentiaContent.ALEMBIC.get());helper.setBlock(CENTER.above().east(),EssentiaContent.ALEMBIC.get());
        var upper=(JarEntity)helper.getLevel().getBlockEntity(sprayer.getBlockPos().above().north());var front=(JarEntity)helper.getLevel().getBlockEntity(sprayer.getBlockPos().above().east());upper.tank.restore(needed,1,null);front.tank.restore(needed,10,null);
        for(int i=0;i<5;i++)sprayer.tick();
        helper.assertValueEqual(upper.tank.amount(),0,"Sprayer did not pull from original upper row of neighbors");helper.assertValueEqual(sprayer.store.progress().getOrDefault(needed,0),1,"Upper transfer did not reach recipe progress");
        front.tank.restore(sprayer.store.needed(),10,null);for(int i=0;i<5;i++)sprayer.tick();helper.assertValueEqual(front.tank.amount(),10,"Sprayer pulled through front nozzle's upper neighbor");
        helper.setBlock(CENTER.above().east(),Blocks.AIR);
        var input=helper.getLevel().getCapability(EssentiaContent.CAPABILITY,sprayer.getBlockPos(),Direction.DOWN);
        helper.assertTrue(input!=null,"Sprayer has no rear essentia input");
        for(int charge=0;charge<8;charge++){
            for(var entry:sprayer.store.recipe().entrySet())input.insert(entry.getKey(),entry.getValue(),Direction.DOWN,false);
            for(int i=0;i<5;i++)sprayer.tick();
            helper.assertValueEqual(sprayer.store.charges(),charge+1,"Paid recipe failed to create charge "+(charge+1));
        }
        helper.assertValueEqual(input.insert(needed,1,Direction.DOWN,true),0,"Full sprayer simulated additional intake");helper.assertValueEqual(input.insert(needed,1,Direction.DOWN,false),0,"Full sprayer consumed additional essentia");helper.assertValueEqual(sprayer.getItem(0).getCount(),1,"Eight charges consumed template");helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void sprayerRedstoneEdgeAppliesInstantAndCustomEffectsOnlyInFront(GameTestHelper helper){
        var sprayer=sprayer(helper);var template=PotionContents.createItemStack(Items.POTION,Potions.HEALING);
        template.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.of(Potions.HEALING),Optional.empty(),List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,1200,1))));
        sprayer.setItem(0,template);sprayer.store.restore(sprayer.store.recipe(),Map.of(),2);
        var target=EntityType.COW.create(helper.getLevel());var outside=EntityType.COW.create(helper.getLevel());
        target.setPos(Vec3.atCenterOf(sprayer.getBlockPos().east(2)));outside.setPos(Vec3.atCenterOf(sprayer.getBlockPos().west(2)));target.setNoAi(true);outside.setNoAi(true);target.setHealth(1);outside.setHealth(1);helper.getLevel().addFreshEntity(target);helper.getLevel().addFreshEntity(outside);
        helper.setBlock(CENTER.west(),Blocks.REDSTONE_BLOCK);sprayer.tick();
        helper.assertValueEqual(target.getHealth(),5f,"Full-strength instant healing did not reach forward target");helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED),"Custom potion effect was lost");helper.assertValueEqual(target.getEffect(MobEffects.MOVEMENT_SPEED).getDuration(),1200,"Sprayer shortened custom effect duration");helper.assertValueEqual(outside.getHealth(),1f,"Sprayer affected entity behind nozzle");helper.assertFalse(outside.hasEffect(MobEffects.MOVEMENT_SPEED),"Sprayer leaked custom effect behind nozzle");helper.assertValueEqual(sprayer.store.charges(),1,"Redstone edge did not spend exactly one charge");
        var reloaded=new PotionSprayerEntity(sprayer.getBlockPos(),sprayer.getBlockState());reloaded.loadWithComponents(sprayer.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());reloaded.setLevel(helper.getLevel());reloaded.tick();
        helper.assertValueEqual(reloaded.store.charges(),1,"Save/load invented a second redstone rising edge");helper.assertValueEqual(target.getHealth(),5f,"Save/load re-applied active pulse");
        for(int i=0;i<20;i++)sprayer.tick();helper.assertValueEqual(sprayer.store.charges(),1,"Constant redstone retriggered sprayer");helper.assertValueEqual(target.getHealth(),5f,"Constant redstone reapplied instant effect");
        helper.setBlock(CENTER.west(),Blocks.AIR);sprayer.tick();helper.setBlock(CENTER.west(),Blocks.REDSTONE_BLOCK);sprayer.tick();sprayer.tick();
        helper.assertValueEqual(sprayer.store.charges(),0,"Second rising redstone edge failed to spray");helper.assertValueEqual(target.getHealth(),9f,"Second charge did not apply healing");helper.assertValueEqual(sprayer.getItem(0).getCount(),1,"Spraying consumed potion template");target.discard();outside.discard();helper.succeed();
    }
    @GameTest(template="alchemy_devices_empty",timeoutTicks=80)
    public static void sprayerSaveMenuQuickMoveAndGogglesKeepProgress(GameTestHelper helper){
        var sprayer=sprayer(helper);var player=player(helper,sprayer.getBlockPos());var menu=new PotionSprayerMenu(47,player.getInventory(),sprayer,sprayer.data);
        player.getInventory().setItem(0,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.STRENGTH));
        helper.assertFalse(menu.quickMoveStack(player,28).isEmpty(),"Shift-click did not place potion template");helper.assertFalse(sprayer.isEmpty(),"Potion slot stayed empty");
        var aspect=sprayer.store.needed();helper.assertTrue(aspect!=null,"Inserted potion has no required aspects");sprayer.insert(aspect,1,Direction.DOWN,false);
        helper.assertValueEqual(RevealingReadout.aspects(sprayer),Map.of(aspect,1),"Goggles did not expose sprayer recipe progress");helper.assertValueEqual(menu.progress(aspect),1,"Menu did not synchronize ingredient progress");helper.assertValueEqual(menu.color(),sprayer.color,"Menu RGB split lost potion color");
        var restored=new PotionSprayerEntity(sprayer.getBlockPos(),sprayer.getBlockState());restored.loadWithComponents(sprayer.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.store.recipe(),sprayer.store.recipe(),"Recipe changed across save/load");helper.assertValueEqual(restored.store.progress(),sprayer.store.progress(),"Progress changed across save/load");helper.assertTrue(ItemStack.isSameItemSameComponents(restored.getItem(0),sprayer.getItem(0)),"Template components changed across save/load");
        helper.assertFalse(menu.quickMoveStack(player,0).isEmpty(),"Shift-click failed to remove potion");helper.assertTrue(sprayer.isEmpty()&&sprayer.store.recipe().isEmpty()&&sprayer.store.progress().isEmpty(),"Removing template retained stale recipe or progress");
        player.getInventory().setItem(0,new ItemStack(Items.DIRT));helper.assertTrue(menu.quickMoveStack(player,28).isEmpty(),"Shift-click accepted non-potion");
        helper.assertTrue(menu.quickMoveStack(player,-1).isEmpty()&&menu.quickMoveStack(player,999).isEmpty(),"Invalid menu index was accepted");
        helper.assertTrue(menu.stillValid(player),"Nearby menu unexpectedly invalid");player.setPos(player.getX()+12,player.getY(),player.getZ());helper.assertFalse(menu.stillValid(player),"Remote player retained menu access");helper.succeed();
    }
    private static EverfullUrnEntity urn(GameTestHelper helper){helper.setBlock(CENTER,AlchemyDevices.URN.get());return (EverfullUrnEntity)helper.getLevel().getBlockEntity(helper.absolutePos(CENTER));}
    private static PotionSprayerEntity sprayer(GameTestHelper helper){helper.setBlock(CENTER,AlchemyDevices.SPRAYER.get().defaultBlockState().setValue(PotionSprayerBlock.FACING,Direction.EAST));return (PotionSprayerEntity)helper.getLevel().getBlockEntity(helper.absolutePos(CENTER));}
    private static ServerPlayer player(GameTestHelper helper,BlockPos pos){var player=GameTestPlayers.create(helper);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()-1.5);return player;}
    private static void click(ServerPlayer player,BlockPos pos){player.gameMode.useItemOn(player,player.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
}
