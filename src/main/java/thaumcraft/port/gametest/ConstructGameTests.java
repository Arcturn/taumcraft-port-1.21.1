package thaumcraft.port.gametest;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.construct.*;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ConstructGameTests {
    private static TurretEntity turret(GameTestHelper helper,boolean advanced,BlockPos position){var turret=helper.spawn(advanced?ConstructContent.ADVANCED.get():ConstructContent.BASIC.get(),position);turret.setValidSpawn();turret.setNoAi(true);return turret;}
    private static ArcaneBoreEntity bore(GameTestHelper helper,BlockPos position){var bore=helper.spawn(ConstructContent.BORE.get(),position);bore.setValidSpawn();bore.setNoAi(true);bore.setFacing(Direction.EAST);bore.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));bore.setCharge(10);return bore;}
    @GameTest(template="elemental_tools_empty")
    public static void sourceArrowShotConsumesOneMagazineRoundAndCannotFireEmpty(GameTestHelper helper){
        var turret=turret(helper,false,new BlockPos(4,2,4));var zombie=helper.spawn(EntityType.ZOMBIE,new BlockPos(8,2,4));zombie.setNoAi(true);turret.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TIPPED_ARROW,2));turret.getRandom().setSeed(21);turret.performRangedAttack(zombie,.5f);
        helper.assertValueEqual(turret.getMainHandItem().getCount(),1,"One turret shot consumed more than one arrow");var arrows=helper.getLevel().getEntitiesOfClass(Arrow.class,turret.getBoundingBox().inflate(3));helper.assertValueEqual(arrows.size(),1,"Shot did not spawn one real arrow");helper.assertTrue(arrows.getFirst().getOwner()==turret,"Turret projectile lost its owner");helper.assertTrue(arrows.getFirst().getBaseDamage()>2.5&&arrows.getFirst().getBaseDamage()<4,"Source range-dependent damage was not applied");
        turret.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);turret.performRangedAttack(zombie,1);helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(Arrow.class,turret.getBoundingBox().inflate(3)).size(),1,"Empty turret invented ammunition");turret.discard();zombie.discard();arrows.forEach(Arrow::discard);helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void dispenserFeedRequiresUpwardFacingAndMovesWholeArrowStack(GameTestHelper helper){
        BlockPos position=new BlockPos(4,2,4),dispenserPos=position.below();var turret=turret(helper,false,position);helper.setBlock(dispenserPos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.NORTH));var dispenser=(DispenserBlockEntity)helper.getBlockEntity(dispenserPos);dispenser.setItem(0,new ItemStack(Items.STONE,12));dispenser.setItem(1,new ItemStack(Items.ARROW,23));
        helper.assertFalse(turret.reloadFromDispenser(),"Sideways dispenser fed the turret");helper.setBlock(dispenserPos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.UP));
        helper.assertTrue(turret.reloadFromDispenser(),"Upward dispenser did not load arrows");helper.assertValueEqual(turret.getMainHandItem().getCount(),23,"Feed changed magazine count");helper.assertTrue(dispenser.getItem(1).isEmpty(),"Feed duplicated arrows in dispenser");helper.assertValueEqual(dispenser.getItem(0).getCount(),12,"Feed consumed a non-arrow item");helper.assertFalse(turret.reloadFromDispenser(),"Occupied magazine refilled again");turret.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void advancedTargetFlagsAndOwnerOnlyMenuPersist(GameTestHelper helper){
        var turret=turret(helper,true,new BlockPos(4,2,4));var owner=GameTestPlayers.create(helper);var stranger=GameTestPlayers.create(helper);owner.setPos(turret.position());stranger.setPos(turret.position());turret.setOwnerUUID(owner.getUUID());var menu=new ConstructMenu(4,owner.getInventory(),turret);
        helper.assertTrue(turret.targetFlag(1)&&!turret.targetFlag(0)&&!turret.targetFlag(2),"Advanced turret defaults differ from hostile-only source");helper.assertFalse(menu.clickMenuButton(stranger,1),"Another player changed targeting");helper.assertTrue(menu.clickMenuButton(owner,1),"Owner cannot toggle animals");helper.assertTrue(turret.targetFlag(0),"Animal toggle failed");
        var cow=helper.spawn(EntityType.COW,new BlockPos(6,2,4));cow.setNoAi(true);helper.assertTrue(turret.validTarget(cow,false),"Animal target toggle has no gameplay effect");var bat=helper.spawn(EntityType.BAT,new BlockPos(6,3,4));bat.setNoAi(true);helper.assertTrue(turret.validTarget(bat,false),"Source IAnimals selection lost ambient bats");bat.discard();turret.setTargetFlag(0,false);helper.assertFalse(turret.validTarget(cow,false),"Disabled animal targeting still selects cow");
        turret.setTargetFlag(2,true);turret.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ARROW,19));var tag=new CompoundTag();turret.addAdditionalSaveData(tag);var loaded=ConstructContent.ADVANCED.get().create(helper.getLevel());loaded.readAdditionalSaveData(tag);
        helper.assertTrue(owner.getUUID().equals(loaded.getOwnerUUID()),"Reload lost owner");helper.assertTrue(loaded.targetFlag(2),"Reload lost player setting");helper.assertValueEqual(loaded.getMainHandItem().getCount(),19,"Reload lost magazine");owner.setPos(owner.position().add(20,0,0));helper.assertFalse(menu.clickMenuButton(owner,2),"Distant configuration packet was accepted");turret.discard();cow.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void borePickAugmentsControlRadiusDepthSpeedAndFortune(GameTestHelper helper){
        var bore=bore(helper,new BlockPos(4,2,4));var pick=bore.getMainHandItem();helper.assertValueEqual(bore.getDigRadius(),4,"Iron enchantability must yield radius 14/3");helper.assertValueEqual(bore.getDigDepth(),32,"Base depth is eight times radius");
        InfusionAugments.setEnchantment(pick,"destructive",1);InfusionAugments.setEnchantment(pick,"burrowing",1);InfusionAugments.setEnchantment(pick,"sounding",3);InfusionAugments.setEnchantment(pick,"refining",2);
        helper.assertValueEqual(bore.getDigRadius(),6,"Destructive must add two radius");helper.assertValueEqual(bore.getDigDepth(),64,"Burrowing must add sixteen depth");helper.assertValueEqual(bore.getFortune(),3,"Sounding did not provide fortune");helper.assertValueEqual(bore.getRefining(),2,"Bore lost refining augmentation");helper.assertValueEqual(bore.getDigSpeed(Blocks.STONE.defaultBlockState()),3,"Iron mining speed differs from source half destroy speed");
        pick.setDamageValue(pick.getMaxDamage()-1);helper.assertFalse(bore.validInventory(),"Bore used last remaining tool durability");helper.assertFalse(bore.accepts(new ItemStack(Items.IRON_SWORD)),"Bore accepted a sword");bore.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void canceledAndUnbreakableMiningPreserveBlockToolChargeAndItems(GameTestHelper helper){
        var bore=bore(helper,new BlockPos(4,2,4));var relative=new BlockPos(8,2,4);var position=helper.absolutePos(relative);helper.setBlock(relative,Blocks.STONE);var loose=new ItemEntity(helper.getLevel(),position.getX()+.5,position.getY()+.5,position.getZ()+.5,new ItemStack(Items.DIAMOND,3));helper.getLevel().addFreshEntity(loose);
        Consumer<BlockEvent.BreakEvent> cancel=event->{if(event.getPos().equals(position))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(cancel);
        try{helper.assertFalse(bore.mineBlock(position),"Canceled protection hook was bypassed");helper.assertBlockPresent(Blocks.STONE,relative);helper.assertValueEqual(bore.getCharge(),10f,"Canceled mining consumed charge");helper.assertValueEqual(bore.getMainHandItem().getDamageValue(),0,"Canceled mining damaged tool");helper.assertFalse(loose.isRemoved(),"Canceled mining stole another player's item");}
        finally{NeoForge.EVENT_BUS.unregister(cancel);}
        helper.setBlock(relative,Blocks.BEDROCK);helper.assertFalse(bore.mineBlock(position),"Bore destroyed unbreakable block");helper.assertValueEqual(bore.getCharge(),10f,"Unbreakable block consumed charge");bore.discard();loose.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void successfulBoreMiningChargesOnceOutputsToChestAndWearsEveryFifty(GameTestHelper helper){
        var bore=bore(helper,new BlockPos(4,2,4));var chestRelative=new BlockPos(3,2,4);helper.setBlock(chestRelative,Blocks.CHEST);var chest=(ChestBlockEntity)helper.getBlockEntity(chestRelative);var block=new BlockPos(8,2,4);var absolute=helper.absolutePos(block);
        var loose=new ItemEntity(helper.getLevel(),absolute.getX()+.5,absolute.getY()+.5,absolute.getZ()+.5,new ItemStack(Items.DIAMOND,3));helper.getLevel().addFreshEntity(loose);
        bore.setCharge(16);
        for(int i=0;i<50;i++){helper.setBlock(block,Blocks.STONE);helper.assertTrue(bore.mineBlock(absolute),"Bore failed a valid protected harvest");helper.assertTrue(helper.getLevel().getBlockState(absolute).isAir(),"Harvest did not remove stone");helper.assertValueEqual(bore.getCharge(),16-(i+1)*.25f,"Successful mining must debit exactly one quarter vis per block");if(i==3)helper.assertValueEqual(bore.getCharge(),15f,"Four mined blocks must consume exactly one vis");if(i<49)helper.assertValueEqual(bore.getMainHandItem().getDamageValue(),0,"Tool wore before fifty blocks");}
        helper.assertValueEqual(bore.getMainHandItem().getDamageValue(),1,"Fifty blocks must cost one tool durability");int count=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.COBBLESTONE))count+=chest.getItem(i).getCount();helper.assertValueEqual(count,50,"Adjacent chest did not receive exact harvest drops");helper.assertFalse(loose.isRemoved(),"Bore collected a pre-existing player's item");helper.assertValueEqual(loose.getItem().getCount(),3,"Bore altered another drop");bore.discard();loose.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void boreAuraRechargeAndPersistentFractionalReserveMatchSource(GameTestHelper helper){
        var bore=bore(helper,new BlockPos(4,2,4));var aura=AuraSystem.get(helper.getLevel(),bore.blockPosition());aura.drain(false,Float.MAX_VALUE,false);aura.addVis(30);bore.setCharge(9.5f);bore.rechargeVis();helper.assertValueEqual(bore.getCharge(),19.5f,"Recharge incorrectly capped source's ten-vis batch");helper.assertValueEqual(aura.vis(),20f,"Recharge did not drain ten local vis");bore.rechargeVis();helper.assertValueEqual(aura.vis(),20f,"Already charged bore drained again");
        bore.setFacing(Direction.UP);bore.setActive(true);bore.getMainHandItem().setDamageValue(12);var tag=new CompoundTag();bore.addAdditionalSaveData(tag);var restored=ConstructContent.BORE.get().create(helper.getLevel());restored.readAdditionalSaveData(tag);helper.assertValueEqual(restored.getCharge(),19.5f,"Charge was not persisted as float");helper.assertTrue(restored.getFacing()==Direction.UP&&restored.isActive(),"Facing or active state lost on reload");helper.assertValueEqual(restored.getMainHandItem().getDamageValue(),12,"Bore tool state was lost");bore.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=60)
    public static void poweredBoreFindsAndMinesRealBlockThroughNormalTick(GameTestHelper helper){
        var bore=bore(helper,new BlockPos(4,2,4));helper.setBlock(new BlockPos(4,1,4),Blocks.STONE);helper.setBlock(new BlockPos(3,1,4),Blocks.REDSTONE_BLOCK);helper.setBlock(new BlockPos(8,2,4),Blocks.STONE);
        helper.succeedWhen(()->{helper.assertTrue(bore.isActive(),"Redstone did not activate bore");helper.assertTrue(helper.getBlockState(new BlockPos(8,2,4)).isAir(),"Normal scanner/charge/delay tick never mined the block");bore.discard();});
    }
    @GameTest(template="elemental_tools_empty")
    public static void ownerCanPackConstructAndRecoverAmmoWithoutDuplication(GameTestHelper helper){
        var turret=turret(helper,false,new BlockPos(4,2,4));var owner=GameTestPlayers.create(helper);owner.setPos(turret.position());turret.setOwnerUUID(owner.getUUID());turret.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ARROW,17));owner.setShiftKeyDown(true);var area=turret.getBoundingBox().inflate(3);turret.interact(owner,InteractionHand.OFF_HAND);
        helper.assertTrue(turret.isRemoved(),"Owner crouch interaction did not pack turret");var drops=helper.getLevel().getEntitiesOfClass(ItemEntity.class,area);var placer=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:turret_placer_basic"));int ammo=0,placers=0;for(var drop:drops){if(drop.getItem().is(Items.ARROW))ammo+=drop.getItem().getCount();if(drop.getItem().is(placer))placers+=drop.getItem().getCount();}helper.assertValueEqual(ammo,17,"Packing changed ammunition count");helper.assertValueEqual(placers,1,"Packing did not return exactly one placer");helper.assertTrue(turret.getMainHandItem().isEmpty(),"Packed entity retained a second inventory copy");drops.forEach(ItemEntity::discard);helper.succeed();
    }
    private ConstructGameTests(){}
}
