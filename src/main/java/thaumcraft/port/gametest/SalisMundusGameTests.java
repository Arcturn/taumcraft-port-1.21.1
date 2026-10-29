package thaumcraft.port.gametest;

import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.SalisMundusRituals;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.essentia.ThaumatoriumBlock;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.research.ResearchStorage;

/** Exercises the real use-on entry point and server ticks, including reservation and cancellation. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class SalisMundusGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final BlockPos LOWER = TARGET.above();
    private static final BlockPos UPPER = LOWER.above();

    private SalisMundusGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void vanillaRightClickStartsRitualBeforeOpeningCraftingMenu(GameTestHelper helper) {
        MenuTrackingPlayer player = new MenuTrackingPlayer(helper, 2);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit(helper, TARGET));
        helper.assertTrue(result.consumesAction(), "Right-click did not start the dust ritual");
        helper.assertValueEqual(player.menuOpenAttempts, 0, "Crafting table opened its menu before the dust ran");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Dust use replaced the player's menu");
        helper.assertValueEqual(dustInInventory(player), 1, "Right-click did not consume exactly one dust");
        helper.runAfterDelay(49, () -> helper.assertBlockPresent(Blocks.CRAFTING_TABLE, TARGET));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.WORKBENCH.get(), TARGET);
            helper.assertValueEqual(dustInInventory(player), 1, "Right-click charged the ritual twice");
            helper.assertValueEqual(player.menuOpenAttempts, 0, "Delayed ritual unexpectedly opened a menu");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void vanillaRightClickKeepsUnrelatedFurnaceInteraction(GameTestHelper helper) {
        MenuTrackingPlayer player = new MenuTrackingPlayer(helper, 2);
        helper.setBlock(TARGET, Blocks.FURNACE);
        var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit(helper, TARGET));
        helper.assertTrue(result.consumesAction(), "Dust blocked normal furnace interaction");
        helper.assertValueEqual(player.menuOpenAttempts, 1, "Dust did not pass the click to the furnace");
        helper.assertTrue(player.containerMenu instanceof FurnaceMenu, "Normal furnace menu was not created");
        helper.assertValueEqual(dustInInventory(player), 2, "Opening an unrelated menu consumed dust");
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(Blocks.FURNACE, TARGET);
            helper.assertValueEqual(dustInInventory(player), 2, "Unrelated block interaction queued a ritual");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void workbenchWaitsFiftyTicksAndConsumesOnce(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        useDust(helper, player, TARGET);
        helper.assertBlockPresent(Blocks.CRAFTING_TABLE, TARGET);
        helper.assertValueEqual(dustInInventory(player), 2, "Dust was not consumed at ritual start");
        helper.runAfterDelay(49, () -> {
            helper.assertBlockPresent(Blocks.CRAFTING_TABLE, TARGET);
            helper.assertValueEqual(dustInInventory(player), 2, "Pending ritual consumed more dust");
        });
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.WORKBENCH.get(), TARGET);
            helper.assertValueEqual(dustInInventory(player), 2, "Completing ritual consumed dust again");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void waterCauldronPreservesItsWaterAfterDelay(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 2);
        helper.setBlock(TARGET, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 2));
        useDust(helper, player, TARGET);
        helper.assertValueEqual(dustInInventory(player), 1, "Water cauldron did not reserve one dust");
        helper.runAfterDelay(49, () -> {
            helper.assertBlockPresent(Blocks.WATER_CAULDRON, TARGET);
            helper.assertValueEqual(helper.getLevel().getBlockState(helper.absolutePos(TARGET))
                    .getValue(LayeredCauldronBlock.LEVEL), 2, "Water changed before transformation");
        });
        helper.runAfterDelay(52, () -> {
            assertCrucibleWater(helper, TARGET, 666);
            helper.assertValueEqual(dustInInventory(player), 1, "Water cauldron consumed dust twice");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void creativeEmptyCauldronUsesDelayWithoutConsumption(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 2);
        player.getAbilities().instabuild = true;
        helper.setBlock(TARGET, Blocks.CAULDRON);
        useDust(helper, player, TARGET);
        helper.assertValueEqual(dustInInventory(player), 2, "Creative use consumed dust");
        helper.runAfterDelay(49, () -> helper.assertBlockPresent(Blocks.CAULDRON, TARGET));
        helper.runAfterDelay(52, () -> {
            assertCrucibleWater(helper, TARGET, 0);
            helper.assertValueEqual(dustInInventory(player), 2, "Creative completion consumed dust");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void repeatedClicksByTwoPlayersReserveOnlyOneDust(GameTestHelper helper) {
        ServerPlayer first = playerWithDust(helper, 3);
        ServerPlayer second = playerWithDust(helper, 3);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        useDust(helper, first, TARGET);
        useDust(helper, first, TARGET);
        useDust(helper, second, TARGET);
        helper.assertValueEqual(dustInInventory(first), 2, "Repeated click charged the first player twice");
        helper.assertValueEqual(dustInInventory(second), 3, "Reserved target charged a second player");
        helper.runAfterDelay(49, () -> {
            helper.assertBlockPresent(Blocks.CRAFTING_TABLE, TARGET);
            useDust(helper, second, TARGET);
            helper.assertValueEqual(dustInInventory(second), 3, "Late duplicate click consumed dust");
        });
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.WORKBENCH.get(), TARGET);
            helper.assertValueEqual(dustInInventory(first) + dustInInventory(second), 5,
                    "One transformation did not cost exactly one dust");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidTargetAndSneakingDoNotStartRituals(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        BlockPos sneakingTarget = TARGET.east();
        helper.setBlock(TARGET, Blocks.STONE);
        helper.setBlock(sneakingTarget, Blocks.CRAFTING_TABLE);
        useDust(helper, player, TARGET);
        player.setShiftKeyDown(true);
        useDust(helper, player, sneakingTarget);
        player.setShiftKeyDown(false);
        helper.assertValueEqual(dustInInventory(player), 3, "Rejected use consumed dust");
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(Blocks.STONE, TARGET);
            helper.assertBlockPresent(Blocks.CRAFTING_TABLE, sneakingTarget);
            helper.assertValueEqual(dustInInventory(player), 3, "Rejected use left a pending ritual");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void changedSourceCancelsAndRefundsDust(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        helper.setBlock(TARGET.below(), Blocks.STONE);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        useDust(helper, player, TARGET);
        helper.assertValueEqual(dustInInventory(player), 2, "Ritual did not reserve dust");
        helper.runAfterDelay(25, () -> helper.setBlock(TARGET, Blocks.STONE));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(Blocks.STONE, TARGET);
            helper.assertValueEqual(dustIncludingDrops(helper, player, TARGET), 3,
                    "Canceled ritual lost or duplicated its reserved dust");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void cancellationDropsRefundWhenInventoryIsFull(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 1);
        helper.setBlock(TARGET.below(), Blocks.STONE);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        useDust(helper, player, TARGET);
        helper.assertValueEqual(dustInInventory(player), 0, "Ritual did not reserve the last dust");
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Blocks.STONE, 64));
        }
        helper.runAfterDelay(25, () -> helper.setBlock(TARGET, Blocks.STONE));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(Blocks.STONE, TARGET);
            helper.assertValueEqual(dustInInventory(player), 0, "Refund overwrote a full inventory slot");
            helper.assertValueEqual(dustIncludingDrops(helper, player, TARGET), 1,
                    "Full inventory caused canceled ritual payment to disappear");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void thaumatoriumRequiresResearchBeforeReservingDust(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        placeThaumatoriumParts(helper);
        helper.assertFalse(ResearchStorage.get(player.getServer()).player(player.getUUID())
                .knows("THAUMATORIUM"), "Test player unexpectedly has thaumatorium research");
        useDust(helper, player, LOWER);
        useDust(helper, player, UPPER);
        helper.assertValueEqual(dustInInventory(player), 3, "Locked assembly consumed dust");
        helper.runAfterDelay(52, () -> {
            assertUnchangedThaumatoriumParts(helper);
            helper.assertValueEqual(dustInInventory(player), 3, "Locked assembly started a delayed ritual");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void thaumatoriumReservesBothHalvesForOneDelayedTransformation(GameTestHelper helper) {
        ServerPlayer first = playerWithDust(helper, 3);
        ServerPlayer second = playerWithDust(helper, 3);
        unlockThaumatorium(first);
        unlockThaumatorium(second);
        placeThaumatoriumParts(helper);
        first.setYRot(90);
        Direction facing = first.getDirection().getOpposite();
        useDust(helper, first, UPPER);
        useDust(helper, first, LOWER);
        useDust(helper, second, LOWER);
        helper.assertValueEqual(dustInInventory(first), 2, "Upper/lower clicks charged twice");
        helper.assertValueEqual(dustInInventory(second), 3, "Reserved multiblock charged another player");
        helper.runAfterDelay(49, () -> assertUnchangedThaumatoriumParts(helper));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.CRUCIBLE.get(), TARGET);
            helper.assertBlockPresent(EssentiaContent.THAUMATORIUM.get(), LOWER);
            helper.assertBlockPresent(EssentiaContent.THAUMATORIUM_TOP.get(), UPPER);
            for (BlockPos part : new BlockPos[]{LOWER, UPPER}) {
                helper.assertValueEqual(helper.getLevel().getBlockState(helper.absolutePos(part))
                        .getValue(ThaumatoriumBlock.FACING), facing, "Thaumatorium halves face different directions");
            }
            helper.assertValueEqual(dustInInventory(first) + dustInInventory(second), 5,
                    "Thaumatorium completion charged extra dust");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void changedMultiblockPartCancelsWholeRitual(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        unlockThaumatorium(player);
        placeThaumatoriumParts(helper);
        useDust(helper, player, LOWER);
        helper.assertValueEqual(dustInInventory(player), 2, "Multiblock did not reserve one dust");
        helper.runAfterDelay(25, () -> helper.setBlock(UPPER, Blocks.STONE));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.CRUCIBLE.get(), TARGET);
            helper.assertBlockPresent(EssentiaContent.CONSTRUCT.get(), LOWER);
            helper.assertBlockPresent(Blocks.STONE, UPPER);
            helper.assertValueEqual(dustIncludingDrops(helper, player, LOWER), 3,
                    "Canceled multiblock lost or duplicated its reserved dust");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void pendingRitualRoundTripsItsPaymentSourcesAndDeadline(GameTestHelper helper) {
        ServerPlayer player = playerWithDust(helper, 3);
        helper.setBlock(TARGET, Blocks.CRAFTING_TABLE);
        long startedAt = helper.getLevel().getGameTime();
        useDust(helper, player, TARGET);
        var factory = new SavedData.Factory<>(SalisMundusRituals::new, SalisMundusRituals::load, null);
        var rituals = helper.getLevel().getDataStorage().get(factory, "thaumcraft_salis_rituals_v1");
        helper.assertTrue(rituals != null, "Starting a ritual did not create persistent saved data");
        var registries = helper.getLevel().registryAccess();
        CompoundTag snapshot = rituals.save(new CompoundTag(), registries);
        CompoundTag ownJob = null;
        long position = helper.absolutePos(TARGET).asLong();
        for (Tag entry : snapshot.getList("pending", Tag.TAG_COMPOUND)) {
            CompoundTag job = (CompoundTag) entry;
            if (job.getList("sources", Tag.TAG_COMPOUND).getCompound(0).getLong("pos") == position) {
                ownJob = job;
                break;
            }
        }
        helper.assertTrue(ownJob != null, "Pending ritual disappeared from saved data");
        helper.assertValueEqual(ownJob.getLong("due"), startedAt + 50,
                "Saved deadline is not fifty ticks after use");
        ItemStack payment = ItemStack.parseOptional(registries, ownJob.getCompound("payment"));
        helper.assertTrue(payment.is(PortContent.SALIS_MUNDUS.get()) && payment.getCount() == 1,
                "Saved ritual did not retain exactly one paid dust");
        // Keep the live queue intact: other GameTests can have rituals in the same level.
        CompoundTag roundTrip = SalisMundusRituals.load(snapshot, registries).save(new CompoundTag(), registries);
        helper.assertValueEqual(roundTrip, snapshot, "Saved ritual fields changed during load/save");
        helper.runAfterDelay(49, () -> helper.assertBlockPresent(Blocks.CRAFTING_TABLE, TARGET));
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(MachineContent.WORKBENCH.get(), TARGET);
            helper.assertValueEqual(dustInInventory(player), 2, "Saving ritual duplicated its payment");
            helper.succeed();
        });
    }

    private static ServerPlayer playerWithDust(GameTestHelper helper, int count) {
        ServerPlayer player = GameTestPlayers.create(helper);
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.SALIS_MUNDUS.get(), count));
        return player;
    }

    private static void useDust(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        PortContent.SALIS_MUNDUS.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    /** FakePlayer normally suppresses openMenu entirely; retain real menu creation to test routing. */
    private static final class MenuTrackingPlayer extends FakePlayer {
        private int menuOpenAttempts;

        MenuTrackingPlayer(GameTestHelper helper, int dustCount) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tc-menu-test"));
            setGameMode(GameType.SURVIVAL);
            BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
            setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.SALIS_MUNDUS.get(), dustCount));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
            menuOpenAttempts++;
            if (provider == null) return OptionalInt.empty();
            var menu = provider.createMenu(menuOpenAttempts, getInventory(), this);
            if (menu == null) return OptionalInt.empty();
            containerMenu = menu;
            return OptionalInt.of(menu.containerId);
        }
    }

    private static void placeThaumatoriumParts(GameTestHelper helper) {
        helper.setBlock(TARGET, MachineContent.CRUCIBLE.get());
        helper.setBlock(LOWER, EssentiaContent.CONSTRUCT.get());
        helper.setBlock(UPPER, EssentiaContent.CONSTRUCT.get());
    }

    private static void assertUnchangedThaumatoriumParts(GameTestHelper helper) {
        helper.assertBlockPresent(MachineContent.CRUCIBLE.get(), TARGET);
        helper.assertBlockPresent(EssentiaContent.CONSTRUCT.get(), LOWER);
        helper.assertBlockPresent(EssentiaContent.CONSTRUCT.get(), UPPER);
    }

    private static void unlockThaumatorium(ServerPlayer player) {
        ResearchStorage.get(player.getServer()).player(player.getUUID()).flag("THAUMATORIUM");
    }

    private static void assertCrucibleWater(GameTestHelper helper, BlockPos relative, int amount) {
        helper.assertBlockPresent(MachineContent.CRUCIBLE.get(), relative);
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(entity instanceof CrucibleEntity, "Transformed crucible has no block entity");
        helper.assertValueEqual(((CrucibleEntity) entity).state.water(), amount,
                "Cauldron water was not preserved in the crucible");
    }

    private static int dustInInventory(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(PortContent.SALIS_MUNDUS.get())) count += stack.getCount();
        }
        return count;
    }

    private static int dustIncludingDrops(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        int count = dustInInventory(player);
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(relative)).inflate(2),
                item -> item.isAlive() && item.getItem().is(PortContent.SALIS_MUNDUS.get()))) {
            count += entity.getItem().getCount();
        }
        return count;
    }
}
