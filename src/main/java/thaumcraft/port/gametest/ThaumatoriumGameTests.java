package thaumcraft.port.gametest;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.BrainBoxBlock;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.essentia.JarEntity;
import thaumcraft.port.essentia.ThaumatoriumEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

/** Runs recipes against real inventories, blocks, capabilities and serialized machine state. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ThaumatoriumGameTests {
    private static final BlockPos BASE = new BlockPos(1, 3, 1);
    private static final ResourceLocation NITOR = ResourceLocation.parse("thaumcraft:crucible_nitor");
    private static final ResourceLocation BRASS = ResourceLocation.parse("thaumcraft:crucible_brassingot");
    private static final ResourceLocation THAUMIUM = ResourceLocation.parse("thaumcraft:crucible_thaumiumingot");

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void outputEntersFrontInventoryAndConsumesOneCatalyst(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        ready(machine, player);
        helper.setBlock(BASE.relative(Direction.NORTH), Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(machine.getBlockPos().north());
        tick(machine, 5);
        helper.assertValueEqual(machine.getItem(0).getCount(), 1, "Craft did not consume exactly one catalyst");
        helper.assertValueEqual(machine.contents.total(), 0, "Craft retained the spent essentia");
        helper.assertTrue(chest.getItem(0).is(MachineContent.NITOR.get("nitor_yellow").get().asItem()), "Output bypassed front chest");
        helper.assertValueEqual(chest.getItem(0).getCount(), 1, "Front chest received incorrect output count");
        helper.assertTrue(outputs(helper, machine).isEmpty(), "Inventory output was also dropped into world");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fullOutputInventoryEjectsRemainder(GameTestHelper helper) {
        var machine = fixture(helper);
        ready(machine, player(helper, machine));
        helper.setBlock(BASE.relative(Direction.NORTH), Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(machine.getBlockPos().north());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        tick(machine, 5);
        helper.assertValueEqual(outputs(helper, machine).size(), 1, "Full output inventory swallowed the recipe result");
        helper.assertValueEqual(chest.getItem(0).getCount(), 64, "Craft replaced an occupied chest stack");
        var output = outputs(helper, machine).getFirst();
        helper.assertTrue(Math.abs(output.getDeltaMovement().z + .3) < .000001, "Ejection lost original outward velocity");
        output.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void researchIsRequiredForSelectionButNotEveryAutomatedCraft(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        machine.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
        machine.choose(player, NITOR);
        helper.assertTrue(machine.selected.isEmpty(), "Unknown research allowed recipe selection");
        var research = ResearchStorage.get(player.getServer()).player(player.getUUID());
        research.stage("UNLOCKALCHEMY", 3, false);
        machine.choose(player, NITOR);
        helper.assertTrue(machine.selected.containsKey(NITOR), "Known recipe could not be selected");
        research.stage("UNLOCKALCHEMY", 1, false);
        machine.contents.restore(nitorAspects());
        tick(machine, 5);
        helper.assertValueEqual(machine.getItem(0).getCount(), 1, "Saved selection was incorrectly gated on owner research at craft time");
        helper.assertValueEqual(outputs(helper, machine).size(), 1, "Saved selection failed to produce output");
        outputs(helper, machine).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void selectionChangesAndSaveRetainAccumulatedEssentia(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        machine.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
        machine.contents.restore(Map.of(Aspect.FIRE, 7, Aspect.ENERGY, 2));
        machine.choose(player, NITOR);
        machine.choose(player, NITOR);
        helper.assertValueEqual(machine.contents.amount(Aspect.FIRE), 7, "Unselecting discarded accumulated essentia");
        machine.choose(player, NITOR);
        var restored = new ThaumatoriumEntity(machine.getBlockPos(), machine.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(machine.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.contents.amount(Aspect.ENERGY), 2, "Essentia did not survive save/load");
        helper.assertValueEqual(restored.selected.get(NITOR), player.getUUID(), "Recipe owner did not survive save/load");
        helper.assertValueEqual(restored.getItem(0).getCount(), 2, "Catalyst did not survive save/load");
        machine.setItem(0, ItemStack.EMPTY);
        helper.assertValueEqual(machine.choices(player).stream().map(holder -> holder.id()).toList(), List.of(NITOR),
                "Empty catalyst must retain selected recipe in the UI");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void topSharesCatalystAndLowerComparatorMeasuresInventory(GameTestHelper helper) {
        var machine = fixture(helper);
        var top = (ThaumatoriumEntity) helper.getLevel().getBlockEntity(machine.getBlockPos().above());
        top.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 64));
        helper.assertValueEqual(machine.getItem(0).getCount(), 64, "Direct upper inventory is detached from machine");
        helper.assertValueEqual(machine.getBlockState().getAnalogOutputSignal(helper.getLevel(), machine.getBlockPos()), 15,
                "Full catalyst slot must output comparator signal 15");
        helper.assertFalse(top.getBlockState().hasAnalogOutputSignal(), "Upper half invented an extra comparator output");
        var capability = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, top.getBlockPos(), Direction.UP);
        helper.assertTrue(capability != null, "Upper half lost item capability");
        helper.assertValueEqual(capability.extractItem(0, 32, false).getCount(), 32, "Upper automation cannot extract catalyst");
        helper.assertValueEqual(machine.getBlockState().getAnalogOutputSignal(helper.getLevel(), machine.getBlockPos()), 8,
                "Half-filled catalyst slot has wrong comparator signal");
        top.clearContent();
        helper.assertTrue(machine.isEmpty(), "Clearing upper inventory did not clear main slot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brainUpgradeAddsTwoRecipesAndRemovalTrimsLatestSelection(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        var research = ResearchStorage.get(player.getServer()).player(player.getUUID());
        research.stage("UNLOCKALCHEMY", 3, false);
        research.stage("METALLURGY", 2, false);
        helper.setBlock(BASE.east(), EssentiaContent.BRAIN_BOX.get().defaultBlockState().setValue(BrainBoxBlock.FACING, Direction.WEST));
        tick(machine, 1);
        helper.assertValueEqual(machine.capacity, 3, "One inward brain box must add two recipe choices");
        machine.setItem(0, new ItemStack(Items.GLOWSTONE_DUST)); machine.choose(player, NITOR);
        machine.setItem(0, new ItemStack(Items.IRON_INGOT)); machine.choose(player, BRASS); machine.choose(player, THAUMIUM);
        helper.assertValueEqual(machine.selected.size(), 3, "Upgraded machine rejected selectable recipes");
        machine.contents.restore(Map.of(Aspect.FIRE, 4));
        helper.setBlock(BASE.east(), Blocks.AIR);
        tick(machine, 39);
        helper.assertValueEqual(machine.capacity, 3, "Environment upgrades changed before original 40-tick poll");
        tick(machine, 1);
        helper.assertValueEqual(machine.capacity, 1, "Removed brain box did not reduce recipe capacity");
        helper.assertValueEqual(List.copyOf(machine.selected.keySet()), List.of(NITOR), "Capacity loss removed oldest instead of newest choices");
        helper.assertValueEqual(machine.contents.amount(Aspect.FIRE), 4, "Upgrade removal discarded accumulated essentia");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void redstoneStopsCraftingUntilUnpowered(GameTestHelper helper) {
        var machine = fixture(helper);
        ready(machine, player(helper, machine));
        helper.setBlock(BASE.east(), Blocks.REDSTONE_BLOCK);
        tick(machine, 5);
        helper.assertValueEqual(machine.getItem(0).getCount(), 2, "Powered machine consumed a catalyst");
        helper.assertValueEqual(machine.contents.total(), 30, "Powered machine spent stored essentia");
        helper.assertValueEqual(machine.suction(Direction.WEST), 0, "Powered machine still draws essentia");
        helper.setBlock(BASE.east(), Blocks.AIR);
        tick(machine, 5);
        helper.assertValueEqual(machine.getItem(0).getCount(), 1, "Unpowered machine did not resume crafting");
        outputs(helper, machine).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brainBoxOnlyPlacesOnMachineSidesAndDropsWithSupport(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        var block = EssentiaContent.BRAIN_BOX.get();
        var stack = new ItemStack(block);
        var backHit = new BlockHitResult(Vec3.atCenterOf(machine.getBlockPos()), Direction.SOUTH, machine.getBlockPos(), false);
        var frontHit = new BlockHitResult(Vec3.atCenterOf(machine.getBlockPos()), Direction.NORTH, machine.getBlockPos(), false);
        var valid = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, backHit));
        helper.assertTrue(valid != null && valid.getValue(BrainBoxBlock.FACING) == Direction.NORTH, "Brain box rejected rear attachment");
        helper.assertTrue(block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, frontHit)) == null,
                "Brain box attached to output face");
        helper.assertFalse(block.defaultBlockState().canSurvive(helper.getLevel(), machine.getBlockPos().east()),
                "Brain box survived without inward-facing machine support");
        helper.setBlock(BASE.south(), valid);
        helper.assertTrue(Math.abs(valid.getShape(helper.getLevel(), machine.getBlockPos().south()).bounds().getXsize() - .625) < .000001,
                "Brain box collision box no longer matches original 10-pixel cube");
        var aura = AuraSystem.get(helper.getLevel(), machine.getBlockPos());
        helper.assertTrue(aura != null, "Aura missing for break check");
        float flux = aura.flux();
        machine.contents.restore(Map.of(Aspect.FIRE, 20));
        helper.getLevel().destroyBlock(machine.getBlockPos(), false);
        helper.assertTrue(helper.getLevel().getBlockState(machine.getBlockPos().south()).isAir(), "Unsupported brain box remained floating");
        helper.assertTrue(Math.abs(aura.flux() - flux) < .000001, "Breaking a thaumatorium invented essentia pollution absent from TC6");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void pullsOneEssentiaPerStepAndUpperPushSharesBoundedStore(GameTestHelper helper) {
        var machine = fixture(helper);
        var player = player(helper, machine);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        machine.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
        machine.choose(player, NITOR);
        helper.setBlock(BASE.west(), EssentiaContent.ALEMBIC.get());
        var source = (JarEntity) helper.getLevel().getBlockEntity(machine.getBlockPos().west());
        source.tank.insert(Aspect.FIRE, 20, false, false);
        tick(machine, 4);
        helper.assertValueEqual(machine.contents.total(), 0, "Machine pulled essentia before fifth processing tick");
        tick(machine, 1);
        helper.assertValueEqual(machine.contents.amount(Aspect.FIRE), 1, "Processing step must pull exactly one essentia");
        helper.assertValueEqual(source.tank.amount(), 19, "Transport removed more than one essentia from source");
        var top = (ThaumatoriumEntity) helper.getLevel().getBlockEntity(machine.getBlockPos().above());
        helper.assertValueEqual(top.insert(Aspect.LIGHT, 99, Direction.EAST, true), 10, "Simulated push did not respect recipe demand cap");
        helper.assertValueEqual(machine.contents.amount(Aspect.LIGHT), 0, "Simulated push mutated essentia");
        helper.assertValueEqual(top.insert(Aspect.LIGHT, 99, Direction.EAST, false), 10, "Upper transport did not fill required aspect");
        helper.assertValueEqual(machine.contents.amount(Aspect.LIGHT), 10, "Upper transport wrote a detached store");
        helper.assertValueEqual(machine.insert(Aspect.FIRE, 1, Direction.NORTH, false), 0, "Output face accepted essentia");
        helper.assertValueEqual(machine.insert(Aspect.WATER, 1, Direction.EAST, false), 0, "Machine accepted an aspect absent from its recipe");
        tick(machine, 5);
        helper.assertValueEqual(machine.contents.amount(Aspect.FIRE), 2, "Five-tick processing cadence lost one-unit transport limit");
        helper.succeed();
    }

    private static ThaumatoriumEntity fixture(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BASE);
        level.setBlock(pos.below(2), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        level.setBlock(pos.below(), MachineContent.CRUCIBLE.get().defaultBlockState(), 2);
        level.setBlock(pos, EssentiaContent.THAUMATORIUM.get().defaultBlockState(), 2);
        level.setBlock(pos.above(), EssentiaContent.THAUMATORIUM_TOP.get().defaultBlockState(), 2);
        var machine = (ThaumatoriumEntity) level.getBlockEntity(pos);
        helper.assertTrue(machine.formed(), "Thaumatorium fixture is not formed");
        return machine;
    }
    private static ServerPlayer player(GameTestHelper helper, ThaumatoriumEntity machine) {
        var player = GameTestPlayers.create(helper);
        player.setPos(machine.getBlockPos().getX() + .5, machine.getBlockPos().getY(), machine.getBlockPos().getZ() - 1.5);
        return player;
    }
    private static Map<Aspect, Integer> nitorAspects() { return Map.of(Aspect.FIRE, 10, Aspect.ENERGY, 10, Aspect.LIGHT, 10); }
    private static void ready(ThaumatoriumEntity machine, ServerPlayer player) {
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        machine.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
        machine.choose(player, NITOR);
        machine.contents.restore(nitorAspects());
    }
    private static void tick(ThaumatoriumEntity machine, int count) { for (int i = 0; i < count; i++) machine.tick(); }
    private static List<ItemEntity> outputs(GameTestHelper helper, ThaumatoriumEntity machine) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(machine.getBlockPos()).inflate(2),
                entity -> entity.isAlive() && entity.getItem().is(MachineContent.NITOR.get("nitor_yellow").get().asItem()));
    }
}
