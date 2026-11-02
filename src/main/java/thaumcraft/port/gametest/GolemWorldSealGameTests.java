package thaumcraft.port.gametest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemWorldSealGameTests {
    private static GolemEntity golem(GameTestHelper helper,BlockPos pos) {
        var golem=helper.spawn(GolemContent.GOLEM.get(),pos); golem.setValidSpawn(); golem.setNoAi(true);
        golem.setOwnerUUID(GameTestPlayers.create(helper).getUUID()); return golem;
    }
    private static SealEntity seal(GameTestHelper helper,ISeal behavior,BlockPos pos,Direction face) {
        var seal=new SealEntity(helper.getLevel(),new SealPos(helper.absolutePos(pos),face),behavior);
        seal.setArea(new BlockPos(1,1,1)); seal.setPriority((byte)3); return seal;
    }
    private static List<Task> tasks(GameTestHelper helper,SealEntity seal) {
        return TaskHandler.getTasks(helper.getLevel()).values().stream().filter(t->seal.getSealPos().equals(t.getSealPos())).toList();
    }
    private static Task generate(GameTestHelper helper,SealEntity seal,int ticks) {
        for (int i=0;i<ticks;i++) seal.getSeal().tickSeal(helper.getLevel(),seal);
        var tasks=tasks(helper,seal).stream().filter(t->!t.isSuspended()&&!t.isCompleted()).toList();
        helper.assertFalse(tasks.isEmpty(),"Seal failed to generate a physical golem task"); return tasks.getFirst();
    }
    private static void cleanup(GameTestHelper helper,SealEntity seal,GolemEntity golem) {
        TaskHandler.getTasks(helper.getLevel()).entrySet().removeIf(e->seal.getSealPos().equals(e.getValue().getSealPos()));
        golem.discard();
    }
    private static int drops(GameTestHelper helper,BlockPos pos,net.minecraft.world.item.Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(helper.absolutePos(pos)).inflate(2)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }

    @GameTest(template="elemental_tools_empty")
    public static void guardCreatesShortTasksAndAssignsCombatWithoutRemoteDamage(GameTestHelper helper) {
        var golem=golem(helper,new BlockPos(3,2,4));
        var behavior=new SealGuard(); var seal=seal(helper,behavior,new BlockPos(5,1,5),Direction.UP); seal.setArea(new BlockPos(3,3,3));
        var zombie=helper.spawn(EntityType.ZOMBIE,new BlockPos(5,2,5)); zombie.setNoAi(true);
        var cow=helper.spawn(EntityType.COW,new BlockPos(6,2,5)); cow.setNoAi(true);
        var task=generate(helper,seal,20);
        helper.assertTrue(tasks(helper,seal).stream().allMatch(t->t.getEntity()==zombie),"Basic guard selected a peaceful animal");
        helper.assertValueEqual(task.getLifespan(),10L,"Guard ticket lifetime differs from BETA26");
        helper.assertValueEqual(task.getPriority(),(byte)3,"Guard lost priority");
        float health=zombie.getHealth(); behavior.onTaskStarted(helper.getLevel(),golem,task);
        helper.assertTrue(golem.getTarget()==zombie && task.isSuspended(),"Guard did not hand its target to combat AI");
        helper.assertValueEqual(zombie.getHealth(),health,"Seal attacked remotely rather than assigning combat");
        cleanup(helper,seal,golem); zombie.discard(); cow.discard(); helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void butcherKeepsTwoAdultsAndIgnoresBabiesAndPets(GameTestHelper helper) {
        var golem=golem(helper,new BlockPos(3,2,4)); var behavior=new SealButcher();
        var seal=seal(helper,behavior,new BlockPos(5,1,5),Direction.UP); seal.setArea(new BlockPos(3,3,3));
        var first=helper.spawn(EntityType.COW,new BlockPos(5,2,5)); first.setNoAi(true);
        var second=helper.spawn(EntityType.COW,new BlockPos(6,2,5)); second.setNoAi(true);
        var baby=helper.spawn(EntityType.COW,new BlockPos(5,2,6)); baby.setBaby(true); baby.setNoAi(true);
        var wolf=helper.spawn(EntityType.WOLF,new BlockPos(6,2,6)); wolf.setTame(true,false); wolf.setNoAi(true);
        for(int i=0;i<201;i++)behavior.tickSeal(helper.getLevel(),seal);
        helper.assertTrue(tasks(helper,seal).isEmpty(),"Butcher counted a calf toward the two breeding adults");
        helper.assertFalse(SealButcher.validAnimal(wolf),"Butcher selected a tamed animal");
        var third=helper.spawn(EntityType.COW,new BlockPos(4,2,5)); third.setNoAi(true);
        var task=generate(helper,seal,200); helper.assertValueEqual(tasks(helper,seal).size(),1,"Butcher queued a whole-herd attack");
        helper.assertTrue(task.getEntity()!=baby,"Butcher targeted a calf");
        behavior.onTaskStarted(helper.getLevel(),golem,task);
        helper.assertTrue(golem.getTarget()==task.getEntity(),"Butcher did not assign the selected target");
        cleanup(helper,seal,golem); first.discard();second.discard();baby.discard();wolf.discard();third.discard();helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void breakerWaitsForTaskAndRespectsOwnerProtection(GameTestHelper helper) {
        BlockPos block=new BlockPos(5,2,5); helper.setBlock(block,Blocks.STONE);
        var golem=golem(helper,block.west()); var behavior=new SealBreaker(); var seal=seal(helper,behavior,block.below(),Direction.UP);
        var task=generate(helper,seal,1); helper.assertBlockPresent(Blocks.STONE,block);
        var observed=new AtomicInteger(); var absolute=helper.absolutePos(block);
        Consumer<BlockEvent.BreakEvent> cancel=event->{if(event.getPos().equals(absolute)){
            helper.assertTrue(event.getPlayer().getUUID().equals(golem.getOwnerUUID()),"Protection hook lost the golem owner");observed.incrementAndGet();event.setCanceled(true);
        }};
        NeoForge.EVENT_BUS.addListener(cancel);
        try { behavior.onTaskCompletion(helper.getLevel(),golem,task); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertValueEqual(observed.get(),1,"Breaker skipped the protection hook"); helper.assertBlockPresent(Blocks.STONE,block);
        helper.assertValueEqual(drops(helper,block,Items.COBBLESTONE),0,"Canceled mining generated drops");
        var retry=generate(helper,seal,1); behavior.onTaskCompletion(helper.getLevel(),golem,retry);
        helper.assertBlockNotPresent(Blocks.STONE,block); helper.assertValueEqual(drops(helper,block,Items.COBBLESTONE),1,"Innate breaker did not produce the source forced-harvest drop");
        helper.setBlock(block,Blocks.BEDROCK); helper.assertFalse(behavior.isValidBlock(helper.getLevel(),absolute),"Breaker accepted negative hardness");
        cleanup(helper,seal,golem);helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void advancedBreakerSilkProgressAndSourceWhitelistArePreserved(GameTestHelper helper) {
        BlockPos block=new BlockPos(5,2,5);helper.setBlock(block,Blocks.DIAMOND_ORE);
        var golem=golem(helper,block.west());var behavior=new SealBreakerAdvanced();var seal=seal(helper,behavior,block.below(),Direction.UP);
        behavior.setBlacklist(false);behavior.setFilterSlot(0,new ItemStack(Items.DIAMOND_ORE));behavior.setFilterSlot(1,new ItemStack(Items.STONE));
        helper.assertFalse(behavior.isValidBlock(helper.getLevel(),helper.absolutePos(block)),"Advanced whitelist lost the source all-filled-filters rule");
        behavior.setFilterSlot(1,ItemStack.EMPTY);behavior.setToggle(1,true);
        var task=generate(helper,seal,1);helper.assertFalse(behavior.onTaskCompletion(helper.getLevel(),golem,task),"Silk breaker ignored hardness progress");
        helper.assertBlockPresent(Blocks.DIAMOND_ORE,block);
        for(int i=0;i<8&&!task.isSuspended();i++)behavior.onTaskCompletion(helper.getLevel(),golem,task);
        helper.assertBlockNotPresent(Blocks.DIAMOND_ORE,block);helper.assertValueEqual(drops(helper,block,Items.DIAMOND_ORE),1,"Silk breaker produced the wrong loot");
        helper.assertValueEqual(drops(helper,block,Items.DIAMOND),0,"Silk breaker also produced normal loot");cleanup(helper,seal,golem);helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void lumberRemovesFurthestMatchingLogOneTaskActionAtATime(GameTestHelper helper) {
        BlockPos base=new BlockPos(5,2,5);helper.setBlock(base,Blocks.OAK_LOG);helper.setBlock(base.above(),Blocks.OAK_LOG);helper.setBlock(base.above(2),Blocks.OAK_LOG);helper.setBlock(base.east().above(2),Blocks.BIRCH_LOG);
        var golem=golem(helper,base.west());var behavior=new SealLumber();var seal=seal(helper,behavior,base.below(),Direction.UP);
        var task=generate(helper,seal,1);helper.assertFalse(behavior.onTaskCompletion(helper.getLevel(),golem,task),"Lumber discarded a live tree task");
        helper.assertBlockNotPresent(Blocks.OAK_LOG,base.above(2));helper.assertBlockPresent(Blocks.OAK_LOG,base);helper.assertBlockPresent(Blocks.OAK_LOG,base.above());helper.assertBlockPresent(Blocks.BIRCH_LOG,base.east().above(2));
        helper.assertValueEqual(drops(helper,base.above(),Items.OAK_LOG),1,"One lumber action removed more than one log");
        behavior.onTaskCompletion(helper.getLevel(),golem,task);behavior.onTaskCompletion(helper.getLevel(),golem,task);
        helper.assertBlockNotPresent(Blocks.OAK_LOG,base);cleanup(helper,seal,golem);helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void harvestReplantSurvivesSaveAndConsumesOneRealSeed(GameTestHelper helper) {
        BlockPos crop=new BlockPos(5,2,5);helper.setBlock(crop.below(),Blocks.FARMLAND);helper.setBlock(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var golem=golem(helper,crop.west());var behavior=new SealHarvest();var seal=seal(helper,behavior,crop.below(),Direction.UP);
        var task=generate(helper,seal,5);behavior.onTaskCompletion(helper.getLevel(),golem,task);helper.assertBlockNotPresent(Blocks.WHEAT,crop);
        var saved=new CompoundTag();behavior.writeCustomNBT(saved,helper.getLevel().registryAccess());
        helper.assertValueEqual(saved.getList("replant",10).size(),1,"Harvest did not persist replant intent");
        TaskHandler.getTasks(helper.getLevel()).entrySet().removeIf(e->seal.getSealPos().equals(e.getValue().getSealPos()));
        var restored=new SealHarvest();restored.readCustomNBT(saved,helper.getLevel().registryAccess());var loaded=seal(helper,restored,crop.below(),Direction.UP);
        var replant=generate(helper,loaded,5);helper.assertFalse(restored.canGolemPerformTask(golem,replant),"Replant accepted a worker without a seed");
        golem.holdItem(new ItemStack(Items.WHEAT_SEEDS,3));helper.assertTrue(restored.canGolemPerformTask(golem,replant),"Replant refused the saved seed type");
        restored.onTaskCompletion(helper.getLevel(),golem,replant);helper.assertBlockPresent(Blocks.WHEAT,crop);
        helper.assertValueEqual(helper.getBlockState(crop).getValue(CropBlock.AGE),0,"Replanted wheat was already mature");
        helper.assertValueEqual(golem.getCarrying().stream().filter(s->s.is(Items.WHEAT_SEEDS)).mapToInt(ItemStack::getCount).sum(),2,"Replant duplicated or overconsumed seeds");
        cleanup(helper,loaded,golem);helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void harvestLeavesImmatureCropsAndStackedBasesAlone(GameTestHelper helper) {
        BlockPos crop=new BlockPos(5,2,5);helper.setBlock(crop.below(),Blocks.FARMLAND);helper.setBlock(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,6));
        helper.assertFalse(SealHarvest.isGrownCrop(helper.getLevel(),helper.absolutePos(crop)),"Harvest selected immature wheat");
        helper.setBlock(crop,Blocks.CACTUS);helper.setBlock(crop.above(),Blocks.CACTUS);
        helper.assertFalse(SealHarvest.isGrownCrop(helper.getLevel(),helper.absolutePos(crop)),"Harvest selected the bottom cactus segment");
        helper.assertTrue(SealHarvest.isGrownCrop(helper.getLevel(),helper.absolutePos(crop.above())),"Harvest missed the upper cactus segment");helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void useEmptyHandKeepsCargoAndCanceledPlacementReturnsItsStack(GameTestHelper helper) {
        BlockPos lever=new BlockPos(5,2,5);helper.setBlock(lever.below(),Blocks.STONE);
        helper.setBlock(lever,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.FLOOR));
        var golem=golem(helper,lever.west());golem.holdItem(new ItemStack(Items.DIAMOND,4));var behavior=new SealUse();behavior.setToggle(6,true);
        var seal=seal(helper,behavior,lever,Direction.UP);var task=generate(helper,seal,5);
        behavior.onTaskCompletion(helper.getLevel(),golem,task);
        helper.assertTrue(helper.getBlockState(lever).getValue(LeverBlock.POWERED),"Empty-hand use did not activate the block");
        helper.assertValueEqual(golem.getCarrying().getFirst().getCount(),4,"Empty-hand use lost the carried diamonds");
        golem.dropItem(new ItemStack(Items.DIAMOND,4));golem.holdItem(new ItemStack(Items.STONE,8));behavior.setToggle(6,false);
        BlockPos target=lever.east(2);helper.setBlock(target,Blocks.DIRT);var absolute=helper.absolutePos(target);var protectedTask=new Task(new SealPos(absolute,Direction.UP),absolute);
        var observed=new AtomicInteger();Consumer<PlayerInteractEvent.RightClickBlock> cancel=event->{if(event.getPos().equals(absolute)){
            helper.assertTrue(event.getEntity().getUUID().equals(golem.getOwnerUUID()),"Use event lost the owner identity");observed.incrementAndGet();event.setCanceled(true);
        }};NeoForge.EVENT_BUS.addListener(cancel);
        try{behavior.onTaskCompletion(helper.getLevel(),golem,protectedTask);}finally{NeoForge.EVENT_BUS.unregister(cancel);}
        helper.assertValueEqual(observed.get(),1,"Use skipped the right-click hook");helper.assertBlockNotPresent(Blocks.STONE,target.above());
        helper.assertValueEqual(golem.getCarrying().getFirst().getCount(),8,"Canceled use lost or duplicated its stack");
        cleanup(helper,seal,golem);helper.succeed();
    }

    @GameTest(template="elemental_tools_empty")
    public static void rejectedReturnDropPersistsUntilItCanBeTransferred(GameTestHelper helper) {
        BlockPos target=new BlockPos(5,2,5);helper.setBlock(target,Blocks.STONE);var golem=golem(helper,target.west());
        while(golem.canCarry(new ItemStack(Items.STONE,64),false))golem.holdItem(new ItemStack(Items.STONE,64));
        var absolute=helper.absolutePos(target);
        Consumer<PlayerInteractEvent.RightClickBlock> remainder=event->{if(event.getPos().equals(absolute)){
            event.getEntity().getInventory().add(new ItemStack(Items.DIAMOND,3));event.setCanceled(true);
        }};
        Consumer<EntityJoinLevelEvent> cancel=event->{if(event.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND)&&item.distanceToSqr(golem)<16)event.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(remainder);NeoForge.EVENT_BUS.addListener(cancel);
        try {
            GolemInteractionHelper.golemClick(helper.getLevel(),golem,absolute,Direction.UP,ItemStack.EMPTY,false,true);
            helper.assertTrue(GolemInteractionHelper.hasPendingReturns(golem),"Canceled return drop lost ownership");
            GolemInteractionHelper.flushReturns(golem);
            helper.assertTrue(GolemInteractionHelper.hasPendingReturns(golem),"Retry erased a still-rejected return");
            helper.assertValueEqual(drops(helper,target.west(),Items.DIAMOND),0,"Rejected item was nonetheless spawned");
        } finally { NeoForge.EVENT_BUS.unregister(remainder);NeoForge.EVENT_BUS.unregister(cancel); }
        GolemInteractionHelper.flushReturns(golem);
        helper.assertFalse(GolemInteractionHelper.hasPendingReturns(golem),"Successful return left a duplicate pending stack");
        helper.assertValueEqual(drops(helper,target.west(),Items.DIAMOND),3,"Return retry duplicated or lost items");
        GolemInteractionHelper.flushReturns(golem);helper.assertValueEqual(drops(helper,target.west(),Items.DIAMOND),3,"Second retry duplicated the return");golem.discard();helper.succeed();
    }
    private GolemWorldSealGameTests() { }
}
