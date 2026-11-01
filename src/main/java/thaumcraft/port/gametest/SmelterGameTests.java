package thaumcraft.port.gametest;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.essentia.*;
import thaumcraft.port.world.AuraSystem;

/** Real machine ticks, inventory capabilities, furnace upgrades and world pollution. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class SmelterGameTests {
    private static final BlockPos POSITION=new BlockPos(2,2,2);

    @GameTest(template="empty",timeoutTicks=80)
    public static void everyTierAndAlumentumUseMachineLocalDistillationCadence(GameTestHelper helper){
        for(String name:List.of("smelter_basic","smelter_thaumium","smelter_void"))for(boolean boosted:List.of(false,true)){
            var machine=machine(helper,name);
            var jar=alembic(helper,machine.getBlockPos().above());
            machine.store.restore(Map.of(Aspect.AIR,3));machine.boost=boosted;
            int period=name.equals("smelter_thaumium")?10:15;if(boosted)period=(int)(period*.8);
            for(int tick=1;tick<period;tick++)machine.tick();
            helper.assertValueEqual(jar.tank.amount(),0,"Distillation used world time or ran too soon: "+name+" boost="+boosted);
            machine.tick();
            helper.assertValueEqual(jar.tank.amount(),1,"First original transfer cadence was missed: "+name+" boost="+boosted);
            helper.assertValueEqual(machine.store.total(),2,"Distillation did not conserve essentia");
            for(int tick=0;tick<period;tick++)machine.tick();
            helper.assertValueEqual(jar.tank.amount(),2,"Repeated transfer cadence changed");
            helper.getLevel().removeBlock(jar.getBlockPos(),false);
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void auxiliaryOutletsTransferIndependentlyAndPreferMatchingAlembics(GameTestHelper helper){
        var machine=machine(helper,"smelter_basic");
        var lower=alembic(helper,machine.getBlockPos().above());
        var upper=alembic(helper,machine.getBlockPos().above(2));upper.fill(Aspect.AIR,7,false);
        var extras=new java.util.ArrayList<JarEntity>();
        for(Direction side:List.of(Direction.SOUTH,Direction.WEST,Direction.EAST)){
            var pos=machine.getBlockPos().relative(side);
            helper.getLevel().setBlock(pos,EssentiaContent.AUX.get().defaultBlockState().setValue(SmelterAttachmentBlock.FACING,side.getOpposite()),3);
            extras.add(alembic(helper,pos.above()));
        }
        machine.store.restore(Map.of(Aspect.AIR,20));for(int tick=0;tick<15;tick++)machine.tick();
        helper.assertValueEqual(lower.tank.amount(),0,"Empty lower alembic took priority over an upper matching stack");
        helper.assertValueEqual(upper.tank.amount(),8,"Existing aspect stack was not topped up first");
        for(var jar:extras)helper.assertValueEqual(jar.tank.amount(),1,"Auxiliary outlet did not distill independently");
        helper.assertValueEqual(machine.store.total(),16,"Base and three auxiliary outlets did not transfer exactly four essentia");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void alumentumIs4800TickFuelAndSmeltingConservesEssentiaPlusFlux(GameTestHelper helper){
        var machine=machine(helper,"smelter_basic");
        var input=new ItemStack(Items.IRON_INGOT);
        int aspects=RecipeAspects.get(input,helper.getLevel()).values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(aspects>0,"Iron ingot lacks real recipe aspects");
        var fuel=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:alumentum")),2);
        helper.assertValueEqual(SmelterEntity.fuel(fuel),4800,"Alumentum fuel data map did not load in the running server");
        var aura=AuraSystem.get(helper.getLevel(),machine.getBlockPos());float before=aura.flux();
        machine.setItem(0,input);machine.setItem(1,fuel);machine.tick();
        helper.assertTrue(machine.boost,"Alumentum did not enable distillation boost");
        helper.assertValueEqual(machine.burn,4800,"Alumentum burned for the wrong duration");
        helper.assertValueEqual(machine.getItem(1).getCount(),1,"Smelter consumed more than one fuel");
        for(int tick=1;tick<aspects*2;tick++)machine.tick();
        helper.assertTrue(machine.getItem(0).isEmpty(),"Essentia smelting did not complete on the original aspect-dependent timer");
        helper.assertTrue(Math.abs(machine.store.total()+aura.flux()-before-aspects)<.001,
                "Distillation efficiency destroyed or duplicated aspects rather than converting loss into flux");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void fullSmelterWaitsWithoutConsumingInputOrFuel(GameTestHelper helper){
        var machine=machine(helper,"smelter_void");machine.store.restore(Map.of(Aspect.AIR,256));
        machine.setItem(0,new ItemStack(Items.IRON_INGOT));machine.setItem(1,new ItemStack(Items.COAL));
        for(int tick=0;tick<80;tick++)machine.tick();
        helper.assertValueEqual(machine.burn,0,"A full smelter ignited");
        helper.assertValueEqual(machine.store.total(),256,"A full smelter changed its stored aspects");
        helper.assertTrue(machine.getItem(0).is(Items.IRON_INGOT)&&machine.getItem(1).is(Items.COAL),"Full storage consumed input or fuel");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void breakingSmelterReleasesStoredEssentiaButLightingChangesDoNot(GameTestHelper helper){
        var machine=machine(helper,"smelter_thaumium");machine.store.restore(Map.of(Aspect.AIR,13,Aspect.FLUX,7));
        var aura=AuraSystem.get(helper.getLevel(),machine.getBlockPos());float before=aura.flux();
        helper.getLevel().setBlock(machine.getBlockPos(),machine.getBlockState().setValue(SmelterBlock.LIT,true),3);
        helper.assertValueEqual(aura.flux(),before,"Lighting a smelter released stored essentia");
        helper.getLevel().removeBlock(machine.getBlockPos(),false);
        helper.assertTrue(Math.abs(aura.flux()-before-20)<.001,"Breaking the smelter did not release every stored essentia as flux");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void sidedInventoryComparatorAndFuelShiftFallbackMatchOriginal(GameTestHelper helper){
        var machine=machine(helper,"smelter_basic");var level=helper.getLevel();var pos=machine.getBlockPos();
        var up=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
        var side=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.EAST);
        var bottom=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.DOWN);
        helper.assertTrue(up!=null&&side!=null&&bottom!=null,"Smelter inventory capability was not registered");
        helper.assertValueEqual(up.getSlots(),0,"Top exposes an original-disabled input");
        helper.assertValueEqual(side.getSlots(),1,"Side should expose only ingredient slot");
        helper.assertValueEqual(bottom.getSlots(),1,"Bottom should expose only fuel slot");
        helper.assertTrue(bottom.insertItem(0,new ItemStack(Items.COAL,64),false).isEmpty(),"Fuel failed to enter from below");
        var player=GameTestPlayers.create(helper);var menu=new SmelterMenu(1,player.getInventory(),machine,machine.data);
        player.getInventory().setItem(9,new ItemStack(Items.COAL,5));
        helper.assertFalse(menu.quickMoveStack(player,2).isEmpty(),"Shift-click failed when fuel was full but the ingredient slot could accept coal");
        helper.assertTrue(machine.getItem(0).is(Items.COAL)&&machine.getItem(0).getCount()==5,"Fuel did not fall back to the ingredient slot");
        machine.setItem(0,new ItemStack(Items.IRON_INGOT,64));
        helper.assertValueEqual(machine.getBlockState().getAnalogOutputSignal(level,pos),15,"Full inventory did not produce comparator signal 15");
        helper.assertFalse(machine.canPlaceItemThroughFace(0,new ItemStack(Items.IRON_INGOT),Direction.UP),"Ingredient entered from the disabled top");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void bellowsRespectFacingPowerAndTwoTickFurnaceCadence(GameTestHelper helper){
        var machine=machine(helper,"smelter_basic");
        for(Direction direction:Direction.Plane.HORIZONTAL)helper.getLevel().setBlock(machine.getBlockPos().relative(direction),
                EssentiaContent.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,direction.getOpposite()),2);
        helper.assertValueEqual(BellowsEntity.count(helper.getLevel(),machine.getBlockPos(),Direction.NORTH),3,"Smelter bellows include its front");
        var east=machine.getBlockPos().east();helper.getLevel().setBlock(east,helper.getLevel().getBlockState(east).setValue(BellowsBlock.ENABLED,false),2);
        helper.assertValueEqual(BellowsEntity.count(helper.getLevel(),machine.getBlockPos(),Direction.NORTH),2,"Powered bellows still accelerated smelting");
        helper.getLevel().setBlock(machine.getBlockPos(),Blocks.FURNACE.defaultBlockState(),3);
        var furnace=(FurnaceBlockEntity)helper.getLevel().getBlockEntity(machine.getBlockPos());
        furnace.litTime=100;furnace.cookingProgress=10;furnace.cookingTotalTime=200;
        var bellows=(BellowsEntity)helper.getLevel().getBlockEntity(machine.getBlockPos().west());
        bellows.tick();helper.assertValueEqual(furnace.cookingProgress,10,"Bellows accelerated before the second tick");
        bellows.tick();helper.assertValueEqual(furnace.cookingProgress,11,"Bellows did not accelerate on the second tick");
        helper.assertValueEqual(furnace.litTime,100,"Bellows changed fuel burn time");
        furnace.cookingProgress=199;bellows.tick();bellows.tick();
        helper.assertValueEqual(furnace.cookingProgress,199,"Bellows bypassed vanilla recipe completion");
        helper.succeed();
    }

    private static SmelterEntity machine(GameTestHelper helper,String name){
        var pos=helper.absolutePos(POSITION);helper.getLevel().removeBlock(pos,false);
        helper.getLevel().setBlock(pos,EssentiaContent.SMELTERS.get(name).get().defaultBlockState(),3);
        return (SmelterEntity)helper.getLevel().getBlockEntity(pos);
    }
    private static JarEntity alembic(GameTestHelper helper,BlockPos pos){
        helper.getLevel().setBlock(pos,EssentiaContent.ALEMBIC.get().defaultBlockState(),3);
        return (JarEntity)helper.getLevel().getBlockEntity(pos);
    }
}
