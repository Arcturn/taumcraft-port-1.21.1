package thaumcraft.port.gametest;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.*;
import thaumcraft.port.research.ResearchStorage;

/** Real-world custody, persistent settings and hostile menu-packet regressions. */
@GameTestHolder("thaumcraft")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class GolemSealCoreGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void sealSettingsAndComponentFiltersRoundTrip(GameTestHelper h){
        for(var face:Direction.values()){
            var original=new SealEntity(h.getLevel(),new SealPos(h.absolutePos(new BlockPos(2,2,2)),face),new SealPickupAdvanced());
            var expected=new BlockPos(face.getStepX()==0?3:1,face.getStepY()==0?3:1,face.getStepZ()==0?3:1);
            h.assertValueEqual(original.getArea(),expected,"Default area differs for "+face);
            original.setOwner(UUID.randomUUID().toString());original.setLocked(true);original.setRedstoneSensitive(true);original.setColor((byte)16);original.setPriority((byte)-5);original.setArea(new BlockPos(8,2,7));
            var filter=(ISealConfigFilter)original.getSeal();filter.setBlacklist(false);var item=new ItemStack(Items.DIAMOND_PICKAXE);item.setDamageValue(17);item.set(DataComponents.CUSTOM_NAME,Component.literal("Reserved tool"));filter.setFilterSlot(8,item);filter.setFilterSlotSize(8,145);
            ((ISealConfigToggles)original.getSeal()).setToggle(3,true);
            var restored=new SealEntity();restored.readNBT(original.writeNBT(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertValueEqual(restored.getSealPos(),original.getSealPos(),"Face attachment lost");h.assertValueEqual(restored.getOwner(),original.getOwner(),"Owner lost");h.assertTrue(restored.isLocked()&&restored.isRedstoneSensitive(),"Lock/redstone lost");h.assertValueEqual(restored.getColor(),(byte)16,"Color lost");h.assertValueEqual(restored.getPriority(),(byte)-5,"Priority lost");h.assertValueEqual(restored.getArea(),original.getArea(),"Area lost");
            var recovered=(ISealConfigFilter)restored.getSeal();h.assertTrue(ItemStack.isSameItemSameComponents(item,recovered.getFilterSlot(8)),"Filter item components lost");h.assertValueEqual(recovered.getFilterSlotSize(8),145,"Filter count lost");h.assertFalse(recovered.isBlacklist(),"Whitelist lost");h.assertTrue(((ISealConfigToggles)restored.getSeal()).getToggles()[3].getValue(),"Toggle lost");
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void ghostCountsNeverMoveOrAliasRealItems(GameTestHelper h){
        var filter=new SealFillAdvanced();filter.setBlacklist(false);var real=new ItemStack(Items.DIAMOND,12);real.set(DataComponents.CUSTOM_NAME,Component.literal("Mine"));
        SealMenu.editGhost(filter,0,real,0,false);h.assertValueEqual(real.getCount(),12,"Ghost creation consumed carried items");h.assertValueEqual(filter.getFilterSlot(0).getCount(),1,"Ghost stack must hold only a template");h.assertValueEqual(filter.getFilterSlotSize(0),0,"First insertion must mean unrestricted *");
        SealMenu.editGhost(filter,0,real,0,false);h.assertValueEqual(filter.getFilterSlotSize(0),12,"Left click must add held stack size");SealMenu.editGhost(filter,0,ItemStack.EMPTY,0,true);h.assertValueEqual(filter.getFilterSlotSize(0),22,"Shift left must add10");SealMenu.editGhost(filter,0,ItemStack.EMPTY,1,true);h.assertValueEqual(filter.getFilterSlotSize(0),12,"Shift right must remove10");
        SealMenu.editGhost(filter,0,real,1,false);h.assertValueEqual(filter.getFilterSlotSize(0),0,"Exact decrement must retain unrestricted *");h.assertFalse(filter.getFilterSlot(0).isEmpty(),"Zero count prematurely removed ghost");SealMenu.editGhost(filter,0,ItemStack.EMPTY,1,false);h.assertTrue(filter.getFilterSlot(0).isEmpty(),"Negative count must clear ghost");
        SealMenu.editGhost(filter,0,real,0,false);filter.setFilterSlotSize(0,Integer.MAX_VALUE);SealMenu.editGhost(filter,0,real,0,false);h.assertValueEqual(filter.getFilterSlotSize(0),Integer.MAX_VALUE,"Count overflowed");real.set(DataComponents.CUSTOM_NAME,Component.literal("Changed"));h.assertValueEqual(filter.getFilterSlot(0).getHoverName().getString(),"Mine","Ghost aliased carried components");filter.setBlacklist(true);SealMenu.editGhost(filter,0,ItemStack.EMPTY,1,false);h.assertTrue(filter.getFilterSlot(0).isEmpty(),"Blacklist right click did not clear");h.assertValueEqual(real.getCount(),12,"Ghost edits consumed carried items");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void placementDuplicateRemovalAndTaskCancellationConserveSeal(GameTestHelper h){
        var level=h.getLevel();var player=GameTestPlayers.create(h);var pos=h.absolutePos(new BlockPos(2,2,2));level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());var at=new SealPos(pos,Direction.UP);var stack=SealPlacerItem.getSealStack("thaumcraft:pickup");stack.setCount(2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(0,.5,0),Direction.UP,pos,false));
        try{
            ((SealPlacerItem)stack.getItem()).onItemUseFirst(stack,context);h.assertValueEqual(stack.getCount(),1,"Placement must spend exactly one seal");h.assertTrue(SealHandler.getSealEntity(level,at)!=null,"Placement missing");((SealPlacerItem)stack.getItem()).onItemUseFirst(stack,context);h.assertValueEqual(stack.getCount(),1,"Duplicate placement spent an item");
            var task=new Task(at,pos.above());TaskHandler.addTask(level,task);SealHandler.removeSealEntity(level,at,false);h.assertTrue(task.isSuspended(),"Removed seal left task runnable");h.assertTrue(SealHandler.getSealEntity(level,at)==null,"Removed seal persisted");int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e->e.getItem().is(stack.getItem())).mapToInt(e->e.getItem().getCount()).sum();h.assertValueEqual(drops,1,"Removal must refund exactly one seal");SealHandler.removeSealEntity(level,at,false);h.assertValueEqual(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e->e.getItem().is(stack.getItem())).mapToInt(e->e.getItem().getCount()).sum(),1,"Duplicate removal duplicated refund");TaskHandler.getTasks(level).remove(task.getId());
        }finally{SealHandler.removeSealEntity(level,at,true);}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void ownerControlsRangeAndGhostQuickMoveStayAuthoritative(GameTestHelper h){
        var level=h.getLevel();var owner=GameTestPlayers.create(h);var guest=GameTestPlayers.create(h);var pos=h.absolutePos(new BlockPos(2,1,2));level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());var seal=new SealEntity(level,new SealPos(pos,Direction.UP),new SealFillAdvanced());seal.setOwner(owner.getUUID().toString());SealHandler.addSealEntity(level,seal);
        try{
            var foreign=new SealMenu(21,guest.getInventory(),seal);h.assertTrue(foreign.stillValid(guest),"Public seal configuration should remain available");foreign.category=0;h.assertFalse(foreign.clickMenuButton(guest,25),"Guest locked another player's seal");h.assertFalse(foreign.clickMenuButton(guest,27),"Guest changed owner's redstone control");h.assertTrue(foreign.clickMenuButton(guest,81),"Public priority adjustment missing");
            var menu=new SealMenu(22,owner.getInventory(),seal);menu.category=0;h.assertTrue(menu.clickMenuButton(owner,25),"Owner cannot lock");menu.category=1;menu.setCarried(new ItemStack(Items.DIAMOND,7));menu.clicked(0,0,ClickType.PICKUP,owner);h.assertValueEqual(menu.getCarried().getCount(),7,"Ghost click changed cursor custody");h.assertTrue(menu.quickMoveStack(owner,0).isEmpty(),"Ghost became real inventory through quick move");menu.category=0;var filter=(ISealConfigFilter)seal.getSeal();menu.clicked(0,1,ClickType.PICKUP,owner);h.assertFalse(filter.getFilterSlot(0).isEmpty(),"Hidden ghost slot accepted forged click");owner.setPos(pos.getX()+20,pos.getY(),pos.getZ());h.assertFalse(menu.clickMenuButton(owner,26),"Distant packet unlocked seal");h.assertTrue(seal.isLocked(),"Distance guard changed lock");
        }finally{SealHandler.removeSealEntity(level,seal.getSealPos(),true);}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void redstoneCancelsOnlyAttachedSealsTasks(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());var seal=new SealEntity(level,new SealPos(pos,Direction.UP),new SealPickup());seal.setRedstoneSensitive(true);var attached=new Task(seal.getSealPos(),pos.above());var other=new Task(new SealPos(pos.west(),Direction.UP),pos.above());TaskHandler.addTask(level,attached);TaskHandler.addTask(level,other);
        try{h.assertTrue(seal.isStoppedByRedstone(level),"Adjacent redstone did not stop seal");seal.tickSealEntity(level);h.assertTrue(attached.isSuspended(),"Stopped seal task remains runnable");h.assertFalse(other.isSuspended(),"Stopped seal suspended another seal's task");seal.setRedstoneSensitive(false);h.assertFalse(seal.isStoppedByRedstone(level),"Insensitive seal stopped by redstone");}finally{TaskHandler.getTasks(level).remove(attached.getId());TaskHandler.getTasks(level).remove(other.getId());}h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void boundaryRedstonePreservesDirectAndConductedPowerWithoutLoadingNeighbours(GameTestHelper h){
        var level=h.getLevel();var pos=new BlockPos(3_000_015,180,3_000_008);var outside=pos.east();
        // Load exactly the source chunk, then write directly to avoid fixture neighbour updates.
        var chunk=level.getChunkAt(pos);
        h.assertFalse(level.hasChunkAt(outside),"Fixture neighbour chunk is unexpectedly loaded");
        var old=chunk.getBlockState(pos);var oldWest=chunk.getBlockState(pos.west());var oldLever=chunk.getBlockState(pos.west(2));
        try{
            chunk.setBlockState(pos,Blocks.STONE.defaultBlockState(),false);
            var seal=new SealEntity(level,new SealPos(pos,Direction.UP),new SealPickup());seal.setRedstoneSensitive(true);
            h.assertFalse(seal.isStoppedByRedstone(level),"Absent neighbour stopped an unpowered seal");
            h.assertFalse(level.hasChunkAt(outside),"Unpowered seal loaded its neighbour chunk");
            chunk.setBlockState(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),false);
            h.assertTrue(seal.isStoppedByRedstone(level),"Loaded direct power was ignored at a chunk boundary");
            chunk.setBlockState(pos.west(),Blocks.STONE.defaultBlockState(),false);
            var lever=Blocks.LEVER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACING,Direction.WEST)
                    .setValue(net.minecraft.world.level.block.LeverBlock.POWERED,true);
            chunk.setBlockState(pos.west(2),lever,false);
            h.assertTrue(seal.isStoppedByRedstone(level),"Strong power through a loaded conductor was ignored");
            h.assertFalse(level.hasChunkAt(outside),"Conducted redstone loaded its neighbour chunk");
            seal.setRedstoneSensitive(false);h.assertFalse(seal.isStoppedByRedstone(level),"Insensitive seal stopped at a chunk boundary");
        }finally{
            chunk.setBlockState(pos,old,false);chunk.setBlockState(pos.west(),oldWest,false);chunk.setBlockState(pos.west(2),oldLever,false);
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void deliveryCanInspectAnUnloadedSourceSealWithoutLoadingIt(GameTestHelper h){
        var level=h.getLevel();var source=new BlockPos(3_001_015,180,3_001_008);
        h.assertFalse(level.hasChunkAt(source),"Fixture source chunk is unexpectedly loaded");
        var seal=new SealEntity(level,new SealPos(source,Direction.EAST),new SealProvide());seal.setRedstoneSensitive(true);
        // A provider delivery can outlive its source chunk; task validation still queries this seal.
        h.assertFalse(seal.isStoppedByRedstone(level),"Unavailable source invented a redstone signal");
        for(var direction:Direction.values())h.assertFalse(level.hasChunkAt(source.relative(direction)),"Source signal query loaded a chunk");
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void logisticsCatalogPreservesComponentsAndRejectsStaleSelections(GameTestHelper h){
        var level=h.getLevel();var player=GameTestPlayers.create(h);ResearchStorage.get(level.getServer()).player(player.getUUID()).flag("GOLEMLOGISTICS");var pos=h.absolutePos(new BlockPos(2,1,2));level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.Container)level.getBlockEntity(pos);var named=new ItemStack(Items.DIAMOND,32);named.set(DataComponents.CUSTOM_NAME,Component.literal("Named"));chest.setItem(0,named);chest.setItem(1,named.copyWithCount(40));chest.setItem(2,new ItemStack(Items.DIAMOND,11));var seal=new SealEntity(level,new SealPos(pos,Direction.UP),new SealProvide());seal.setOwner(player.getUUID().toString());SealHandler.addSealEntity(level,seal);
        try{
            var menu=new LogisticsMenu(23,player.getInventory(),(BlockPos)null);h.assertValueEqual(menu.page.size(),2,"Named and plain stacks merged");int index=-1;for(int i=0;i<menu.page.size();i++)if(menu.page.get(i).stack().has(DataComponents.CUSTOM_NAME))index=i;h.assertTrue(index>=0,"Named catalogue entry missing");h.assertValueEqual(menu.page.get(index).count(),72,"Same-component totals not aggregated");int revision=menu.revision;chest.setItem(0,named.copyWithCount(31));menu.refresh();h.assertTrue(menu.revision!=revision,"Changed catalogue revision missing");menu.action(player,new SealPayloads.LogisticsAction(23,index,70,0,"",revision));h.assertFalse(GolemHelper.getProvisionRequests(level).stream().anyMatch(r->r.getEntity()==player),"Stale slot requested wrong item");menu.action(player,new SealPayloads.LogisticsAction(23,index,70,0,"",menu.revision));var requests=GolemHelper.getProvisionRequests(level).stream().filter(r->r.getEntity()==player).toList();h.assertValueEqual(requests.size(),2,"70 items must split into64+6 requests");h.assertValueEqual(requests.stream().mapToInt(r->r.getStack().getCount()).sum(),70,"Provision amount lost");h.assertTrue(requests.stream().allMatch(r->ItemStack.isSameItemSameComponents(r.getStack(),named)),"Provision request lost components");h.assertValueEqual(chest.getItem(0).getCount()+chest.getItem(1).getCount(),71,"Catalogue granted or consumed real items");
        }finally{SealHandler.removeSealEntity(level,seal.getSealPos(),true);GolemHelper.getProvisionRequests(level).removeIf(r->r.getEntity()==player);}h.succeed();
    }
    private GolemSealCoreGameTests(){}
}
