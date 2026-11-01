package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;
import thaumcraft.port.alchemy.spa.SpaContent;
import thaumcraft.port.alchemy.spa.SpaEntity;
import thaumcraft.port.alchemy.spa.SpaMenu;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class SpaGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mixConsumesOneSaltAndOneBucketOnlyEveryFortyTicks(GameTestHelper helper) {
        var spa = fixture(helper, true);
        spa.tank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
        spa.tick();
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().above()).isAir(), "Spa mixed water without bath salts");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 5000, "Missing salt still consumed water");
        spa.setItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get(), 3));
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().above()).is(AlchemyLiquids.PURIFYING_BATH_BLOCK.get()), "Spa did not produce purifying bath");
        helper.assertTrue(helper.getLevel().getFluidState(spa.getBlockPos().above()).isSource(), "Spa produced a flowing bath instead of a source");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 4000, "Spa did not consume exactly one bucket");
        helper.assertValueEqual(spa.getItem(0).getCount(), 2, "Spa did not consume exactly one salt");
        tick(spa, 39);
        helper.assertValueEqual(spa.tank.getFluidAmount(), 4000, "Spa filled faster than the original forty-tick interval");
        spa.tick();
        helper.assertValueEqual(spa.tank.getFluidAmount(), 3000, "Second scheduled placement did not consume water");
        helper.assertValueEqual(spa.getItem(0).getCount(), 1, "Second scheduled placement consumed the wrong salt count");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void redstoneStopsProductionAndPlainModePlacesOtherFluidsWithoutSalt(GameTestHelper helper) {
        var spa = fixture(helper, true);
        spa.tank.fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE);
        spa.setItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get(), 2));
        spa.tick();
        helper.assertValueEqual(spa.tank.getFluidAmount(), 2000, "Mix mode converted lava into bath water");
        spa.toggleMix();
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().above()).isAir(), "Powered spa did not stop");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 2000, "Powered spa consumed fluid");
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().below(), Blocks.STONE.defaultBlockState());
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().above()).is(Blocks.LAVA), "Plain mode rejected a placeable non-water fluid");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 1000, "Plain mode consumed the wrong fluid amount");
        helper.assertValueEqual(spa.getItem(0).getCount(), 2, "Plain mode consumed bath salts");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fluidAndSaltCapabilitiesRespectCapacitySimulationAndOriginalSides(GameTestHelper helper) {
        var spa = fixture(helper, false);
        for (Direction side : Direction.values()) {
            var fluid = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, spa.getBlockPos(), side);
            helper.assertTrue(fluid != null, "Missing spa fluid capability on " + side);
            helper.assertValueEqual(fluid.getTankCapacity(0), 5000, "Wrong spa capacity");
            var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, spa.getBlockPos(), side);
            if (side == Direction.UP) helper.assertTrue(inventory == null || inventory.getSlots() == 0, "Upper face exposes salt slot");
            else {
                helper.assertTrue(inventory != null && inventory.getSlots() == 1, "Salt slot unavailable on " + side);
                helper.assertValueEqual(inventory.insertItem(0, new ItemStack(Items.DIRT), false).getCount(), 1, "Spa automation accepted a non-salt item");
                helper.assertTrue(inventory.insertItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get()), false).isEmpty(), "Salt insertion failed");
                helper.assertValueEqual(inventory.extractItem(0, 1, false).getCount(), 1, "Salt extraction failed");
            }
        }
        var tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, spa.getBlockPos(), Direction.UP);
        helper.assertValueEqual(tank.fill(new FluidStack(Fluids.WATER, 7000), FluidAction.SIMULATE), 5000, "Simulated fill did not cap at five buckets");
        helper.assertTrue(spa.tank.isEmpty(), "Simulated fill mutated the tank");
        helper.assertValueEqual(tank.fill(new FluidStack(Fluids.WATER, 7000), FluidAction.EXECUTE), 5000, "Executed fill overfilled the tank");
        helper.assertValueEqual(tank.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), 0, "Tank mixed incompatible fluids");
        helper.assertValueEqual(tank.drain(1200, FluidAction.SIMULATE).getAmount(), 1200, "Simulated drain returned wrong amount");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 5000, "Simulated drain mutated the tank");
        helper.assertTrue(tank.drain(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE).isEmpty(), "Requested mismatched fluid drained water");
        helper.assertValueEqual(tank.drain(1200, FluidAction.EXECUTE).getAmount(), 1200, "External fluid drain failed");
        helper.assertValueEqual(tank.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.EXECUTE), 1200, "Partial tank top-up exceeded capacity");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bathRequiresSupportConnectionAndNeverOverwritesObstacles(GameTestHelper helper) {
        var spa = fixture(helper, false);
        spa.toggleMix();
        spa.tank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().above(), Blocks.STONE.defaultBlockState());
        spa.tick();
        helper.assertValueEqual(spa.tank.getFluidAmount(), 5000, "Blocked outlet still consumed fluid");
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().above()).is(Blocks.STONE), "Spa destroyed an obstacle");
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().above(), Blocks.AIR.defaultBlockState());
        tick(spa, 40);
        helper.assertValueEqual(spa.tank.getFluidAmount(), 4000, "Spa did not fill its own supported top");
        tick(spa, 40);
        helper.assertValueEqual(spa.tank.getFluidAmount(), 4000, "Spa placed a neighboring source without a solid floor");
        // A detached floor cell inside the radius is still not a valid next source.
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().offset(-2, 0, 0), Blocks.STONE.defaultBlockState());
        tick(spa, 40);
        helper.assertValueEqual(spa.tank.getFluidAmount(), 4000, "Spa jumped across a gap");
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().west(), Blocks.STONE.defaultBlockState());
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getFluidState(spa.getBlockPos().west().above()).isSource(), "Spa did not fill a connected supported neighbor");
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getFluidState(spa.getBlockPos().west(2).above()).isSource(), "Spa could not extend to the edge of its radius");
        helper.getLevel().setBlockAndUpdate(spa.getBlockPos().west(3), Blocks.STONE.defaultBlockState());
        tick(spa, 40);
        helper.assertTrue(helper.getLevel().getBlockState(spa.getBlockPos().west(3).above()).isAir(), "Spa placed outside its five-block-wide bath");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 2000, "Invalid placement consumed water");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bathStopsAtTwentyFiveSourcesWithoutSpendingExtraIngredients(GameTestHelper helper) {
        var spa = fixture(helper, true);
        spa.setItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get(), 32));
        spa.tank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
        spa.tick();
        for (int iteration = 0; iteration < 26; iteration++) {
            spa.tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
            tick(spa, 40);
        }
        int sources = 0;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (helper.getLevel().getFluidState(spa.getBlockPos().offset(x, 1, z)).isSource()) sources++;
        helper.assertValueEqual(sources, 25, "Spa did not fill the entire five-by-five floor");
        helper.assertValueEqual(spa.getItem(0).getCount(), 7, "Full bath kept consuming ingredients");
        helper.assertValueEqual(spa.tank.getFluidAmount(), 5000, "Full bath wasted fluid");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void modeTankAndSaltPersistAndBreakingDropsStoredIngredients(GameTestHelper helper) {
        var spa = fixture(helper, false);
        spa.toggleMix();
        spa.tank.fill(new FluidStack(Fluids.LAVA, 2750), FluidAction.EXECUTE);
        spa.setItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get(), 17));
        var saved = spa.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new SpaEntity(spa.getBlockPos(), spa.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertFalse(restored.mixing(), "Mode was reset on reload");
        helper.assertTrue(restored.tank.getFluid().is(Fluids.LAVA), "Fluid type was lost on reload");
        helper.assertValueEqual(restored.tank.getFluidAmount(), 2750, "Fluid quantity was lost on reload");
        helper.assertValueEqual(restored.getItem(0).getCount(), 17, "Bath salts were lost on reload");
        helper.getLevel().destroyBlock(spa.getBlockPos(), false);
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(spa.getBlockPos()).inflate(2),
                item -> item.getItem().is(AlchemyLiquids.BATH_SALTS.get())).stream().mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 17, "Breaking spa did not drop stored salts exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void originalMenuSlotsShiftClickFullInventoryAndButtonAuthorization(GameTestHelper helper) {
        var spa = fixture(helper, false);
        var player = viewer(helper, spa);
        var menu = new SpaMenu(23, player.getInventory(), spa);
        player.containerMenu = menu;
        helper.assertValueEqual(menu.slots.size(), 37, "Wrong original spa inventory size");
        helper.assertValueEqual(menu.getSlot(0).x, 65, "Wrong original salt slot X");
        helper.assertValueEqual(menu.getSlot(0).y, 31, "Wrong original salt slot Y");
        helper.assertValueEqual(menu.getSlot(1).y, 84, "Wrong original player inventory Y");
        helper.assertValueEqual(menu.getSlot(28).y, 142, "Wrong original hotbar Y");
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 3));
        helper.assertTrue(menu.quickMoveStack(player, 28).isEmpty(), "Shift-click moved invalid ingredients");
        player.getInventory().setItem(0, new ItemStack(AlchemyLiquids.BATH_SALTS.get(), 12));
        helper.assertValueEqual(menu.quickMoveStack(player, 28).getCount(), 12, "Salt shift-click failed");
        helper.assertValueEqual(spa.getItem(0).getCount(), 12, "Shift-click lost salt");
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "Full inventory should reject extraction");
        helper.assertValueEqual(spa.getItem(0).getCount(), 12, "Full inventory extraction lost ingredients");
        player.getInventory().setItem(8, ItemStack.EMPTY);
        helper.assertValueEqual(menu.quickMoveStack(player, 0).getCount(), 12, "Free hotbar slot was ignored");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 12, "Original reverse transfer order changed");
        helper.assertFalse(menu.clickMenuButton(player, 0), "Unknown button toggled the spa");
        helper.assertTrue(menu.clickMenuButton(player, 1), "Valid toggle was rejected");
        helper.assertFalse(spa.mixing(), "Menu button did not synchronize mode");
        var stranger = viewer(helper, spa);
        stranger.containerMenu = menu;
        helper.assertFalse(menu.clickMenuButton(stranger, 1), "A different viewer controlled this menu");
        player.containerMenu = player.inventoryMenu;
        helper.assertFalse(menu.clickMenuButton(player, 1), "Closed menu still controlled the spa");
        player.containerMenu = menu;
        player.setPos(spa.getBlockPos().getX() + 20, spa.getBlockPos().getY(), spa.getBlockPos().getZ());
        helper.assertFalse(menu.clickMenuButton(player, 1), "Remote player controlled an out-of-range spa");
        helper.assertFalse(spa.mixing(), "Rejected clicks changed the saved mode");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bucketFillAndMenuSnapshotUseRealFluidState(GameTestHelper helper) {
        var spa = fixture(helper, false);
        var player = viewer(helper, spa);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(spa.getBlockPos()), Direction.NORTH, spa.getBlockPos(), false));
        helper.assertValueEqual(spa.tank.getFluidAmount(), 1000, "Right-click did not empty the water bucket into spa");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Spa did not return the empty bucket");
        var client = new SpaMenu(24, player.getInventory(), spa.getBlockPos());
        for (int field = 0; field < 4; field++) client.data.set(field, spa.data.get(field));
        helper.assertTrue(client.mixing(), "Menu snapshot lost mix mode");
        helper.assertTrue(client.fluid().is(Fluids.WATER), "Menu snapshot lost fluid type");
        helper.assertValueEqual(client.fluid().getAmount(), 1000, "Menu snapshot lost fluid amount");
        spa.tank.drain(5000, FluidAction.EXECUTE);
        for (int field = 0; field < 4; field++) client.data.set(field, spa.data.get(field));
        helper.assertTrue(client.fluid().isEmpty(), "Drained spa still displays a fluid");
        helper.succeed();
    }

    private static SpaEntity fixture(GameTestHelper helper, boolean floor) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(POS);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            level.setBlock(pos.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(pos.offset(x, 0, z), (floor ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
        }
        level.setBlock(pos, SpaContent.SPA.get().defaultBlockState(), 2);
        return (SpaEntity) level.getBlockEntity(pos);
    }
    private static ServerPlayer viewer(GameTestHelper helper, SpaEntity spa) {
        var player = GameTestPlayers.create(helper);
        player.setPos(spa.getBlockPos().getX() + .5, spa.getBlockPos().getY(), spa.getBlockPos().getZ() - 1.5);
        return player;
    }
    private static void tick(SpaEntity spa, int count) { for (int i = 0; i < count; i++) spa.tick(); }
    private SpaGameTests() {}
}
