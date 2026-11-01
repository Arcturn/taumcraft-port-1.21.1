package thaumcraft.port.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.infusion.structure.*;
import thaumcraft.port.world.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionStructureGameTests {
    private static final BlockPos BASE=new BlockPos(10,2,10);
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void pedestalOneSlotAutomationSaveAndBreakingPreserveItsOffering(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);level.setBlock(pos,StructureContent.PEDESTAL_ARCANE.get().defaultBlockState(),3);
        var pedestal=(PedestalEntity)level.getBlockEntity(pos);
        helper.assertTrue(pedestal.getBlockState().triggerEvent(level,pos,5,0),"Pedestal block did not forward shield event to its entity");
        var top=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
        helper.assertTrue(top!=null,"Pedestal has no top inventory capability");
        var rest=top.insertItem(0,new ItemStack(Items.DIAMOND,3),false);
        helper.assertValueEqual(rest.getCount(),2,"Pedestal accepted more than one offering");
        helper.assertValueEqual(pedestal.getItem(0).getCount(),1,"Pedestal stored more than one offering");
        for(Direction face:Direction.values()) {
            var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,face);
            helper.assertTrue(handler!=null,"Pedestal missing automation on "+face);
            helper.assertValueEqual(handler.extractItem(0,1,true).getCount(),1,"Pedestal extraction simulation failed on "+face);
        }
        var restored=new PedestalEntity(pos,pedestal.getBlockState());restored.loadWithComponents(pedestal.saveWithoutMetadata(level.registryAccess()),level.registryAccess());
        helper.assertTrue(restored.getItem(0).is(Items.DIAMOND),"Offering lost across save/load");
        level.destroyBlock(pos,true);
        int dropped=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped,1,"Breaking pedestal lost or duplicated offering");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void handInteractionUsesOneItemAndReturnsTheOfferingAtPlayer(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);level.setBlock(pos,StructureContent.PEDESTAL_ANCIENT.get().defaultBlockState(),3);
        var player=GameTestPlayers.create(helper);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.EMERALD,3));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        var pedestal=(PedestalEntity)level.getBlockEntity(pos);
        helper.assertValueEqual(player.getMainHandItem().getCount(),2,"Insertion did not consume exactly one held item");
        helper.assertTrue(pedestal.getItem(0).is(Items.EMERALD),"Hand insertion failed");
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(pedestal.isEmpty(),"Second interaction did not return offering");
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(1)).stream().anyMatch(e->e.getItem().is(Items.EMERALD)),"Offering was not returned at the player's position");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void inlayCarriesStabilizerEnergyAndRemovalClearsTheWholePath(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);
        for(int x=0;x<5;x++)level.setBlock(pos.east(x).below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,StructureContent.STABILIZER.get().defaultBlockState(),3);
        level.setBlock(pos.east(),StructureContent.INLAY.get().defaultBlockState(),3);
        level.setBlock(pos.east(2),StructureContent.INLAY.get().defaultBlockState(),3);
        level.setBlock(pos.east(3),StructureContent.PEDESTAL_ELDRITCH.get().defaultBlockState(),3);
        var source=(StabilizerEntity)level.getBlockEntity(pos);setEnergy(helper,source,15);InlayNetwork.update(level,pos);
        helper.assertValueEqual(level.getBlockState(pos.east()).getValue(InlayBlock.CHARGE),15,"Adjacent source did not deliver full charge");
        helper.assertValueEqual(level.getBlockState(pos.east(2)).getValue(InlayBlock.CHARGE),14,"Inlay did not attenuate by one");
        var pedestal=(PedestalEntity)level.getBlockEntity(pos.east(3));
        helper.assertTrue(pos.equals(pedestal.findInstabilityMitigator()),"Pedestal could not trace increasing charge back to its stabilizer");
        helper.assertTrue(source.mitigate(5),"Five-energy mitigation failed");
        helper.assertValueEqual(level.getBlockState(pos.east(3)).getValue(InlayBlock.CHARGE),8,"Mitigation did not update network power");
        level.removeBlock(pos,false);
        helper.assertValueEqual(level.getBlockState(pos.east()).getValue(InlayBlock.CHARGE),0,"Removed source left first wire powered");
        helper.assertValueEqual(level.getBlockState(pos.east(3)).getValue(InlayBlock.CHARGE),0,"Removed source left pedestal powered");
        helper.assertTrue(pedestal.findInstabilityMitigator()==null,"Unpowered pedestal retained mitigation");
        level.setBlock(pos,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        helper.assertValueEqual(level.getBlockState(pos.east()).getValue(InlayBlock.CHARGE),0,"Vanilla redstone incorrectly powered magical inlay");
        level.removeBlock(pos.east().below(),false);
        helper.assertTrue(level.getBlockState(pos.east()).isAir(),"Unsupported inlay stayed in the air");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void stabilizerChargesAtOriginalFluxCostAndNeverExceedsFifteen(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);level.setBlock(pos,StructureContent.STABILIZER.get().defaultBlockState(),3);
        var entity=(StabilizerEntity)level.getBlockEntity(pos);var aura=AuraSystem.get(level,pos);float before=aura.flux();
        for(int i=0;i<19;i++)entity.tick();helper.assertValueEqual(entity.getEnergy(),0,"Stabilizer charged before twenty ticks");entity.tick();
        helper.assertValueEqual(entity.getEnergy(),1,"Stabilizer did not create one energy at twenty ticks");
        helper.assertTrue(Math.abs(aura.flux()-before-.25f)<.001,"Stabilizer did not produce .25 flux per energy");
        for(int i=0;i<400;i++)entity.tick();helper.assertValueEqual(entity.getEnergy(),15,"Stabilizer exceeded or failed to reach energy cap");
        helper.assertTrue(Math.abs(aura.flux()-before-3.75f)<.001,"Full stabilizer continued generating flux");
        var restored=new StabilizerEntity(pos,entity.getBlockState());restored.loadWithComponents(entity.saveWithoutMetadata(level.registryAccess()),level.registryAccess());
        helper.assertValueEqual(restored.getEnergy(),15,"Stabilizer energy was lost on reload");
        helper.assertFalse(entity.mitigate(16),"Insufficient energy allowed mitigation");helper.assertValueEqual(entity.getEnergy(),15,"Failed mitigation consumed charge");
        helper.assertTrue(entity.mitigate(5),"Valid mitigation failed");helper.assertValueEqual(entity.getEnergy(),10,"Mitigation used incorrect energy");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void stabilizerSpendsEnergyOnNearbyUnstableRiftsOnly(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);level.setBlock(pos,StructureContent.STABILIZER.get().defaultBlockState(),3);
        var entity=(StabilizerEntity)level.getBlockEntity(pos);setEnergy(helper,entity,5);
        var rift=WorldEntities.RIFT.get().create(level);rift.moveTo(pos.getX()+3.5,pos.getY()+1,pos.getZ()+.5);level.addFreshEntity(rift);
        for(int i=0;i<5;i++)entity.tick();helper.assertValueEqual(entity.getEnergy(),4,"Rift stabilization did not consume one energy");
        helper.assertTrue(Math.abs(rift.stability()-.125f)<.0001,"Rift gained incorrect original stability amount");
        rift.addStability(60);for(int i=0;i<10;i++)entity.tick();helper.assertValueEqual(entity.getEnergy(),4,"Very stable rift consumed extra energy");rift.discard();helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void rechargePedestalPaysAuraAndCapsPersistentCharge(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);level.setBlock(pos,StructureContent.RECHARGE_PEDESTAL.get().defaultBlockState(),3);
        var pedestal=(RechargePedestalEntity)level.getBlockEntity(pos);
        var boots=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:traveller_boots")));
        helper.assertTrue(boots.getItem() instanceof RechargeableItem,"Traveller boots do not share recharge API");
        var side=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.NORTH);
        helper.assertValueEqual(side.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount(),1,"Charging pedestal accepted non-rechargeable item");
        helper.assertTrue(side.insertItem(0,boots,false).isEmpty(),"Charging pedestal rejected rechargeable equipment");
        AuraSystem.drain(level,pos,Float.MAX_VALUE,false,false);AuraSystem.add(level,pos,12,false);var aura=AuraSystem.get(level,pos);
        pedestal.tick();helper.assertValueEqual(ItemCharge.get(pedestal.getItem(0)),5,"Recharge cycle did not transfer five charge");
        helper.assertTrue(Math.abs(aura.vis()-7)<.001,"Recharge cycle did not cost five aura vis");
        for(int i=0;i<9;i++)pedestal.tick();helper.assertValueEqual(ItemCharge.get(pedestal.getItem(0)),5,"Recharge ran before ten ticks");pedestal.tick();
        helper.assertValueEqual(ItemCharge.get(pedestal.getItem(0)),10,"Second cycle failed");
        int maximum=((RechargeableItem)boots.getItem()).maxCharge(boots,null);ItemCharge.add(pedestal.getItem(0),null,maximum);
        float before=aura.vis();for(int i=0;i<10;i++)pedestal.tick();helper.assertValueEqual(ItemCharge.get(pedestal.getItem(0)),maximum,"Charge exceeded item capacity");
        helper.assertValueEqual(aura.vis(),before,"Full item continued draining aura");
        var restored=new RechargePedestalEntity(pos,pedestal.getBlockState());restored.loadWithComponents(pedestal.saveWithoutMetadata(level.registryAccess()),level.registryAccess());
        helper.assertValueEqual(ItemCharge.get(restored.getItem(0)),maximum,"Charge component did not survive container save/load");
        ItemCharge.consume(pedestal.getItem(0),null,maximum);AuraSystem.drain(level,pos,Float.MAX_VALUE,false,false);
        for(int i=0;i<10;i++)pedestal.tick();helper.assertValueEqual(ItemCharge.get(pedestal.getItem(0)),0,"Dry aura created free equipment charge");
        helper.assertFalse(ItemCharge.consume(pedestal.getItem(0),null,1),"Empty equipment allowed charge consumption");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void pillarCollisionAndDropsMatchTwoOriginalStoneBlocks(GameTestHelper helper) {
        var level=helper.getLevel();BlockPos pos=helper.absolutePos(BASE);var state=StructureContent.PILLAR_ANCIENT.get().defaultBlockState();
        helper.assertTrue(Math.abs(state.getCollisionShape(level,pos).bounds().maxY-2)<.0001,"Pillar lost its two-block collision");
        helper.assertTrue(Math.abs(state.getShape(level,pos).bounds().maxY-.5)<.0001,"Pillar selection differs from original");
        level.setBlock(pos,state,3);level.destroyBlock(pos,true);
        int stones=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(StructureContent.STONE_ANCIENT.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
        helper.assertValueEqual(stones,2,"Dismantled pillar did not return exactly two source stones");helper.succeed();
    }
    @GameTest(template="condenser_empty",timeoutTicks=80)
    public static void originalStoneHarvestRequirementsDoNotLockHandHarvestableDevices(GameTestHelper helper) {
        var player=GameTestPlayers.create(helper);
        for(var block:java.util.List.of(StructureContent.STONE_ANCIENT.get(),StructureContent.STONE_ELDRITCH_TILE.get(),StructureContent.MATRIX_COST.get(),StructureContent.MATRIX_SPEED.get(),StructureContent.SLAB_ANCIENT.get(),StructureContent.INLAY.get())) {
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            helper.assertFalse(player.hasCorrectToolForDrops(block.defaultBlockState()),"Stone or inlay dropped without required pickaxe: "+block);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));
            helper.assertTrue(player.hasCorrectToolForDrops(block.defaultBlockState()),"Original minimum wooden pickaxe could not harvest "+block);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(var block:java.util.List.of(StructureContent.PEDESTAL_ARCANE.get(),StructureContent.STABILIZER.get(),StructureContent.RECHARGE_PEDESTAL.get(),StructureContent.PILLAR_ARCANE.get()))
            helper.assertTrue(player.hasCorrectToolForDrops(block.defaultBlockState()),"Original hand-harvestable device became tool-locked: "+block);
        helper.succeed();
    }
    private static void setEnergy(GameTestHelper helper,StabilizerEntity entity,int energy) {var tag=entity.saveWithoutMetadata(helper.getLevel().registryAccess());tag.putInt("Energy",energy);entity.loadWithComponents(tag,helper.getLevel().registryAccess());}
    private InfusionStructureGameTests() {}
}
