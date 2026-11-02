package thaumcraft.port.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
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
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.seal.SealEntity;
import thaumcraft.port.golem.seal.SealFill;
import thaumcraft.port.golem.seal.SealHandler;
import thaumcraft.port.golem.seal.SealPickup;
import thaumcraft.port.golem.seal.SealPos;
import thaumcraft.port.golem.task.TaskHandler;

/** World-tick workflows: seals issue tickets and the normal navigation goals do all work. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemWorkflowGameTests {
    private static final BlockPos SOURCE = new BlockPos(4, 1, 5);
    private static final BlockPos DESTINATION = new BlockPos(16, 2, 5);

    @GameTest(template = "elemental_tools_empty", timeoutTicks = 600)
    public static void pickupWalksWithCargoAndFillsDistantChest(GameTestHelper helper) {
        var fixture = new Fixture(helper, 23, 1);
        awaitDelivery(helper, fixture, tick -> { });
    }

    @GameTest(template = "elemental_tools_empty", timeoutTicks = 650)
    public static void twoWorkersCompeteForOneGroundStackWithoutDuplication(GameTestHelper helper) {
        var fixture = new Fixture(helper, 37, 2);
        awaitDelivery(helper, fixture, tick -> {
            long reservations = fixture.workers.stream()
                    .filter(worker -> worker.getTask() != null && !worker.getTask().isSuspended()
                            && worker.getTask().getEntity() == fixture.drop).count();
            helper.assertTrue(reservations <= 1, "Both workers reserved the same ground item");
            long pickupTickets = TaskHandler.getTasks(helper.getLevel()).values().stream()
                    .filter(task -> fixture.pickup.getSealPos().equals(task.getSealPos())
                            && task.getEntity() == fixture.drop && !task.isSuspended() && !task.isCompleted()).count();
            helper.assertTrue(pickupTickets <= 1, "One ground entity generated competing pickup tickets");
        });
    }

    @GameTest(template = "elemental_tools_empty", timeoutTicks = 700)
    public static void colorAndLockedOwnerEachBlockWorkUntilConfigurationMatches(GameTestHelper helper) {
        var fixture = new Fixture(helper, 19, 1);
        var worker = fixture.workers.getFirst();
        fixture.pickup.setColor((byte) 2);
        fixture.fill.setColor((byte) 2);
        worker.setGolemColor((byte) 1);
        String stranger = UUID.randomUUID().toString();
        awaitDelivery(helper, fixture, tick -> {
            if (tick <= 160) {
                helper.assertTrue(fixture.drop.isAlive(), "Blocked worker picked up the item");
                helper.assertValueEqual(fixture.drop.getItem().getCount(), fixture.quantity,
                        "Blocked worker changed the ground stack");
                helper.assertValueEqual(fixture.carried(), 0, "Blocked worker acquired cargo");
                helper.assertValueEqual(fixture.stored(), 0, "Blocked worker delivered cargo");
            }
            if (tick == 80) {
                // Matching the color alone must not bypass another player's locked seal.
                fixture.pickup.setOwner(stranger);
                worker.setGolemColor((byte) 2);
                SealHandler.changed(helper.getLevel(), fixture.pickup);
            }
            if (tick == 160) {
                fixture.pickup.setOwner(fixture.owner.toString());
                SealHandler.changed(helper.getLevel(), fixture.pickup);
            }
        });
    }

    private static void awaitDelivery(GameTestHelper helper, Fixture fixture, IntConsumer perTick) {
        poll(helper, fixture, perTick, 1);
    }

    private static void poll(GameTestHelper helper, Fixture fixture, IntConsumer perTick, int tick) {
        helper.runAfterDelay(1, () -> {
            perTick.accept(tick);
            for (var worker : fixture.workers) {
                helper.assertTrue(worker.isAlive(), "Placed golem vanished before completing its work");
                if (fixture.carried(worker) > 0) {
                    if (worker.position().distanceToSqr(fixture.source) < 16) fixture.sawPickup = true;
                    if (worker.position().distanceToSqr(fixture.source) > 16
                            && worker.position().distanceToSqr(fixture.destination) > 16) fixture.sawCargoInTransit = true;
                }
                if (worker.position().distanceToSqr(fixture.destination) < 16) fixture.sawArrival = true;
            }
            helper.assertValueEqual(fixture.loose() + fixture.carried() + fixture.stored(), fixture.quantity,
                    "World-tick transfer lost or duplicated items at tick " + tick);
            if (fixture.stored() == fixture.quantity) {
                helper.assertTrue(fixture.sawPickup, "No worker physically collected the ground stack");
                helper.assertTrue(fixture.sawCargoInTransit, "Cargo never travelled on a worker between seals");
                helper.assertTrue(fixture.sawArrival, "Items reached the chest without a worker approaching it");
                helper.assertTrue(fixture.drop.isRemoved(), "Delivered ground stack remained in the world");
                helper.assertValueEqual(fixture.carried(), 0, "Delivered items remained in worker equipment");
                if (fixture.deliveredAt < 0) fixture.deliveredAt = tick;
                // Leave the normal task/cleanup loop running to catch a second completion or stale reservation.
                if (tick - fixture.deliveredAt >= 40) {
                    fixture.cleanup();
                    helper.succeed();
                    return;
                }
            }
            poll(helper, fixture, perTick, tick + 1);
        });
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final UUID owner = UUID.randomUUID();
        final int quantity;
        final ItemStack sample;
        final List<GolemEntity> workers = new ArrayList<>();
        final SealEntity pickup, fill;
        final ItemEntity drop;
        final ChestBlockEntity chest;
        final Vec3 source, destination;
        final AABB bounds;
        boolean sawPickup, sawCargoInTransit, sawArrival;
        int deliveredAt = -1;

        Fixture(GameTestHelper helper, int quantity, int workerCount) {
            this.helper = helper;
            this.quantity = quantity;
            for (int x = 1; x <= 21; x++) for (int z = 1; z <= 15; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                for (int y = 2; y <= 6; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
            bounds = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))),
                    Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(23, 7, 23))));
            source = Vec3.atBottomCenterOf(helper.absolutePos(SOURCE.above()));
            destination = Vec3.atCenterOf(helper.absolutePos(DESTINATION));
            // Unique components and locked owners isolate concurrently running world workflows.
            sample = new ItemStack(Items.APPLE);
            sample.set(DataComponents.CUSTOM_NAME, Component.literal("Golem delivery " + owner));
            var pickupBehavior = new SealPickup();
            pickupBehavior.setBlacklist(false);
            pickupBehavior.setFilterSlot(0, sample);
            pickup = new SealEntity(helper.getLevel(), new SealPos(helper.absolutePos(SOURCE), Direction.UP), pickupBehavior);
            var fillBehavior = new SealFill();
            fillBehavior.setBlacklist(false);
            fillBehavior.setFilterSlot(0, sample);
            fillBehavior.setFilterSlotSize(0, quantity);
            helper.setBlock(DESTINATION, Blocks.CHEST);
            chest = (ChestBlockEntity) helper.getBlockEntity(DESTINATION);
            fill = new SealEntity(helper.getLevel(), new SealPos(helper.absolutePos(DESTINATION), Direction.WEST), fillBehavior);
            for (var seal : List.of(pickup, fill)) {
                seal.setOwner(owner.toString());
                seal.setLocked(true);
                seal.setColor((byte) 3);
                helper.assertTrue(SealHandler.addSealEntity(helper.getLevel(), seal), "Could not place workflow seal");
            }
            drop = new ItemEntity(helper.getLevel(), source.x, source.y + .1, source.z, sample.copyWithCount(quantity));
            drop.setDeltaMovement(Vec3.ZERO);
            drop.setNoPickUpDelay();
            helper.assertTrue(helper.getLevel().addFreshEntity(drop), "Could not spawn workflow ground item");
            for (int index = 0; index < workerCount; index++) {
                var worker = GolemContent.GOLEM.get().create(helper.getLevel());
                helper.assertTrue(worker != null, "Golem entity type did not create a worker");
                var start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 8 + index * 2)));
                worker.moveTo(start.x, start.y, start.z, 0, 0);
                worker.setValidSpawn();
                worker.setOwnerUUID(owner);
                worker.setGolemColor((byte) 3);
                worker.initializePlaced();
                helper.assertTrue(helper.getLevel().addFreshEntity(worker), "Could not spawn workflow worker");
                workers.add(worker);
            }
        }

        int carried(GolemEntity worker) {
            return worker.getCarrying().stream().filter(this::matches).mapToInt(ItemStack::getCount).sum();
        }
        int carried() { return workers.stream().mapToInt(this::carried).sum(); }
        int stored() {
            int count = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                var stack = chest.getItem(slot);
                if (matches(stack)) count += stack.getCount();
            }
            return count;
        }
        int loose() {
            return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds,
                    entity -> entity.isAlive() && matches(entity.getItem())).stream()
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
        }
        boolean matches(ItemStack stack) { return ItemStack.isSameItemSameComponents(sample, stack); }
        void cleanup() {
            SealHandler.removeSealEntity(helper.getLevel(), pickup.getSealPos(), true);
            SealHandler.removeSealEntity(helper.getLevel(), fill.getSealPos(), true);
            workers.forEach(GolemEntity::discard);
            drop.discard();
        }
    }

    private GolemWorkflowGameTests() { }
}
