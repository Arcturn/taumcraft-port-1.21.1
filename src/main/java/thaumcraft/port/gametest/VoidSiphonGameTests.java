package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.siphon.*;
import thaumcraft.port.infusion.content.InfusionCraftingContent;
import thaumcraft.port.world.*;

/** Production ticks, rift side effects, persisted output and real automation paths. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class VoidSiphonGameTests {
    private static final BlockPos CENTER=new BlockPos(8,4,8);

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void harvestRunsEveryTwentyTicksAndTruncatesEachRiftSeparately(GameTestHelper h){
        var siphon=siphon(h);var pos=siphon.getBlockPos();
        var first=rift(h,pos.offset(0,2,3),3);var second=rift(h,pos.offset(3,2,0),3);var large=rift(h,pos.offset(-3,2,0),100);
        h.assertValueEqual(siphon.validRifts().size(),3,"Fixture did not expose all three visible rifts");
        noShrink(h,3);tick(siphon,19);
        h.assertValueEqual(siphon.progress(),0,"Siphon harvested before tick twenty");
        h.assertValueEqual(first.stability(),0f,"Idle ticks destabilized a rift");siphon.tick();
        h.assertValueEqual(siphon.progress(),12,"Each sqrt(size) must truncate separately, rather than rounding their sum");
        close(h,first.stability(),-Math.sqrt(3)/15,"Small rift stability loss");
        close(h,large.stability(),-10d/15,"Large rift stability loss");
        h.assertTrue(siphon.isEmpty(),"Partial progress created a premature seed");
        first.discard();second.discard();large.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void thresholdProducesOneSeedAndKeepsPaidRemainder(GameTestHelper h){
        var siphon=siphon(h);var rift=rift(h,siphon.getBlockPos().offset(0,2,3),4);rift.addStability(80);progress(h,siphon,1999);
        h.runAfterDelay(24,()->{
            h.assertTrue(siphon.getItem(0).is(seed()),"Registered block ticker did not produce a void seed");
            h.assertValueEqual(siphon.getItem(0).getCount(),1,"One paid threshold duplicated the output");
            h.assertValueEqual(siphon.progress(),1,"Threshold discarded the paid remainder");
            close(h,rift.stability(),80-2d/15,"Completion charged rift twice");rift.discard();h.succeed();
        });
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fullOrForeignOutputPausesWithoutChargingRifts(GameTestHelper h){
        var siphon=siphon(h);var rift=rift(h,siphon.getBlockPos().offset(0,2,3),100);progress(h,siphon,1990);
        siphon.setItem(0,new ItemStack(seed(),64));tick(siphon,40);
        h.assertValueEqual(siphon.progress(),1990,"Full output consumed progress");h.assertValueEqual(rift.stability(),0f,"Full output charged rift stability");
        siphon.setItem(0,new ItemStack(Items.DIRT));tick(siphon,20);
        h.assertValueEqual(siphon.progress(),1990,"Foreign output allowed harvesting");h.assertTrue(siphon.getItem(0).is(Items.DIRT),"Foreign output was overwritten");
        h.assertValueEqual(rift.size(),100,"Blocked output shrank its rift");
        siphon.setItem(0,ItemStack.EMPTY);noShrink(h,1);tick(siphon,20);
        h.assertValueEqual(siphon.getItem(0).getCount(),1,"Removing obstruction did not resume harvesting");
        h.assertValueEqual(siphon.progress(),0,"Resumed harvest paid the wrong threshold");rift.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void redstonePausesAndResumesWithoutLosingProgressOrOutput(GameTestHelper h){
        var siphon=siphon(h);var pos=siphon.getBlockPos();var rift=rift(h,pos.offset(0,2,3),100);
        progress(h,siphon,55);siphon.setItem(0,new ItemStack(seed(),3));h.getLevel().setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertFalse(h.getLevel().getBlockState(pos).getValue(VoidSiphonBlock.ENABLED),"Redstone did not disable the block");tick(siphon,40);
        h.assertValueEqual(siphon.progress(),55,"Powered siphon harvested or lost progress");h.assertValueEqual(rift.stability(),0f,"Powered siphon charged stability");
        h.assertValueEqual(siphon.getItem(0).getCount(),3,"Power transition changed stored output");h.getLevel().removeBlock(pos.east(),false);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(VoidSiphonBlock.ENABLED),"Removing power did not re-enable the block");
        noShrink(h,1);tick(siphon,20);h.assertValueEqual(siphon.progress(),65,"Unpowered siphon did not resume");rift.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void riftSelectionUsesSourceCubeAndRejectsSmallOrRemovedRifts(GameTestHelper h){
        var siphon=siphon(h);var pos=siphon.getBlockPos();
        var corner=rift(h,pos.offset(7,1,7),3);var far=rift(h,pos.offset(-14,1,0),3);var tiny=rift(h,pos.offset(0,2,-3),1);
        var removed=rift(h,pos.offset(0,2,3),20);removed.discard();corner.collapse();
        var selected=siphon.validRifts();h.assertTrue(selected.contains(corner),"Source cube corner or collapsing rift was incorrectly filtered");
        h.assertFalse(selected.contains(far),"Rift outside the eight-block cube was selected");h.assertFalse(selected.contains(tiny),"Size-one rift remained harvestable");
        h.assertFalse(selected.contains(removed),"Removed rift remained harvestable");noShrink(h,1);tick(siphon,20);
        h.assertValueEqual(siphon.progress(),1,"Invalid rifts contributed progress");
        corner.discard();far.discard();tiny.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void solidWallsBlockTheBeamWhileWaterAndNoncollidingPlantsDoNot(GameTestHelper h){
        var siphon=siphon(h);var pos=siphon.getBlockPos();var rift=rift(h,pos.offset(0,1,6),25);var wall=pos.offset(0,1,3);
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        h.assertFalse(siphon.validRifts().contains(rift),"Beam harvested through a solid wall");tick(siphon,20);
        h.assertValueEqual(siphon.progress(),0,"Occluded rift contributed progress");h.assertValueEqual(rift.stability(),0f,"Occluded rift lost stability");
        h.getLevel().setBlockAndUpdate(wall,Blocks.WATER.defaultBlockState());h.assertTrue(siphon.validRifts().contains(rift),"Water incorrectly blocked the source ray");
        h.getLevel().setBlockAndUpdate(wall,Blocks.SHORT_GRASS.defaultBlockState());h.assertTrue(siphon.validRifts().contains(rift),"Noncolliding plant incorrectly blocked the ray");
        noShrink(h,1);tick(siphon,20);h.assertValueEqual(siphon.progress(),5,"Clear source ray did not resume harvesting");rift.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void sourceShrinkRollIsOneInThirtyThreeAndStopsAtSizeOne(GameTestHelper h){
        var siphon=siphon(h);var rift=rift(h,siphon.getBlockPos().offset(0,2,3),2);
        long shrinking=rollSeed(h,true,1);h.getLevel().random.setSeed(shrinking);tick(siphon,20);
        h.assertValueEqual(rift.size(),1,"The successful source shrink roll did not remove exactly one size unit");
        h.assertValueEqual(siphon.progress(),1,"Shrink changed the pre-shrink progress contribution");float before=rift.stability();
        h.getLevel().random.setSeed(shrinking);tick(siphon,20);
        h.assertValueEqual(rift.size(),1,"Siphon consumed the last rift unit");h.assertValueEqual(rift.stability(),before,"Size-one rift was charged again");
        rift.discard();var other=rift(h,siphon.getBlockPos().offset(0,2,3),2);noShrink(h,1);tick(siphon,20);
        h.assertValueEqual(other.size(),2,"An unsuccessful shrink roll still shrank the rift");other.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void savedLargeProgressRetainsOverflowAndFlushesWithoutNewRiftCharges(GameTestHelper h){
        var siphon=siphon(h);var rift=rift(h,siphon.getBlockPos().offset(0,2,3),100);progress(h,siphon,70003);siphon.setItem(0,new ItemStack(seed(),60));
        var saved=siphon.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertValueEqual(saved.getInt("progress"),70003,"Progress was truncated to the legacy signed short");
        var inventory=GameTestPlayers.create(h).getInventory();var serverMenu=new VoidSiphonMenu(47,inventory,siphon);var clientMenu=new VoidSiphonMenu(47,inventory);
        clientMenu.setData(0,(short)siphon.data.get(0));clientMenu.setData(1,(short)siphon.data.get(1));
        h.assertValueEqual(serverMenu.progress(),70003,"Server menu truncated large progress");h.assertValueEqual(clientMenu.progress(),70003,"Container data wire halves truncated large progress");
        var restored=new VoidSiphonEntity(siphon.getBlockPos(),siphon.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(restored);tick(restored,20);
        h.assertValueEqual(restored.getItem(0).getCount(),64,"Saved pending output did not fill available stack space");
        h.assertValueEqual(restored.progress(),62003,"Full output discarded prepaid overflow");h.assertValueEqual(rift.stability(),0f,"Flushing prepaid output harvested a rift again");
        restored.removeItem(0,64);tick(restored,20);
        h.assertValueEqual(restored.getItem(0).getCount(),31,"Pending overflow became permanently stalled after extraction");
        h.assertValueEqual(restored.progress(),3,"Overflow flush paid an incorrect number of thresholds");h.assertValueEqual(rift.stability(),0f,"Second pending flush charged the rift");
        rift.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void partialProgressAndInventorySurviveReloadBeforeAnySeedCompletes(GameTestHelper h){
        var siphon=siphon(h);var rift=rift(h,siphon.getBlockPos().offset(0,2,3),100);siphon.setItem(0,new ItemStack(seed(),4));
        noShrink(h,1);tick(siphon,20);var saved=siphon.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=new VoidSiphonEntity(siphon.getBlockPos(),siphon.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());restored.setLevel(h.getLevel());h.getLevel().setBlockEntity(restored);
        h.assertValueEqual(restored.progress(),10,"Reload lost progress that had not produced a seed");h.assertValueEqual(restored.getItem(0).getCount(),4,"Reload lost stored seeds");
        noShrink(h,1);tick(restored,20);h.assertValueEqual(restored.progress(),20,"Reload charged an extra cycle or failed to resume");
        h.assertValueEqual(restored.getItem(0).getCount(),4,"Reload duplicated output");rift.discard();h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void everySidedAndUnsidedCapabilityIsExtractionOnlyAndSimulationIsPure(GameTestHelper h){
        var siphon=siphon(h);var level=h.getLevel();var pos=siphon.getBlockPos();
        for(int index=0;index<=Direction.values().length;index++){
            Direction side=index==Direction.values().length?null:Direction.values()[index];var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side);
            h.assertTrue(handler!=null&&handler.getSlots()==1,"Missing one-slot extraction capability on "+side);siphon.setItem(0,new ItemStack(seed(),8));
            h.assertValueEqual(handler.insertItem(0,new ItemStack(seed(),3),true).getCount(),3,"Simulation accepted seed insertion on "+side);
            h.assertValueEqual(handler.insertItem(0,new ItemStack(seed(),3),false).getCount(),3,"Automation inserted seeds on "+side);
            h.assertValueEqual(handler.insertItem(0,new ItemStack(Items.DIRT,3),false).getCount(),3,"Automation inserted foreign items on "+side);
            h.assertValueEqual(handler.extractItem(0,2,true).getCount(),2,"Extraction simulation reported the wrong count");h.assertValueEqual(siphon.getItem(0).getCount(),8,"Simulation mutated the output");
            h.assertValueEqual(handler.extractItem(0,2,false).getCount(),2,"Extraction failed on "+side);h.assertValueEqual(siphon.getItem(0).getCount(),6,"Extraction duplicated or lost seeds");
        }
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void menuRejectsManualAndShiftInsertionAndMovesOutputWithoutDuplication(GameTestHelper h){
        var siphon=siphon(h);var player=GameTestPlayers.create(h);var pos=siphon.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()-1.5);
        var menu=new VoidSiphonMenu(47,player.getInventory(),siphon);progress(h,siphon,1234);
        h.assertValueEqual(menu.progress(),1234,"Menu lost server progress");h.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(seed())),"Output slot accepted seeds manually");
        menu.setCarried(new ItemStack(seed(),3));menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertValueEqual(menu.getCarried().getCount(),3,"Manual output click consumed held seeds");h.assertTrue(siphon.isEmpty(),"Manual click filled the output");menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0,new ItemStack(seed(),7));menu.quickMoveStack(player,28);h.assertTrue(siphon.isEmpty(),"Shift-click inserted seeds into output");
        for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));siphon.setItem(0,new ItemStack(seed(),5));
        h.assertTrue(menu.quickMoveStack(player,0).isEmpty(),"Full player inventory accepted output");h.assertValueEqual(siphon.getItem(0).getCount(),5,"Failed extraction destroyed seeds");
        player.getInventory().setItem(8,ItemStack.EMPTY);h.assertValueEqual(menu.quickMoveStack(player,0).getCount(),5,"Shift-click missed the available hotbar slot");
        h.assertTrue(siphon.isEmpty(),"Successful shift-click left a duplicate output");h.assertValueEqual(player.getInventory().getItem(8).getCount(),5,"Hotbar extraction lost seeds");
        h.assertTrue(menu.quickMoveStack(player,-1).isEmpty()&&menu.quickMoveStack(player,999).isEmpty(),"Malformed menu index was accepted");
        h.assertTrue(menu.stillValid(player),"Nearby player lost menu access");player.setPos(player.getX()+12,player.getY(),player.getZ());h.assertFalse(menu.stillValid(player),"Distant player retained menu access");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void vanillaHoppersExtractBelowButCannotPushIntoTheSiphon(GameTestHelper h){
        var siphon=siphon(h);var level=h.getLevel();var pos=siphon.getBlockPos();siphon.setItem(0,new ItemStack(seed(),5));
        var lower=pos.below();level.setBlockAndUpdate(lower,Blocks.HOPPER.defaultBlockState());var hopper=(HopperBlockEntity)level.getBlockEntity(lower);
        HopperBlockEntity.pushItemsTick(level,lower,level.getBlockState(lower),hopper);
        h.assertValueEqual(count(hopper,seed()),1,"Real lower hopper failed to extract a seed");h.assertValueEqual(siphon.getItem(0).getCount(),4,"Hopper extraction duplicated or over-consumed output");
        var upper=pos.above();level.setBlockAndUpdate(upper,Blocks.HOPPER.defaultBlockState());var input=(HopperBlockEntity)level.getBlockEntity(upper);input.setItem(0,new ItemStack(seed(),3));
        for(int i=0;i<12;i++)HopperBlockEntity.pushItemsTick(level,upper,level.getBlockState(upper),input);
        h.assertValueEqual(count(input,seed()),3,"Real upper hopper inserted into the output slot");h.assertValueEqual(siphon.getItem(0).getCount(),4,"Rejected hopper insertion mutated siphon output");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void breakingDropsStoredSeedsOnceAndKeepsSourceCollisionAndResistance(GameTestHelper h){
        var siphon=siphon(h);var level=h.getLevel();var pos=siphon.getBlockPos();var state=siphon.getBlockState();siphon.setItem(0,new ItemStack(seed(),17));
        h.assertValueEqual(state.getDestroySpeed(level,pos),2f,"Source siphon hardness changed");h.assertValueEqual(state.getBlock().getExplosionResistance(),12f,"Source resistance20 was not converted to modern12");
        var shape=state.getCollisionShape(level,pos);h.assertTrue(Shapes.joinIsNotEmpty(shape,Block.box(6,12,6,7,13,7),BooleanOp.AND),"Siphon orb has no collision");
        h.assertFalse(Shapes.joinIsNotEmpty(shape,Block.box(6,11.1,6,7,11.9,7),BooleanOp.AND),"Gap below orb became solid");
        for(var direction:Direction.values())h.assertFalse(state.isFaceSturdy(level,pos,direction),"Siphon unexpectedly provides a sturdy face");
        level.destroyBlock(pos,true);h.assertValueEqual(drops(h,pos,seed()),17,"Breaking siphon lost or duplicated stored seeds");
        h.assertValueEqual(drops(h,pos,InfusionCraftingContent.VOID_SIPHON.get().asItem()),1,"Breaking siphon duplicated or lost its own block drop");
        level.removeBlock(pos,false);h.assertValueEqual(drops(h,pos,seed()),17,"Repeated removal duplicated the output");h.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void laterCollapseRewardsCurrentSiphonedSizeAndKeepsTheSnapshot(GameTestHelper h){
        var pos=h.absolutePos(CENTER);var rift=rift(h,pos.offset(0,2,3),100);
        for(int i=0;i<96;i++)rift.shrinkForSiphon();h.assertValueEqual(rift.size(),4,"Siphon shrink failed to reduce the pre-collapse rift");
        rift.collapse();var saved=new CompoundTag();rift.saveWithoutId(saved);h.assertValueEqual(saved.getInt("maxSize"),4,"Collapse retained historical pre-siphon reward size");
        rift.shrinkForSiphon();rift.collapse();saved=new CompoundTag();rift.saveWithoutId(saved);h.assertValueEqual(saved.getInt("maxSize"),4,"Repeated collapse reset the reward snapshot");
        rift.tick();rift.tick();h.assertTrue(rift.isRemoved(),"Small collapsing rift did not complete");
        h.assertValueEqual(drops(h,pos.offset(0,2,3),seed()),2,"Collapse used historical size100 instead of its size4 snapshot");h.succeed();
    }

    private static VoidSiphonEntity siphon(GameTestHelper h){h.setBlock(CENTER,InfusionCraftingContent.VOID_SIPHON.get());return (VoidSiphonEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER));}
    private static Item seed(){return BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:void_seed"));}
    private static FluxRift rift(GameTestHelper h,BlockPos pos,int size){
        var rift=WorldEntities.RIFT.get().create(h.getLevel());var tag=new CompoundTag();rift.saveWithoutId(tag);tag.putInt("size",size);tag.putInt("maxSize",size);tag.putFloat("stability",0);rift.load(tag);
        rift.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);rift.points();h.getLevel().addFreshEntity(rift);return rift;
    }
    private static void progress(GameTestHelper h,VoidSiphonEntity siphon,int amount){var tag=siphon.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt("progress",amount);siphon.loadWithComponents(tag,h.getLevel().registryAccess());}
    private static void tick(VoidSiphonEntity siphon,int count){for(int i=0;i<count;i++)siphon.tick();}
    private static void noShrink(GameTestHelper h,int rolls){h.getLevel().random.setSeed(rollSeed(h,false,rolls));}
    private static long rollSeed(GameTestHelper h,boolean success,int rolls){
        for(long seed=0;seed<10000;seed++){h.getLevel().random.setSeed(seed);boolean matches=true;for(int i=0;i<rolls;i++)if((h.getLevel().random.nextInt(33)==0)!=success)matches=false;if(matches)return seed;}
        throw new AssertionError("Could not select a deterministic source random sequence");
    }
    private static int count(net.minecraft.world.Container container,Item item){int count=0;for(int slot=0;slot<container.getContainerSize();slot++)if(container.getItem(slot).is(item))count+=container.getItem(slot).getCount();return count;}
    private static int drops(GameTestHelper h,BlockPos pos,Item item){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static void close(GameTestHelper h,double actual,double expected,String label){h.assertTrue(Math.abs(actual-expected)<1e-5,label+": expected "+expected+", actual "+actual);}
    private VoidSiphonGameTests(){}
}
