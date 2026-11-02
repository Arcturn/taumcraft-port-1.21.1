package thaumcraft.port.golem.ai;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.*;

/** TC6 block/entity ticket worker: reservation, reachability, retries and suspension. */
public final class GolemTaskGoal extends Goal {
    private final GolemEntity golem;
    private final byte type;
    private int counter = -1, cooldown, pause;
    private double minDistance = 4;
    private BlockPos targetBlock, previousRamble;
    public GolemTaskGoal(GolemEntity golem, byte type) { this.golem = golem; this.type = type; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
    @Override public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        cooldown = 5;
        if (golem.getTask() != null && !golem.getTask().isSuspended()) return false;
        targetBlock = null;
        var tickets = type == 0 ? TaskHandler.getBlockTasksSorted(golem.level(), golem.getUUID(), golem)
                : TaskHandler.getEntityTasksSorted(golem.level(), golem.getUUID(), golem);
        for (Task ticket : tickets) {
            if (ticket.isCompleted() || ticket.isSuspended() || !validTraits(ticket) || !ticket.canGolemPerformTask(golem)
                    || !golem.isWithinHome(ticket.getPos()) || !golem.level().hasChunkAt(ticket.getPos())) continue;
            if (type == 1 && (ticket.getEntity() == null || !ticket.getEntity().isAlive())) continue;
            if (!reachable(ticket)) continue;
            if (type == 0) targetBlock = adjacentSpace(ticket.getPos());
            else minDistance = 3.5 + Math.pow(ticket.getEntity().getBbWidth() / 2, 2);
            golem.setTask(ticket); ticket.setReserved(true);
            golem.level().broadcastEntityEvent(golem, (byte)5);
            var seal = SealHandler.getSealEntity(golem.level(), ticket.getSealPos());
            if (seal != null) seal.getSeal().onTaskStarted(golem.level(), golem, ticket);
            return true;
        }
        return false;
    }
    private boolean validTraits(Task ticket) {
        var seal = SealHandler.getSealEntity(golem.level(), ticket.getSealPos()); if (seal == null) return true;
        if (seal.isStoppedByRedstone(golem.level())) return false;
        if (seal.isLocked() && (golem.getOwnerUUID() == null || !golem.getOwnerUUID().toString().equals(seal.getOwner()))) return false;
        var required = seal.getSeal().getRequiredTags();
        if (required != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(required))) return false;
        var forbidden = seal.getSeal().getForbiddenTags();
        if (forbidden != null) for (var trait : forbidden) if (golem.hasTrait(trait)) return false;
        return true;
    }
    private boolean reachable(Task task) {
        if (type == 0 && golem.distanceToSqr(Vec3.atCenterOf(task.getPos())) < minDistance
                || type == 1 && golem.distanceToSqr(task.getEntity()) < minDistance) return true;
        Path path = type == 0 ? golem.getNavigation().createPath(task.getPos(), 0) : golem.getNavigation().createPath(task.getEntity(), 0);
        if (path == null || path.getEndNode() == null) return false;
        var end = path.getEndNode(); var pos = task.getPos(); int dx = end.x - pos.getX(), dy = end.y - pos.getY(), dz = end.z - pos.getZ();
        if (type == 1) return dx * dx + dz * dz < minDistance;
        if (dx == 0 && dz == 0 && dy == 2) dy--;
        return dx * dx + dy * dy + dz * dz < 2.25;
    }
    private BlockPos adjacentSpace(BlockPos pos) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (var direction : Direction.Plane.HORIZONTAL) {
            var candidate = pos.relative(direction);
            if (!golem.level().getBlockState(candidate).getCollisionShape(golem.level(), candidate).isEmpty()) continue;
            double next = golem.distanceToSqr(Vec3.atCenterOf(candidate));
            if (next < distance) { best = candidate; distance = next; }
        }
        return best;
    }
    @Override public void start() { moveTo(); counter = 0; pause = 0; }
    @Override public boolean canContinueToUse() {
        var task = golem.getTask(); return counter >= 0 && counter <= 1000 && task != null && !task.isSuspended()
                && golem.level().hasChunkAt(task.getPos()) && (type != 1 || task.getEntity() != null && task.getEntity().isAlive());
    }
    private void moveTo() {
        var task = golem.getTask(); if (task == null) return;
        if (type == 1) { if (task.getEntity() != null) golem.getNavigation().moveTo(task.getEntity(), golem.getGolemMoveSpeed()); }
        else { var pos = targetBlock == null ? task.getPos() : targetBlock; golem.getNavigation().moveTo(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, golem.getGolemMoveSpeed()); }
    }
    @Override public void tick() {
        var task = golem.getTask(); if (task == null) return;
        if (pause-- <= 0) {
            double distance = type == 1 ? golem.distanceToSqr(task.getEntity()) : golem.distanceToSqr(Vec3.atCenterOf(targetBlock == null ? task.getPos() : targetBlock));
            if (distance > minDistance) {
                task.setCompletion(false); counter++;
                if (counter % 5 == 0) {
                    if (previousRamble != null && previousRamble.equals(golem.blockPosition())) {
                        var position = DefaultRandomPos.getPosTowards(golem, 6, 4, Vec3.atLowerCornerOf(task.getPos()), Math.PI / 2);
                        if (position != null) golem.getNavigation().moveTo(position.x + .5, position.y + .5, position.z + .5, golem.getGolemMoveSpeed());
                    } else moveTo();
                    previousRamble = golem.blockPosition();
                }
            } else {
                if (!validTraits(task) || !task.canGolemPerformTask(golem)) { task.setSuspended(true); return; }
                TaskHandler.completeTask(task, golem);
                if (golem.getTask() != null && golem.getTask().isCompleted()) { if (counter >= 0) counter = 0; pause = 0; }
                else { pause = 10; counter++; }
                counter--;
            }
        }
        task = golem.getTask(); if (task == null) return;
        if (type == 1 && task.getEntity() != null) golem.getLookControl().setLookAt(task.getEntity(), 10, golem.getMaxHeadXRot());
        else golem.getLookControl().setLookAt(task.getPos().getX() + .5, task.getPos().getY() + .5, task.getPos().getZ() + .5, 10, golem.getMaxHeadXRot());
    }
    @Override public void stop() {
        var task = golem.getTask(); if (task == null) return;
        if (!task.isCompleted() && task.isReserved()) golem.level().broadcastEntityEvent(golem, (byte)6);
        if (task.isCompleted() && !task.isSuspended()) task.setSuspended(true);
        task.setReserved(false); golem.setTask(null);
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
}
