package thaumcraft.port.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.ai.GolemTaskGoal;
import thaumcraft.port.golem.part.GolemHead;
import thaumcraft.port.golem.part.GolemLeg;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemTaskGameTests {
    private static GolemEntity worker(GameTestHelper h) {
        var golem = GolemContent.GOLEM.get().create(h.getLevel());
        var props = new GolemProperties(); props.setHead(GolemHead.getHeads()[1]);
        golem.setValidSpawn(); golem.setOwnerUUID(UUID.randomUUID()); golem.setProperties(props);
        var pos = h.absolutePos(new BlockPos(2, 2, 2)); golem.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        golem.initializePlaced(); return golem;
    }
    private static SealEntity seal(GameTestHelper h, ProbeSeal behavior) {
        var pos = new BlockPos(3, 1, 2); h.setBlock(pos, Blocks.STONE);
        var seal = new SealEntity(h.getLevel(), new SealPos(h.absolutePos(pos), Direction.UP), behavior);
        h.assertTrue(SealHandler.addSealEntity(h.getLevel(), seal), "Could not install task test seal"); return seal;
    }
    private static Task ticket(GameTestHelper h, SealEntity seal, GolemEntity golem) {
        var task = new Task(seal.getSealPos(), seal.getSealPos().pos.above());
        task.setGolemUUID(golem.getUUID()); TaskHandler.addTask(h.getLevel(), task); return task;
    }
    private static void clean(GameTestHelper h, SealEntity seal) {
        SealHandler.removeSealEntity(h.getLevel(), seal.getSealPos(), true);
        TaskHandler.getTasks(h.getLevel()).values().removeIf(task -> seal.getSealPos().equals(task.getSealPos()));
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void reservationCompletesOnceAndRankUpKeepsTheRunningGoal(GameTestHelper h) {
        var behavior = new ProbeSeal(); var seal = seal(h, behavior); var first = worker(h); var second = worker(h);
        first.setRankXp(999); var task = ticket(h, seal, first); task.setGolemUUID(null);
        var firstGoal = new GolemTaskGoal(first, (byte)0); var secondGoal = new GolemTaskGoal(second, (byte)0);
        h.assertTrue(firstGoal.canUse(), "Reachable ticket was not claimed");
        h.assertTrue(task.isReserved() && first.getTask() == task, "Claim did not reserve the ticket");
        h.assertFalse(secondGoal.canUse(), "Second golem claimed the reserved ticket");
        var navigation = first.getNavigation(); firstGoal.start(); firstGoal.tick();
        h.assertValueEqual(behavior.completions, 1, "Task completion did not run exactly once");
        h.assertValueEqual(first.getProperties().getRank(), 1, "Completion rank XP was lost");
        h.assertTrue(first.getNavigation() == navigation, "Ranking rebuilt navigation during its task callback");
        h.assertFalse(firstGoal.canContinueToUse(), "Completed task kept its worker goal running");
        firstGoal.stop(); TaskHandler.completeTask(task, first);
        h.assertTrue(task.isSuspended() && !task.isReserved() && first.getTask() == null, "Completion did not release and suspend its reservation");
        h.assertValueEqual(behavior.completions, 1, "Completed task ran a second time"); clean(h, seal); h.succeed();
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void unloadingWorkerImmediatelyReleasesItsReservation(GameTestHelper h) {
        var behavior = new ProbeSeal(); var seal = seal(h, behavior); var first = worker(h); var second = worker(h);
        var task = ticket(h, seal, first); task.setGolemUUID(null);
        h.assertTrue(new GolemTaskGoal(first, (byte)0).canUse(), "First worker did not reserve the task");
        first.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertFalse(task.isReserved(), "Chunk unload left a stale task reservation");
        h.assertTrue(new GolemTaskGoal(second, (byte)0).canUse(), "Second worker could not reclaim the unloaded worker's task");
        second.releaseTask(); clean(h, seal); h.succeed();
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void claimChecksOwnerColorTraitsAndRedstone(GameTestHelper h) {
        var behavior = new ProbeSeal(); var seal = seal(h, behavior); var golem = worker(h); var task = ticket(h, seal, golem);
        seal.setOwner(UUID.randomUUID().toString()); seal.setLocked(true);
        h.assertFalse(new GolemTaskGoal(golem,(byte)0).canUse(), "Claim ignored locked seal owner");
        seal.setOwner(golem.getOwnerUUID().toString()); seal.setColor((byte)2); golem.setGolemColor((byte)3);
        h.assertFalse(new GolemTaskGoal(golem,(byte)0).canUse(), "Claim ignored color channel");
        golem.setGolemColor((byte)2); behavior.required = new EnumGolemTrait[]{EnumGolemTrait.BREAKER};
        h.assertFalse(new GolemTaskGoal(golem,(byte)0).canUse(), "Claim ignored required trait");
        behavior.required = null; behavior.forbidden = new EnumGolemTrait[]{EnumGolemTrait.SMART};
        h.assertFalse(new GolemTaskGoal(golem,(byte)0).canUse(), "Claim ignored forbidden trait");
        behavior.forbidden = null; seal.setRedstoneSensitive(true); h.getLevel().setBlockAndUpdate(seal.getSealPos().pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertFalse(new GolemTaskGoal(golem,(byte)0).canUse(), "Claim ignored redstone stop");
        h.assertFalse(task.isReserved(), "Rejected task retained a reservation"); clean(h, seal); h.succeed();
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void completionRevalidatesEveryMutableSealRestriction(GameTestHelper h) {
        for (int change = 0; change < 6; change++) {
            var behavior = new ProbeSeal(); var seal = seal(h, behavior); var golem = worker(h); var task = ticket(h, seal, golem);
            var goal = new GolemTaskGoal(golem, (byte)0); h.assertTrue(goal.canUse(), "Valid task was rejected before change " + change); goal.start();
            switch (change) {
                case 0 -> { seal.setLocked(true); seal.setOwner(UUID.randomUUID().toString()); }
                case 1 -> { golem.setGolemColor((byte)1); seal.setColor((byte)2); }
                case 2 -> behavior.required = new EnumGolemTrait[]{EnumGolemTrait.BREAKER};
                case 3 -> behavior.forbidden = new EnumGolemTrait[]{EnumGolemTrait.SMART};
                case 4 -> { seal.setRedstoneSensitive(true); h.getLevel().setBlockAndUpdate(seal.getSealPos().pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState()); }
                case 5 -> behavior.allowed = false;
            }
            goal.tick(); h.assertTrue(task.isSuspended(), "Changed restriction did not suspend task " + change);
            h.assertValueEqual(behavior.completions, 0, "Task changed the world after restriction change " + change);
            goal.stop(); h.assertFalse(task.isReserved(), "Invalidated task kept its reservation");
            h.getLevel().removeBlock(seal.getSealPos().pos.above(), false); clean(h, seal);
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=100)
    public static void taskExpiryRunsOncePerSecondAndCallsSuspensionOnce(GameTestHelper h) {
        var behavior = new ProbeSeal(); var seal = seal(h, behavior); var task = ticket(h, seal, worker(h)); task.setLifespan((short)5);
        h.runAfterDelay(22, () -> {
            h.assertTrue(task.getLifespan() >= 3 && task.getLifespan() <= 4, "Task lifespan was decremented every tick instead of once per second");
            h.assertTrue(TaskHandler.getTask(h.getLevel(), task.getId()) == task, "Live task expired before its second-based lifetime");
            h.assertValueEqual(behavior.suspensions, 0, "Live task received suspension callback"); task.setLifespan((short)0);
            h.runAfterDelay(22, () -> {
                h.assertTrue(task.isSuspended() && TaskHandler.getTask(h.getLevel(), task.getId()) == null, "Expired task was not removed on next cleanup");
                h.assertValueEqual(behavior.suspensions, 1, "Expiry callback missing or repeated");
                h.runAfterDelay(22, () -> { h.assertValueEqual(behavior.suspensions, 1, "Removed task suspended again"); clean(h, seal); h.succeed(); });
            });
        });
    }
    @GameTest(template="elemental_tools_empty", timeoutTicks=240)
    public static void flyingWorkerPhysicallyReachesElevatedTaskBeforeCompleting(GameTestHelper h) {
        for (int x=1; x<=10; x++) for (int z=1; z<=4; z++) h.setBlock(new BlockPos(x,1,z), Blocks.STONE);
        var behavior = new ProbeSeal(); var sealPos = new SealPos(h.absolutePos(new BlockPos(8,4,2)), Direction.UP);
        h.getLevel().setBlockAndUpdate(sealPos.pos, Blocks.STONE.defaultBlockState());
        var seal = new SealEntity(h.getLevel(), sealPos, behavior); SealHandler.addSealEntity(h.getLevel(), seal);
        var flyer = worker(h); var props = flyer.getProperties(); props.setLegs(GolemLeg.getLegs()[3]); flyer.setProperties(props);
        double initialY = flyer.getY(); var task = ticket(h, seal, flyer); h.getLevel().addFreshEntity(flyer);
        h.runAfterDelay(5, () -> h.assertValueEqual(behavior.completions, 0, "Flying worker completed the distant task remotely"));
        h.succeedWhen(() -> {
            h.assertValueEqual(behavior.completions, 1, "Flying worker did not reach its elevated task");
            h.assertTrue(behavior.completionY > initialY + 1.5, "Flying worker never gained altitude");
            h.assertTrue(behavior.completionDistance < 10, "Flying worker completed outside working reach");
            flyer.discard(); clean(h, seal);
        });
    }
    private static final class ProbeSeal implements ISeal {
        int completions, suspensions; double completionY, completionDistance; boolean allowed = true; EnumGolemTrait[] required, forbidden;
        public String getKey() { return "thaumcraft:test_task"; }
        public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return true; }
        public void tickSeal(Level level, ISealEntity seal) { }
        public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) { completions++; completionY=golem.getGolemEntity().getY(); completionDistance=golem.getGolemEntity().distanceToSqr(task.getPos().getCenter()); golem.addRankXp(1); return true; }
        public void onTaskSuspension(Level level, Task task) { suspensions++; }
        public boolean canGolemPerformTask(IGolemAPI golem, Task task) { return allowed; }
        public ResourceLocation getSealIcon() { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seal_blank"); }
        public EnumGolemTrait[] getRequiredTags() { return required; }
        public EnumGolemTrait[] getForbiddenTags() { return forbidden; }
    }
    private GolemTaskGameTests() { }
}
