package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemLogisticsGameTests {
    private static final BlockPos P=new BlockPos(3,2,3);
    private static GolemEntity golem(GameTestHelper h){
        var golem=GolemContent.GOLEM.get().create(h.getLevel());
        var pos=h.absolutePos(P);golem.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);return golem;
    }
    private static SealEntity seal(GameTestHelper h,BlockPos pos,ISeal behavior){return new SealEntity(h.getLevel(),new SealPos(h.absolutePos(pos),Direction.UP),behavior);}
    private static BarrelBlockEntity barrel(GameTestHelper h,BlockPos pos){h.setBlock(pos,Blocks.BARREL);return (BarrelBlockEntity)h.getBlockEntity(pos);}
    private static Task task(GameTestHelper h,SealEntity seal){
        return TaskHandler.getTasks(h.getLevel()).values().stream().filter(t->seal.getSealPos().equals(t.getSealPos())&&!t.isSuspended()).findFirst().orElseThrow(()->new AssertionError("Seal did not issue a task"));
    }
    private static int carried(GolemEntity golem,ItemStack match){return golem.getCarrying().stream().filter(s->ItemStack.isSameItemSameComponents(s,match)).mapToInt(ItemStack::getCount).sum();}

    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void permissiveExtractionPreservesTheActualStackComponents(GameTestHelper h){
        var inventory=new ItemStackHandler(3);var first=new ItemStack(Items.IRON_INGOT,3);first.set(DataComponents.CUSTOM_NAME,Component.literal("First"));
        var second=new ItemStack(Items.IRON_INGOT,4);second.set(DataComponents.CUSTOM_NAME,Component.literal("Second"));
        inventory.setStackInSlot(0,first);inventory.setStackInSlot(1,second);inventory.setStackInSlot(2,first.copyWithCount(2));
        var filter=new SealInventory.Filter(false,true,false,false);
        var simulated=SealInventory.extract(inventory,new ItemStack(Items.IRON_INGOT,8),filter,true);
        h.assertValueEqual(simulated.getCount(),5,"Permissive extraction mixed unlike components");
        h.assertValueEqual(inventory.getStackInSlot(0).getCount(),3,"Simulation mutated source");
        var extracted=SealInventory.extract(inventory,new ItemStack(Items.IRON_INGOT,8),filter,false);
        h.assertTrue(ItemStack.isSameItemSameComponents(extracted,first),"Filter template replaced actual item data");
        h.assertValueEqual(extracted.getCount(),5,"Equivalent actual stacks were not merged");
        h.assertValueEqual(inventory.getStackInSlot(1).getCount(),4,"Different named stack was consumed");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void ghostFiltersDistinguishComponentsDamageAndBlacklist(GameTestHelper h){
        var original=new ItemStack(Items.IRON_PICKAXE);var damaged=original.copy();damaged.setDamageValue(7);
        h.assertFalse(SealInventory.matches(original,damaged,SealInventory.STRICT),"Strict filter ignored durability");
        h.assertTrue(SealInventory.matches(original,damaged,new SealInventory.Filter(true,false,false,false)),"Ignore-damage still compared damage component");
        var named=original.copy();named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
        h.assertFalse(SealInventory.matches(original,named,SealInventory.STRICT),"Strict filter ignored custom name");
        h.assertTrue(SealInventory.matches(original,named,new SealInventory.Filter(false,true,false,false)),"Ignore-components filter did not match");
        h.assertTrue(SealInventory.matches(new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.BIRCH_PLANKS),new SealInventory.Filter(false,false,true,false)),"Legacy plankWood equivalence was lost with vanilla material tags");
        h.assertTrue(SealInventory.matchesFilters(List.of(ItemStack.EMPTY),true,named,SealInventory.STRICT),"Empty blacklist must allow items");
        h.assertFalse(SealInventory.matchesFilters(List.of(ItemStack.EMPTY),false,named,SealInventory.STRICT),"Empty whitelist must reject items");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void basicFillHonorsExactStockLimitAndReturnsSurplus(GameTestHelper h){
        var barrel=barrel(h,P);barrel.setItem(0,new ItemStack(Items.APPLE,7));var behavior=new SealFill();behavior.setBlacklist(false);behavior.setFilterSlot(0,new ItemStack(Items.APPLE));behavior.setFilterSlotSize(0,10);
        var seal=seal(h,P,behavior);var ticket=new Task(seal.getSealPos(),seal.getSealPos().pos);var worker=golem(h);worker.holdItem(new ItemStack(Items.APPLE,12));
        h.assertTrue(behavior.canGolemPerformTask(worker,ticket),"Fill did not offer missing stock");behavior.onTaskCompletion(h.getLevel(),worker,ticket);
        h.assertValueEqual(barrel.getItem(0).getCount(),10,"Fill exceeded exact target");h.assertValueEqual(carried(worker,new ItemStack(Items.APPLE)),9,"Undelivered carried items lost");
        h.assertFalse(behavior.canGolemPerformTask(worker,new Task(seal.getSealPos(),seal.getSealPos().pos)),"Satisfied limit offered another delivery");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fillFullDestinationAndExistingOnlyDoNotSpendCargo(GameTestHelper h){
        var barrel=barrel(h,P);for(int i=0;i<barrel.getContainerSize();i++)barrel.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        var behavior=new SealFillAdvanced();var seal=seal(h,P,behavior);var ticket=new Task(seal.getSealPos(),seal.getSealPos().pos);var worker=golem(h);worker.holdItem(new ItemStack(Items.APPLE,12));
        h.assertFalse(behavior.canGolemPerformTask(worker,ticket),"Full inventory accepted delivery");behavior.onTaskCompletion(h.getLevel(),worker,ticket);
        h.assertValueEqual(carried(worker,new ItemStack(Items.APPLE)),12,"Rejected delivery lost cargo");
        barrel.clearContent();behavior.setToggle(4,true);
        h.assertFalse(behavior.canGolemPerformTask(worker,ticket),"Existing-only inserted a new item type");
        barrel.setItem(0,new ItemStack(Items.APPLE));h.assertTrue(behavior.canGolemPerformTask(worker,ticket),"Existing-only rejected stocked item");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void floorFillCountsExistingDroppedStacksAgainstLimit(GameTestHelper h){
        h.setBlock(P,Blocks.STONE);var behavior=new SealFill();behavior.setBlacklist(false);behavior.setFilterSlot(0,new ItemStack(Items.DIAMOND));behavior.setFilterSlotSize(0,10);
        var seal=seal(h,P,behavior);var out=SealInventory.output(seal.getSealPos());var existing=new ItemEntity(h.getLevel(),out.x,out.y,out.z,new ItemStack(Items.DIAMOND,10));h.getLevel().addFreshEntity(existing);
        var worker=golem(h);worker.holdItem(new ItemStack(Items.DIAMOND,5));
        h.assertFalse(behavior.canGolemPerformTask(worker,new Task(seal.getSealPos(),seal.getSealPos().pos)),"Ground stack limit ignored real dropped stacks");existing.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void advancedEmptyLeavesOneAcrossSlotsAndCyclesWhitelist(GameTestHelper h){
        var barrel=barrel(h,P);barrel.setItem(0,new ItemStack(Items.APPLE,3));barrel.setItem(1,new ItemStack(Items.APPLE,2));barrel.setItem(2,new ItemStack(Items.CARROT,4));
        var behavior=new SealEmptyAdvanced();behavior.setBlacklist(false);behavior.setFilterSlot(0,new ItemStack(Items.APPLE));behavior.setFilterSlot(1,new ItemStack(Items.CARROT));behavior.setToggle(4,true);behavior.setToggle(5,true);
        var seal=seal(h,P,behavior);for(int i=0;i<20;i++)behavior.tickSeal(h.getLevel(),seal);var worker=golem(h);behavior.onTaskCompletion(h.getLevel(),worker,task(h,seal));
        int apples=SealInventory.count(SealInventory.handler(h.getLevel(),seal.getSealPos().pos,Direction.UP),new ItemStack(Items.APPLE),SealInventory.STRICT);
        h.assertTrue(apples>=1&&apples<5,"Leave-one removed every apple or extracted nothing");h.assertValueEqual(carried(worker,new ItemStack(Items.APPLE))+apples,5,"Empty did not conserve items");
        h.assertTrue(behavior.getInv(1).getFirst().is(Items.CARROT),"Cycling did not advance to next nonempty filter");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void itemPickupReservesGroundedEntitiesAndKeepsUncarriedRemainder(GameTestHelper h){
        h.setBlock(P,Blocks.STONE);var behavior=new SealPickup();var seal=seal(h,P,behavior);var pos=SealInventory.output(seal.getSealPos());
        var item=new ItemEntity(h.getLevel(),pos.x,pos.y,pos.z,new ItemStack(Items.APPLE,12));item.setOnGround(true);item.setNoPickUpDelay();item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);
        var worker=golem(h);worker.holdItem(new ItemStack(Items.APPLE,60));for(int i=0;i<5;i++)behavior.tickSeal(h.getLevel(),seal);
        var ticket=task(h,seal);h.assertTrue(behavior.canGolemPerformTask(worker,ticket),"Partial carrying room rejected pickup");behavior.onTaskCompletion(h.getLevel(),worker,ticket);
        h.assertValueEqual(carried(worker,new ItemStack(Items.APPLE)),64,"Pickup capacity incorrect");h.assertValueEqual(item.getItem().getCount(),8,"Uncarried entity remainder lost");item.discard();h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void stockRequestsOnlyMissingInsertableAmount(GameTestHelper h){
        var barrel=barrel(h,P);barrel.setItem(0,new ItemStack(Items.EMERALD,7));var behavior=new SealStock();behavior.setFilterSlot(0,new ItemStack(Items.EMERALD));behavior.setFilterSlotSize(0,13);
        var seal=seal(h,P,behavior);for(int i=0;i<20;i++)behavior.tickSeal(h.getLevel(),seal);
        var requests=GolemHelper.getProvisionRequests(h.getLevel()).stream().filter(r->seal.getSealPos().pos.equals(r.getPos())).toList();
        h.assertValueEqual(requests.size(),1,"Stock generated duplicate requests");h.assertValueEqual(requests.getFirst().getStack().getCount(),6,"Stock requested wrong shortage");
        GolemHelper.getProvisionRequests(h.getLevel()).removeAll(requests);h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void providerUsesPickupThenPhysicalDeliveryWithSingleAndLeaveOne(GameTestHelper h){
        var source=barrel(h,P);source.setItem(0,new ItemStack(Items.DIAMOND,10));var destination=barrel(h,P.east(4));
        var behavior=new SealProvide();behavior.setToggle(4,true);behavior.setToggle(5,true);var seal=seal(h,P,behavior);
        var target=h.absolutePos(P.east(4));Task pickup=null;
        for(int i=0;i<45&&pickup==null;i++){
            GolemHelper.requestProvisioning(h.getLevel(),target,Direction.UP,new ItemStack(Items.DIAMOND,4));behavior.tickSeal(h.getLevel(),seal);
            pickup=TaskHandler.getTasks(h.getLevel()).values().stream().filter(t->seal.getSealPos().equals(t.getSealPos())&&t.getLinkedProvision()!=null&&t.getData()==0&&!t.isSuspended()).findFirst().orElse(null);
        }
        h.assertTrue(pickup!=null,"Provider never reserved matching request");var request=pickup.getLinkedProvision();var worker=golem(h);
        h.assertTrue(behavior.canGolemPerformTask(worker,pickup),"Empty carrier could not collect provision");behavior.onTaskCompletion(h.getLevel(),worker,pickup);
        h.assertValueEqual(source.getItem(0).getCount(),9,"Single-item toggle removed wrong quantity");h.assertTrue(destination.isEmpty(),"Provider teleported items without a delivery task");h.assertValueEqual(carried(worker,new ItemStack(Items.DIAMOND)),1,"Pickup did not give item to carrier");
        var delivery=request.getLinkedTask();h.assertTrue(delivery!=pickup&&delivery.getData()==2&&delivery.getPos().equals(target),"Delivery ticket did not address destination");
        h.assertTrue(behavior.canGolemPerformTask(worker,delivery),"Loaded carrier could not deliver provision");behavior.onTaskCompletion(h.getLevel(),worker,delivery);
        h.assertValueEqual(destination.getItem(0).getCount(),1,"Delivery quantity incorrect");h.assertValueEqual(carried(worker,new ItemStack(Items.DIAMOND)),0,"Delivered cargo remained on golem");
        var pending=GolemHelper.getProvisionRequests(h.getLevel()).stream().filter(r->target.equals(r.getPos())&&!r.isInvalid()).toList();
        h.assertTrue(pending.stream().anyMatch(r->r.getStack().getCount()==3),"Partial delivery failed to request remaining quantity");
        GolemHelper.getProvisionRequests(h.getLevel()).removeIf(r->target.equals(r.getPos()));h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void cancelledFloorDeliveryRestoresAllCarriedItems(GameTestHelper h){
        h.setBlock(P,Blocks.STONE);var behavior=new SealFill();var seal=seal(h,P,behavior);var worker=golem(h);
        worker.holdItem(new ItemStack(Items.NETHER_STAR,3));var output=SealInventory.output(seal.getSealPos());
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=event->{
            if(event.getEntity() instanceof ItemEntity item&&item.getItem().is(Items.NETHER_STAR)&&item.position().distanceToSqr(output)<1)event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{behavior.onTaskCompletion(h.getLevel(),worker,new Task(seal.getSealPos(),seal.getSealPos().pos));
            h.assertValueEqual(carried(worker,new ItemStack(Items.NETHER_STAR)),3,"Cancelled item spawn consumed carried delivery");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.succeed();
    }
    private GolemLogisticsGameTests(){}
}
