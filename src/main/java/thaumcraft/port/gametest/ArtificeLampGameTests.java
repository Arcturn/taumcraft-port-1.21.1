package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.lamp.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.content.*;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArtificeLampGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void arcaneGlimmersRespectWallsAndAreRemovedWithPowerAndBreak(GameTestHelper helper) {
        var lamp=lamp(helper,CraftingContent.LAMP_ARCANE.get().defaultBlockState());
        var level=helper.getLevel(); var pos=lamp.getBlockPos(); var target=pos.east(7);
        level.setBlock(pos.east(3),Blocks.STONE.defaultBlockState(),3);
        helper.assertFalse(lamp.tryLight(target),"Arcane lamp illuminated through a solid wall");
        level.removeBlock(pos.east(3),false);
        helper.assertTrue(level.isEmptyBlock(target),"Glimmer fixture target is occupied: "+level.getBlockState(target));
        helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,target)<11,"Glimmer fixture target already has sufficient block light");
        helper.assertTrue(lamp.tryLight(target),"Unobstructed dark space did not receive a glimmer");
        helper.assertTrue(level.getBlockState(target).is(ArtificeLamps.GLIMMER.get()),"Glimmer was not actually placed");
        level.setBlock(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        helper.assertFalse(level.getBlockState(target).is(ArtificeLamps.GLIMMER.get()),"Redstone left stale distant light behind");
        helper.assertFalse(level.getBlockState(pos).getValue(ArcaneLampBlock.LIT),"Powered lamp remains lit");
        helper.assertFalse(lamp.tryLight(target),"Powered lamp created another light");
        level.removeBlock(pos.above(),false);
        // Direct placement avoids stale light-engine work during this single-tick fixture.
        level.setBlock(target,ArtificeLamps.GLIMMER.get().defaultBlockState(),3);
        level.removeBlock(pos,false);
        helper.assertFalse(level.getBlockState(target).is(ArtificeLamps.GLIMMER.get()),"Breaking a lamp left orphaned glimmers");
        helper.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void growthLampPullsOnlyHerbaAtItsMountAndPersistsReserve(GameTestHelper helper) {
        var lamp=lamp(helper,InfusionCraftingContent.LAMP_GROWTH.get().defaultBlockState());
        var jar=jarBelow(helper,lamp); jar.fill(Aspect.FIRE,3,false);
        for(int i=0;i<20;i++)lamp.tick();
        helper.assertTrue(lamp.charges()<=0,"Growth lamp consumed an incorrect aspect");
        helper.assertValueEqual(jar.storedAmount(),3,"Wrong-aspect source lost essentia");
        jar.tank.restore(Aspect.PLANT,3,null);
        for(int i=0;i<20;i++)lamp.tick();
        helper.assertValueEqual(lamp.charges(),20,"One Herba must supply twenty growth attempts");
        helper.assertTrue(lamp.hasReserve(),"Growth lamp failed to buffer its second unit");
        helper.assertValueEqual(jar.storedAmount(),1,"Growth lamp consumed more than active plus reserve capacity");
        helper.assertValueEqual(lamp.suction(Direction.DOWN),0,"Full growth lamp must stop pulling");
        helper.assertFalse(lamp.connects(Direction.NORTH),"Growth lamp accepted essentia away from its mount");
        var copy=new LampEntity(lamp.getBlockPos(),lamp.getBlockState());
        copy.loadWithComponents(lamp.saveWithoutMetadata(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertValueEqual(copy.charges(),20,"Reload lost growth charges");
        helper.assertTrue(copy.hasReserve(),"Reload lost the reserved unit");
        helper.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void growthLampAdvancesCropsAndStopsUnderRedstone(GameTestHelper helper) {
        var lamp=lamp(helper,InfusionCraftingContent.LAMP_GROWTH.get().defaultBlockState());
        var level=helper.getLevel(); var pos=lamp.getBlockPos(); var crop=pos.east(2);
        level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,7),3);
        level.setBlock(crop,Blocks.WHEAT.defaultBlockState(),3);
        jarBelow(helper,lamp).fill(Aspect.PLANT,20,false);
        for(int i=0;i<5000 && level.getBlockState(crop).getValue(CropBlock.AGE)==0;i++)lamp.tick();
        helper.assertTrue(level.getBlockState(crop).getValue(CropBlock.AGE)>0,"Growth lamp never delivered crop updates");
        level.setBlock(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        lamp.tick(); // Any already scheduled crop tick may finish, like vanilla scheduled ticks.
        int age=level.getBlockState(crop).getValue(CropBlock.AGE);
        for(int i=0;i<200;i++)lamp.tick();
        helper.assertValueEqual(level.getBlockState(crop).getValue(CropBlock.AGE),age,"Powered growth lamp kept scheduling growth");
        helper.assertFalse(level.getBlockState(pos).getValue(ArcaneLampBlock.LIT),"Powered growth lamp emitted light");
        helper.succeed();
    }

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fertilityLampBreedsOnePairAndHonorsSpeciesPopulationLimit(GameTestHelper helper) {
        var lamp=lamp(helper,InfusionCraftingContent.LAMP_FERTILITY.get().defaultBlockState());
        var jar=jarBelow(helper,lamp); jar.fill(Aspect.DESIRE,20,false);
        var first=cow(helper,lamp.getBlockPos().east(2)); var second=cow(helper,lamp.getBlockPos().west(2));
        for(int i=0;i<10;i++)lamp.tick();
        helper.assertTrue(first.isInLove() && second.isInLove(),"Fertility lamp did not put both adult partners in love");
        helper.assertValueEqual(lamp.charges(),-3,"Original two-unit activation must deduct five, retaining a debt");
        helper.assertValueEqual(jar.storedAmount(),18,"Fertility lamp did not take exactly two initial units");
        first.resetLove();second.resetLove();
        for(int i=0;i<8;i++)cow(helper,lamp.getBlockPos().south(2));
        for(int i=0;i<400;i++)lamp.tick();
        helper.assertFalse(first.isInLove() || second.isInLove(),"Lamp bred a species with ten nearby animals");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void hungryChestTracksMultipleViewersAndClosesAfterBothLeave(GameTestHelper helper) {
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        helper.getLevel().setBlock(pos,CraftingContent.HUNGRY_CHEST.get().defaultBlockState(),3);
        var chest=(HungryChestEntity)helper.getLevel().getBlockEntity(pos);
        var first=helper.makeMockPlayer(GameType.SURVIVAL); var second=helper.makeMockPlayer(GameType.SURVIVAL);
        var firstMenu=chest.createMenu(1,first.getInventory(),first);
        var secondMenu=chest.createMenu(2,second.getInventory(),second);
        helper.assertValueEqual(chest.openCount(),2,"Both menu viewers must keep the lid open");
        firstMenu.removed(first);
        helper.assertValueEqual(chest.openCount(),1,"First viewer closed another player's chest");
        secondMenu.removed(second);
        helper.assertValueEqual(chest.openCount(),0,"Chest opener count did not return to zero");
        helper.succeed();
    }

    private static LampEntity lamp(GameTestHelper helper,BlockState state) {
        var pos=helper.absolutePos(new BlockPos(2,3,2));
        helper.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        helper.getLevel().setBlock(pos,state,3);
        return (LampEntity)helper.getLevel().getBlockEntity(pos);
    }
    private static JarEntity jarBelow(GameTestHelper helper,LampEntity lamp) {
        helper.getLevel().setBlock(lamp.getBlockPos().below(),EssentiaContent.JAR.get().defaultBlockState(),3);
        return (JarEntity)helper.getLevel().getBlockEntity(lamp.getBlockPos().below());
    }
    private static Cow cow(GameTestHelper helper,BlockPos pos) {
        var cow=EntityType.COW.create(helper.getLevel()); cow.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);cow.setNoAi(true);helper.getLevel().addFreshEntity(cow);return cow;
    }
    private ArtificeLampGameTests() {}
}
