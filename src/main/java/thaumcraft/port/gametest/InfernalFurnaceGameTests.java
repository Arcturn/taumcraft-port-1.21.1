package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.port.artifice.furnace.*;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.essentia.*;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.*;

/** Real world construction, saved furnace ticks, capabilities and drops; no GUI-only recipe assertions. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfernalFurnaceGameTests {
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    @GameTest(template="infernal_furnace_empty",timeoutTicks=80)
    public static void salisRequiresResearchPaysOnceAndFormsFromAnyBlueprintPart(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(CENTER);blueprint(h,Direction.EAST);
        var player=GameTestPlayers.create(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.SALIS_MUNDUS.get(),2));
        var clicked=center.offset(-1,1,-1);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false));
        h.assertFalse(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Unknown research formed the furnace");
        h.assertValueEqual(player.getMainHandItem().getCount(),2,"Failed research check charged dust");
        var knowledge=ResearchStorage.get(level.getServer()).player(player.getUUID());knowledge.stage("INFERNALFURNACE",1);
        h.assertTrue(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Started research must satisfy original non-strict dust gate");
        h.assertValueEqual(player.getMainHandItem().getCount(),1,"Formation did not reserve exactly one dust");
        h.assertFalse(PortContent.SALIS_MUNDUS.get().useOn(context).consumesAction(),"Duplicate ritual reserved the same structure twice");
        h.runAfterDelay(48,()->h.assertTrue(level.getBlockState(center).is(Blocks.LAVA),"Furnace formed before its 50-tick ritual"));
        h.runAfterDelay(53,()->{
            h.assertTrue(level.getBlockState(center).is(InfernalFurnaceContent.FURNACE.get()),"Delayed ritual never formed the core");
            var furnace=(InfernalFurnaceEntity)level.getBlockEntity(center);
            h.assertValueEqual(furnace.output(),Direction.EAST,"Output must follow the bars, not player yaw");
            h.assertValueEqual(shellCount(h),24,"Formation lost a load-bearing shell block");
            h.assertTrue(level.isEmptyBlock(center.east()),"Formation did not remove the mouth bars");
            h.assertValueEqual(player.getMainHandItem().getCount(),1,"Completion charged dust twice");
            h.assertTrue(knowledge.crafted().contains("thaumcraft:infernal_furnace"),"Formation did not fire the crafting observation");
            h.succeed();
        });
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void allOrientationsKeepTopOpeningAndRevertTheirOriginalMaterials(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(CENTER);
        for(var direction:Direction.Plane.HORIZONTAL){
            blueprint(h,direction);level.setBlockAndUpdate(center.above(),Blocks.HOPPER.defaultBlockState());
            var match=InfernalFurnaceStructure.match(level,center,direction);
            h.assertTrue(match!=null&&match.parts().size()==26,"Source blueprint must contain exactly 26 required positions");
            h.assertTrue(InfernalFurnaceStructure.form(level,center,direction),"Valid rotated furnace failed to form: "+direction);
            h.assertTrue(level.getBlockState(center.above()).is(Blocks.HOPPER),"Formation consumed the unspecified top opening");
            h.assertValueEqual(((InfernalFurnaceEntity)level.getBlockEntity(center)).output(),direction,"Rotated mouth points the wrong way");
            var broken=center.offset(1,1,1);level.destroyBlock(broken,true);
            h.assertTrue(level.isEmptyBlock(broken),"Broken shell was restored as a second harvestable block");
            h.assertTrue(level.getBlockState(center).is(Blocks.LAVA),"Dismantling did not restore lava");
            h.assertTrue(level.getBlockState(center.relative(direction)).is(Blocks.IRON_BARS),"Dismantling did not restore mouth bars");
            h.assertValueEqual(shellCount(h),0,"Dismantling left invisible collision blocks");
            for(var part:match.parts())if(!part.pos().equals(broken)&&!part.pos().equals(center))
                h.assertTrue(level.getBlockState(part.pos()).is(part.source().getBlock()),"Dismantling restored the wrong material");
        }
        h.assertValueEqual(drops(h,Items.NETHER_BRICKS),4,"Four broken brick shells must yield exactly four blocks");
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=90)
    public static void failedRitualRefundSurvivesAboveLavaWhenInventoryIsFull(GameTestHelper h){
        blueprint(h,Direction.SOUTH);var level=h.getLevel();var center=h.absolutePos(CENTER);var player=GameTestPlayers.create(h);
        ResearchStorage.get(level.getServer()).player(player.getUUID()).stage("INFERNALFURNACE",1);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.SALIS_MUNDUS.get()));
        var clicked=center.offset(1,1,1);
        h.assertTrue(PortContent.SALIS_MUNDUS.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false))).consumesAction(),"Fixture ritual did not start");
        for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,64));
        level.setBlockAndUpdate(clicked,Blocks.STONE.defaultBlockState());
        h.runAfterDelay(78,()->{
            h.assertTrue(level.getBlockState(center).is(Blocks.LAVA),"Invalid ritual still converted the core");
            var refunds=level.getEntitiesOfClass(ItemEntity.class,bounds(h),e->e.getItem().is(PortContent.SALIS_MUNDUS.get()));
            h.assertValueEqual(refunds.stream().mapToInt(e->e.getItem().getCount()).sum(),1,"Failed ritual lost or duplicated paid dust");
            h.assertTrue(refunds.getFirst().isNoGravity()&&refunds.getFirst().getY()>center.getY()+2,"Refund fell into the original lava core");
            h.succeed();
        });
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void coreBreakDropsQueueOnceAndRestoresLava(GameTestHelper h){
        var furnace=formed(h,Direction.SOUTH);var level=h.getLevel();var center=furnace.getBlockPos();
        furnace.setItem(0,new ItemStack(Items.RAW_IRON,17));furnace.setItem(31,new ItemStack(Items.BEEF,3));
        level.destroyBlock(center,true);
        h.assertTrue(level.getBlockState(center).is(Blocks.LAVA),"Breaking the core did not leave its original lava");
        h.assertValueEqual(shellCount(h),0,"Core removal left invisible shell parts");
        h.assertValueEqual(drops(h,Items.RAW_IRON),17,"Core removal lost or duplicated the first queued stack");
        h.assertValueEqual(drops(h,Items.BEEF),3,"Core removal lost or duplicated the last queued stack");
        h.assertValueEqual(drops(h,InfernalFurnaceContent.FURNACE.get().asItem()),0,"The formed furnace must not duplicate itself as an item");
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void invalidBlueprintCannotPartiallyReplaceBlocks(GameTestHelper h){
        blueprint(h,Direction.NORTH);var level=h.getLevel();var center=h.absolutePos(CENTER);var bad=center.offset(1,-1,-1);
        level.setBlockAndUpdate(bad,Blocks.STONE.defaultBlockState());
        h.assertFalse(InfernalFurnaceStructure.form(level,center,Direction.NORTH),"Invalid corner formed a partial furnace");
        h.assertValueEqual(shellCount(h),0,"Failed matching replaced original inputs");
        h.assertTrue(level.getBlockState(center).is(Blocks.LAVA)&&level.getBlockState(center.north()).is(Blocks.IRON_BARS),"Failed matching consumed lava or bars");
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void topCapabilitySimulatesAndRefusesExtractionWhilePhysicalIntakeBurnsJunk(GameTestHelper h){
        var furnace=formed(h,Direction.SOUTH);var level=h.getLevel();var center=furnace.getBlockPos();
        var top=level.getCapability(Capabilities.ItemHandler.BLOCK,center,Direction.UP);
        h.assertTrue(top!=null&&top.getSlots()==32,"Top intake must expose 32 slots");
        for(var side:Direction.values())if(side!=Direction.UP){var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,center,side);h.assertTrue(handler==null||handler.getSlots()==0,"Furnace exposed intake on "+side);}
        h.assertTrue(ItemHandlerHelper.insertItemStacked(top,new ItemStack(Items.BEEF,64),true).isEmpty(),"Simulation reported a false obstruction");
        h.assertTrue(furnace.isEmpty(),"Simulated insertion mutated furnace contents");
        h.assertTrue(ItemHandlerHelper.insertItemStacked(top,new ItemStack(Items.BEEF,64),false).isEmpty(),"Actual insertion failed");
        h.assertTrue(top.extractItem(0,64,false).isEmpty(),"Automation extracted unfinished inputs");
        for(int slot=1;slot<32;slot++)furnace.setItem(slot,new ItemStack(Items.BEEF,64));
        h.assertValueEqual(furnace.accept(new ItemStack(Items.BEEF,7)).getCount(),7,"Full furnace destroyed a smeltable remainder");
        h.assertTrue(furnace.accept(new ItemStack(Items.DIRT,9)).isEmpty(),"Physical unsmeltable input was not burned");
        h.assertValueEqual(furnace.getItem(0).getCount(),64,"Burning junk modified queued ingredients");
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=150)
    public static void droppedItemsActuallyFallThroughTheOpeningAndFinishASmelt(GameTestHelper h){
        var furnace=formed(h,Direction.SOUTH);var level=h.getLevel();var center=furnace.getBlockPos();
        var item=new ItemEntity(level,center.getX()+.5,center.getY()+2.3,center.getZ()+.5,new ItemStack(Items.BEEF,2),0,0,0);
        item.setPickUpDelay(32767);level.addFreshEntity(item);
        h.runAfterDelay(30,()->{
            h.assertFalse(item.isAlive(),"Falling input never reached the furnace's collision intake");
            h.assertValueEqual(furnace.getItem(0).getCount(),2,"Physical intake changed the submitted stack count");
        });
        h.runAfterDelay(130,()->{
            h.assertValueEqual(furnace.getItem(0).getCount(),1,"Real ticking furnace did not consume exactly one first-cycle ingredient");
            h.assertValueEqual(drops(h,Items.COOKED_BEEF),1,"Real ticking furnace did not eject the cooked output");
            h.succeed();
        });
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void cookReservePersistsAndARealSmeltInsertsOneOutputBeforeStartingTheNext(GameTestHelper h){
        var furnace=formed(h,Direction.SOUTH);var level=h.getLevel();var center=furnace.getBlockPos();
        level.setBlockAndUpdate(center.south(),Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)level.getBlockEntity(center.south());
        var savedAura=level.getChunkAt(center).getData(AuraSystem.AURA);
        try {
            level.getChunkAt(center).setData(AuraSystem.AURA,new AuraState(100,100,0));
            furnace.speedyTime=0;furnace.tick();
            h.assertValueEqual(furnace.speedyTime,20,"Idle furnace did not reserve twenty vis");
            h.assertValueEqual(AuraSystem.get(level,center).vis(),80f,"Idle reservation paid the wrong vis amount");
            furnace.setItem(0,new ItemStack(Items.BEEF,2));furnace.tick();
            h.assertValueEqual(furnace.furnaceCookTime,80,"Boosted cook duration differs from original");
            furnace.furnaceCookTime=1;
            var restored=new InfernalFurnaceEntity(center,furnace.getBlockState());
            restored.loadWithComponents(furnace.saveWithoutMetadata(level.registryAccess()),level.registryAccess());restored.setLevel(level);level.setBlockEntity(restored);furnace=restored;
            h.assertValueEqual(furnace.furnaceCookTime,1,"Reload lost cook progress");h.assertValueEqual(furnace.speedyTime,20,"Reload lost prepaid reserve");
            furnace.tick();
            h.assertValueEqual(furnace.getItem(0).getCount(),1,"Completion must consume one raw input");
            h.assertValueEqual(count(chest,Items.COOKED_BEEF),1,"Real smelting recipe did not insert its cooked result at the mouth");
            h.assertValueEqual(drops(h,Items.COOKED_BEEF),0,"Successful adjacent insertion also spawned a duplicate world drop");
            h.assertValueEqual(furnace.speedyTime,19,"Completion must consume one reserve unit");
            h.assertValueEqual(furnace.furnaceCookTime,0,"The next smelt started on the completion tick");
            h.assertValueEqual(AuraSystem.get(level,center).vis(),80f,"Completion charged another vis batch prematurely");
            furnace.tick();h.assertValueEqual(furnace.furnaceCookTime,80,"Next tick failed to schedule the remaining input");
            for(int i=0;i<80;i++)furnace.tick();
            h.assertTrue(furnace.getItem(0).isEmpty(),"Second cycle did not finish");h.assertValueEqual(count(chest,Items.COOKED_BEEF),2,"Second cycle lost or duplicated the cooked result");
        }finally{level.getChunkAt(center).setData(AuraSystem.AURA,savedAura);furnace.speedyTime=20;}
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void zeroAuraStillSmeltsAndBlockedInventoryEjectsItsRemainderAndExperience(GameTestHelper h){
        var furnace=formed(h,Direction.WEST);var level=h.getLevel();var center=furnace.getBlockPos();
        level.setBlockAndUpdate(center.west(),Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)level.getBlockEntity(center.west());
        for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        var savedAura=level.getChunkAt(center).getData(AuraSystem.AURA);
        try{
            level.getChunkAt(center).setData(AuraSystem.AURA,new AuraState(100,0,0));furnace.speedyTime=0;
            furnace.setItem(0,new ItemStack(Items.DIAMOND_ORE));furnace.tick();
            h.assertValueEqual(furnace.furnaceCookTime,140,"No-vis furnace must work without fuel at 140 ticks");
            for(int i=0;i<140;i++)furnace.tick();
            h.assertTrue(furnace.isEmpty(),"No-vis smelt stalled");h.assertValueEqual(drops(h,Items.DIAMOND),1,"Full mouth inventory swallowed the output");
            var drops=level.getEntitiesOfClass(ItemEntity.class,bounds(h),e->e.getItem().is(Items.DIAMOND));var drop=drops.getFirst();
            h.assertTrue(Math.abs(drop.getX()-(center.getX()-.5))<1e-6&&Math.abs(drop.getY()-center.getY())<1e-6,"World output spawned at the wrong mouth coordinates");
            h.assertTrue(Math.abs(drop.getDeltaMovement().x+.3)<1e-6,"Output did not move outward");
            h.assertValueEqual(level.getEntitiesOfClass(ExperienceOrb.class,bounds(h)).stream().mapToInt(ExperienceOrb::getValue).sum(),1,"Source furnace XP was lost or multiplied");
            h.assertValueEqual(count(chest,Items.COBBLESTONE),27*64,"Overflow modified existing destination items");
        }finally{level.getChunkAt(center).setData(AuraSystem.AURA,savedAura);furnace.speedyTime=20;}
        h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void bellowsUseSourceDistanceFacingPowerCapAndDiminishingDiscount(GameTestHelper h){
        var furnace=formed(h,Direction.SOUTH);var level=h.getLevel();var center=furnace.getBlockPos();
        int n=0;int[] normal={140,120,102,86,72},boosted={80,60,42,26,12};
        for(var side:Direction.Plane.HORIZONTAL){
            var pos=center.relative(side,2);level.setBlockAndUpdate(pos,EssentiaContent.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,side.getOpposite()));n++;
            h.assertValueEqual(furnace.bellows(),n,"Correct bellows at two-block range were not counted");
            furnace.speedyTime=0;h.assertValueEqual(furnace.calcCookTime(),normal[n],"Original normal-speed diminishing discount changed");
            furnace.speedyTime=20;h.assertValueEqual(furnace.calcCookTime(),boosted[n],"Original boosted diminishing discount changed");
        }
        level.setBlockAndUpdate(center.below(2),EssentiaContent.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.UP));
        h.assertValueEqual(furnace.bellows(),4,"A fifth bellows exceeded the original cap");
        level.removeBlock(center.below(2),false);
        level.setBlockAndUpdate(center.above(2),EssentiaContent.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.DOWN));
        level.setBlockAndUpdate(center.north(2).above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertValueEqual(furnace.bellows(),3,"Powered bellows or top bellows incorrectly contributed");
        level.setBlockAndUpdate(center.east(2),EssentiaContent.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.EAST));
        h.assertValueEqual(furnace.bellows(),2,"Outward-facing bellows contributed");h.succeed();
    }
    @GameTest(template="infernal_furnace_empty",timeoutTicks=30)
    public static void bonusTableRollsEachMatchingRewardEachPassAndCopperNuggetsHaveAUsableRecipe(GameTestHelper h){
        var level=h.getLevel();
        for(String metal:List.of("iron","gold","cinnabar","copper","tin","silver","lead","quartz")){
            var cluster=stack("cluster_"+metal);h.assertFalse(cluster.isEmpty(),"Missing source cluster "+metal);
            var bonus=InfernalFurnaceBonuses.roll(cluster,4,()->0);
            h.assertValueEqual(bonus.size(),10,"Five passes must independently yield both cluster nugget and rare earth");
            h.assertValueEqual(bonus.stream().filter(s->s.is(stack("rareearth").getItem())).count(),5L,"Cluster rare-earth reward is not per pass");
            h.assertTrue(bonus.stream().noneMatch(ItemStack::isEmpty),"Bonus table contains an unregistered placeholder output");
            h.assertTrue(InfernalFurnaceBonuses.roll(cluster,4,()->1).isEmpty(),"Failed bonus rolls still awarded items");
        }
        h.assertValueEqual(InfernalFurnaceBonuses.roll(new ItemStack(Items.IRON_ORE),0,()->.02).size(),1,"Iron's .01 rare-earth chance became its .33 nugget chance");
        h.assertValueEqual(InfernalFurnaceBonuses.roll(new ItemStack(Items.DIAMOND_ORE),0,()->.02).size(),1,"Diamond's .025 rare-earth reward was omitted");
        h.assertValueEqual(InfernalFurnaceBonuses.roll(new ItemStack(Items.BEEF),4,()->0).size(),5,"Meat bonuses must gain one independent roll per bellows");
        h.assertTrue(InfernalFurnaceBonuses.roll(new ItemStack(Items.RAW_IRON),4,()->0).isEmpty(),"Modern raw iron silently received a new source bonus");
        var ingredient=new ItemStack(InfernalFurnaceContent.NUGGETS.get("copper").get());
        var input=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->ingredient.copy()).toList());
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        h.assertTrue(recipe.isPresent()&&recipe.get().value().assemble(input,level.registryAccess()).is(Items.COPPER_INGOT),"Copper bonus nuggets cannot actually be recombined");
        h.succeed();
    }
    private static InfernalFurnaceEntity formed(GameTestHelper h,Direction mouth){blueprint(h,mouth);h.assertTrue(InfernalFurnaceStructure.form(h.getLevel(),h.absolutePos(CENTER),mouth),"Fixture blueprint failed to form");var furnace=(InfernalFurnaceEntity)h.getBlockEntity(CENTER);furnace.speedyTime=20;return furnace;}
    private static void blueprint(GameTestHelper h,Direction mouth){
        var level=h.getLevel();var center=h.absolutePos(CENTER);
        for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            Block block=x==0&&z==0&&y==1?Blocks.AIR:x==0&&z==0&&y==0?Blocks.LAVA
                    :y==0&&x==mouth.getStepX()&&z==mouth.getStepZ()?Blocks.IRON_BARS:x!=0&&z!=0?Blocks.NETHER_BRICKS:Blocks.OBSIDIAN;
            level.setBlock(center.offset(x,y,z),block.defaultBlockState(),Block.UPDATE_CLIENTS);
        }
    }
    private static int shellCount(GameTestHelper h){int count=0;var center=h.absolutePos(CENTER);for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))if(h.getLevel().getBlockState(pos).getBlock() instanceof FurnaceShellBlock)count++;return count;}
    private static AABB bounds(GameTestHelper h){return new AABB(h.absolutePos(CENTER)).inflate(3);}
    private static int drops(GameTestHelper h,Item item){return h.getLevel().getEntitiesOfClass(ItemEntity.class,bounds(h),e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static int count(net.minecraft.world.Container container,Item item){int total=0;for(int i=0;i<container.getContainerSize();i++)if(container.getItem(i).is(item))total+=container.getItem(i).getCount();return total;}
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)));}
    private InfernalFurnaceGameTests(){}
}
