package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import thaumcraft.port.artifice.device.*;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.world.AuraSystem;
import static thaumcraft.port.artifice.device.ArtificeDeviceBlock.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArtificeDeviceGameTests {
    private static final BlockPos P=new BlockPos(2,3,2);
    private static ArtificeDeviceEntity place(GameTestHelper h,Block block){h.setBlock(P.below(),Blocks.STONE);h.setBlock(P,block);return (ArtificeDeviceEntity)h.getBlockEntity(P);}
    private static void close(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<.0001,message+": "+actual+" != "+expected);}

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void levitatorConsumesOneAuraFor1200WorkAndChargesEachLift(GameTestHelper h){
        var be=place(h,CraftingContent.LEVITATOR.get());var aura=AuraSystem.get(h.getLevel(),be.getBlockPos());aura.addVis(100);float before=aura.vis();
        ItemEntity item=new ItemEntity(h.getLevel(),be.getBlockPos().getX()+.5,be.getBlockPos().getY()+1.2,be.getBlockPos().getZ()+.5,new ItemStack(Items.STICK));item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);
        be.tick();close(h,aura.vis(),before-1,"Levitator aura payment");h.assertValueEqual(be.vis,1184,"Default eight-block range costs16 per lifted entity");close(h,item.getDeltaMovement().y,.1f,"Upward impulse");h.assertValueEqual(item.fallDistance,0f,"Lift did not reset fall damage");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void levitatorCrouchHorizontalLiftAndSpeedCapsMatchSource(GameTestHelper h){
        close(h,ArtificeDeviceEntity.liftVelocity(new Vec3(.1,-.2,.1),Direction.UP,true,false).y,-.18f,"Crouching descent damping");
        var horizontal=ArtificeDeviceEntity.liftVelocity(new Vec3(1,-.2,-1),Direction.EAST,false,false);close(h,horizontal.x,.35f,"Positive speed cap");close(h,horizontal.z,-.35f,"Negative speed cap");close(h,horizontal.y,-.1f,"Horizontal airborne compensation");
        close(h,ArtificeDeviceEntity.liftVelocity(new Vec3(0,-.2,0),Direction.EAST,false,true).y,-.2,"Grounded horizontal lift changed vertical velocity");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void levitatorRangeStopsAtSolidBlocksAndRedstoneDisablesWork(GameTestHelper h){
        var be=place(h,CraftingContent.LEVITATOR.get());be.vis=1200;h.setBlock(P.above(3),Blocks.STONE);for(int i=0;i<20;i++)be.tick();h.assertValueEqual(be.rangeActual,2,"Initial scan must stop before solid obstruction");
        h.setBlock(P.above(3),Blocks.AIR);for(int i=0;i<8;i++)be.tick();h.assertValueEqual(be.rangeActual,8,"Cleared obstruction did not restore range");
        h.setBlock(P.above(3),Blocks.STONE);for(int i=0;i<8;i++)be.tick();h.assertValueEqual(be.rangeActual,3,"New obstruction must truncate established range at its position");
        h.setBlock(P.east(),Blocks.REDSTONE_BLOCK);h.assertFalse(be.enabled(),"Redstone did not disable levitator");int before=be.vis;be.tick();h.assertValueEqual(be.vis,before,"Disabled device spent work");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void relayThresholdAndTwoTickOutputRespectFacing(GameTestHelper h){
        var be=place(h,CraftingContent.REDSTONE_RELAY.get());be.input=12;be.output=7;var pos=be.getBlockPos();Direction face=be.facing();
        h.getLevel().setBlockAndUpdate(pos.relative(face),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertFalse(be.enabled(),"Relay skipped its two-tick delay");
        h.runAfterDelay(3,()->{h.assertTrue(be.enabled(),"Threshold-qualified input did not power relay");h.assertValueEqual(h.getLevel().getSignal(pos,face),7,"Configured relay output ignored");h.assertValueEqual(h.getLevel().getSignal(pos,face.getOpposite()),0,"Relay powered its input side");
            h.getLevel().removeBlock(pos.relative(face),false);h.runAfterDelay(3,()->{h.assertFalse(be.enabled(),"Relay did not turn off after input disappeared");h.succeed();});});
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void relayRejectsWeakInputBelowSelectedThreshold(GameTestHelper h){
        var be=place(h,CraftingContent.REDSTONE_RELAY.get());be.input=12;var source=be.getBlockPos().relative(be.facing());
        h.getLevel().setBlockAndUpdate(source.below(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlock(source,Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState().setValue(WeightedPressurePlateBlock.POWER,7),2);
        h.assertValueEqual(h.getLevel().getSignal(source,be.facing()),7,"Fixture must provide an actual seven-strength signal");
        h.assertFalse(((ArtificeDeviceBlock)be.getBlockState().getBlock()).shouldPower(h.getLevel(),be.getBlockPos(),be.getBlockState()),"Weak input crossed threshold");be.input=7;
        h.assertTrue(((ArtificeDeviceBlock)be.getBlockState().getBlock()).shouldPower(h.getLevel(),be.getBlockPos(),be.getBlockState()),"Equal threshold did not qualify");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void relayPlacementOntoLiveInputUsesOriginalOneTickStartup(GameTestHelper h){
        h.setBlock(P.north(),Blocks.REDSTONE_BLOCK);var be=place(h,CraftingContent.REDSTONE_RELAY.get());h.assertFalse(be.enabled(),"Placement skipped startup tick");
        h.runAfterDelay(1,()->{h.assertTrue(be.enabled(),"Powered placement incorrectly used the two-tick input-change delay");h.succeed();});
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void earsHearRealNoteBlockEventsAndPulseExactlyTenTicks(GameTestHelper h){
        var be=place(h,CraftingContent.ARCANE_EAR.get());be.note=6;be.onLoad();h.assertValueEqual(be.tone,NoteBlockInstrument.BASEDRUM.ordinal(),"Backing stone instrument incorrect");
        BlockPos n=be.getBlockPos().east(2);h.getLevel().setBlockAndUpdate(n,Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE,6).setValue(NoteBlock.INSTRUMENT,NoteBlockInstrument.BASEDRUM));
        h.getLevel().blockEvent(n,Blocks.NOTE_BLOCK,0,0);
        h.runAfterDelay(2,()->{h.assertTrue(be.enabled(),"Real note event did not reach ear");h.assertTrue(be.pulse>0&&be.pulse<=10,"Pulse duration out of range");int remaining=be.pulse;for(int i=0;i<remaining-1;i++)be.tick();h.assertTrue(be.enabled(),"Pulse expired early");be.tick();h.assertFalse(be.enabled(),"Pulse did not expire");h.succeed();});
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void earRejectsWrongPitchInstrumentAndBeyond64Blocks(GameTestHelper h){
        var be=place(h,CraftingContent.ARCANE_EAR.get());be.note=5;be.onLoad();var l=h.getLevel();var pos=be.getBlockPos();
        ArtificeDeviceEntity.hearNote(l,pos,NoteBlockInstrument.BASEDRUM,4);ArtificeDeviceEntity.hearNote(l,pos,NoteBlockInstrument.HARP,5);ArtificeDeviceEntity.hearNote(l,pos.east(65),NoteBlockInstrument.BASEDRUM,5);h.assertFalse(be.enabled(),"Ear accepted unrelated note");
        ArtificeDeviceEntity.hearNote(l,pos.east(64),NoteBlockInstrument.BASEDRUM,5);h.assertTrue(be.enabled(),"64-block inclusive hearing boundary lost");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void toggleEarKeepsStateAndTogglesOncePerMatchingTick(GameTestHelper h){
        var be=place(h,ArtificeDevices.ARCANE_EAR_TOGGLE.get());be.onLoad();var l=h.getLevel();var p=be.getBlockPos();
        ArtificeDeviceEntity.hearNote(l,p,NoteBlockInstrument.BASEDRUM,0);ArtificeDeviceEntity.hearNote(l,p,NoteBlockInstrument.BASEDRUM,0);h.assertTrue(be.enabled(),"Repeated same-tick notes toggled twice");
        for(int i=0;i<20;i++)be.tick();h.assertTrue(be.enabled(),"Toggle ear was treated as pulse ear");
        h.runAfterDelay(2,()->{ArtificeDeviceEntity.hearNote(l,p,NoteBlockInstrument.BASEDRUM,0);h.assertFalse(be.enabled(),"Second matching note did not release latch");h.succeed();});
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void dioptraSamplesVisFluxAndComparatorWithoutDrainingAura(GameTestHelper h){
        var be=place(h,CraftingContent.DIOPTRA.get());var aura=AuraSystem.get(h.getLevel(),be.getBlockPos());aura.drain(false,Float.MAX_VALUE,false);aura.drain(true,Float.MAX_VALUE,false);aura.addVis(250);aura.addFlux(125);
        be.sampleAura();h.assertValueEqual((int)be.grid[84],32,"Center vis sample");h.assertValueEqual(be.comparator(),8,"Vis comparator");close(h,aura.vis(),250,"Dioptra consumed vis");
        h.getLevel().setBlockAndUpdate(be.getBlockPos(),be.getBlockState().setValue(ENABLED,false));be.sampleAura();h.assertValueEqual((int)be.grid[84],16,"Center flux sample");h.assertValueEqual(be.comparator(),4,"Flux comparator");close(h,aura.flux(),125,"Dioptra consumed flux");
        aura.addFlux(1000);be.sampleAura();h.assertValueEqual((int)be.grid[84],64,"Grid must cap at64");h.assertValueEqual(be.comparator(),15,"Full comparator output");h.succeed();
    }
    private static ArtificeDeviceEntity crafter(GameTestHelper h){var be=place(h,CraftingContent.PATTERN_CRAFTER.get());h.setBlock(P.above(),Blocks.BARREL);h.setBlock(P.below(),Blocks.BARREL);be.power=5;return be;}
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void patternCrafterConsumesAcrossSlotsAndOutputsVanillaRecipe(GameTestHelper h){
        var be=crafter(h);var input=(BarrelBlockEntity)h.getBlockEntity(P.above());var output=(BarrelBlockEntity)h.getBlockEntity(P.below());input.setItem(0,new ItemStack(Items.IRON_NUGGET,3));input.setItem(1,new ItemStack(Items.IRON_NUGGET,6));
        h.assertTrue(be.craftOnce(),"Nine-nugget recipe not crafted");h.assertTrue(input.isEmpty(),"Source ingredients were not consumed across slots");h.assertTrue(output.getItem(0).is(Items.IRON_INGOT)&&output.getItem(0).getCount()==1,"Craft output incorrect");close(h,be.power,4,"Per-craft aura cost");h.assertFalse(be.craftOnce(),"Empty source duplicated a craft");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void patternCrafterFullDestinationNeverSpendsIngredientsOrBufferedPower(GameTestHelper h){
        var be=crafter(h);be.pattern=4;var input=(BarrelBlockEntity)h.getBlockEntity(P.above());var output=(BarrelBlockEntity)h.getBlockEntity(P.below());input.setItem(0,new ItemStack(Items.OAK_PLANKS,4));for(int i=0;i<output.getContainerSize();i++)output.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        h.assertFalse(be.craftOnce(),"Full output inventory accepted a craft");h.assertValueEqual(input.getItem(0).getCount(),4,"Failed craft spent inputs");close(h,be.power,5,"Failed craft spent buffered power");output.clearContent();h.assertTrue(be.craftOnce(),"Freed output did not resume crafting");h.assertTrue(output.getItem(0).is(Items.CRAFTING_TABLE),"2×2 pattern wrong");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void patternCrafterAllTenSourceLayoutsAndContainerReservations(GameTestHelper h){
        int[] widths={3,1,2,1,2,3,1,3,2,3},heights={3,1,1,2,2,1,3,2,3,3},counts={9,1,2,2,4,3,3,6,6,8};
        for(int i=0;i<10;i++){var in=ArtificeDeviceEntity.patternInput(i,new ItemStack(Items.STICK));h.assertValueEqual(in.width(),widths[i],"Pattern width"+i);h.assertValueEqual(in.height(),heights[i],"Pattern height"+i);h.assertValueEqual(in.ingredientCount(),counts[i],"Pattern count"+i);}
        var oneSlot=new ItemStackHandler(1);oneSlot.setStackInSlot(0,new ItemStack(Items.BUCKET,15));
        h.assertTrue(ArtificeDeviceEntity.reserveOutputs(oneSlot,List.of(new ItemStack(Items.BUCKET),new ItemStack(Items.BUCKET)))==null,"Independent remainder simulations overbooked a shared slot");h.assertValueEqual(oneSlot.getStackInSlot(0).getCount(),15,"Output simulation mutated inventory");
        h.assertTrue(ArtificeDeviceEntity.reserveOutputs(new ItemStackHandler(2),List.of(new ItemStack(Items.BUCKET),new ItemStack(Items.WATER_BUCKET)))!=null,"Distinct container returns rejected with enough capacity");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void patternCrafterOreEquivalenceCannotConsumeCustomNamedMaterials(GameTestHelper h){
        var input=new ItemStackHandler(2);input.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,8));var named=new ItemStack(Items.IRON_INGOT);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep this ingot"));input.setStackInSlot(1,named);
        h.assertTrue(ArtificeDeviceEntity.reserveInputs(input,new ItemStack(Items.IRON_INGOT),9)==null,"Ore-equivalence consumed a custom named material");h.assertValueEqual(input.getStackInSlot(0).getCount(),8,"Failed reservation changed ordinary items");h.assertTrue(input.getStackInSlot(1).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME),"Failed reservation lost the custom name");
        h.assertTrue(ArtificeDeviceEntity.reserveInputs(input,named,1)!=null,"Exact component-preserving input match was rejected");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void visGeneratorPaysOnlyWhenEmptyAndPushesAtMost20(GameTestHelper h){
        var be=place(h,CraftingContent.VIS_GENERATOR.get());var aura=AuraSystem.get(h.getLevel(),be.getBlockPos());aura.addVis(100);float before=aura.vis();be.generate();h.assertValueEqual(be.energy,1000,"One vis must produce1000FE");close(h,aura.vis(),before-1,"Initial vis payment");be.generate();close(h,aura.vis(),before-1,"Nonempty buffer recharged");
        var sink=new EnergyStorage(1000);be.sendEnergy(sink);h.assertValueEqual(sink.getEnergyStored(),20,"Generator exceeded20FE per tick");h.assertValueEqual(be.energy,980,"Generator buffer debit");
        var restricted=new EnergyStorage(7);be.sendEnergy(restricted);h.assertValueEqual(be.energy,973,"Generator debited energy rejected by recipient");h.assertValueEqual(be.extractEnergy(1000,false),0,"Source is push-only");h.assertValueEqual(be.receiveEnergy(1000,false),0,"Generator accepted external energy");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void visGeneratorCapabilityIsOnlyOnOutputAndRedstoneStopsRecharge(GameTestHelper h){
        var be=place(h,CraftingContent.VIS_GENERATOR.get());for(Direction side:Direction.values()){var cap=h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),side);h.assertTrue((cap!=null)==(side==be.facing()),"Energy exposed on wrong side "+side);}
        h.assertTrue(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),null)==null,"Unsided energy capability exposed");h.setBlock(P.east(),Blocks.REDSTONE_BLOCK);be.energy=0;be.tick();h.assertValueEqual(be.energy,0,"Redstone-disabled generator recharged");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void deviceSettingsBuffersAndPulseSurviveSaveAndClientUpdate(GameTestHelper h){
        var be=place(h,CraftingContent.LEVITATOR.get());be.range=3;be.vis=999;be.input=7;be.output=13;be.note=24;be.tone=5;be.pulse=7;be.pattern=9;be.power=3.5f;be.energy=777;be.grid[84]=63;
        var saved=be.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new ArtificeDeviceEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertValueEqual(copy.rangeLimit(),32,"Range lost");h.assertValueEqual(copy.vis,999,"Levitator work lost");h.assertValueEqual(copy.input,7,"Relay threshold lost");h.assertValueEqual(copy.output,13,"Relay output lost");h.assertValueEqual(copy.note,24,"Pitch lost");h.assertValueEqual(copy.pulse,7,"Pulse lost");h.assertValueEqual(copy.pattern,9,"Pattern lost");close(h,copy.power,3.5,"Craft buffer lost");h.assertValueEqual(copy.energy,777,"Energy lost");h.assertValueEqual((int)copy.grid[84],63,"Dioptra grid lost");
        h.assertTrue(be.getUpdateTag(h.getLevel().registryAccess()).contains("grid_a"),"Client grid omitted");h.succeed();
    }
}
