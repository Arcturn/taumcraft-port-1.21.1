package thaumcraft.port.golem.ai;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;
import thaumcraft.port.golem.GolemEntity;
public final class GolemFollowOwnerGoal extends Goal {
    private final GolemEntity golem; private LivingEntity owner; private int delay; private float oldWater;
    public GolemFollowOwnerGoal(GolemEntity golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { owner = golem.getOwner(); return owner != null && golem.distanceToSqr(owner) >= 100; }
    @Override public boolean canContinueToUse() { return owner != null && owner.isAlive() && !golem.getNavigation().isDone() && golem.distanceToSqr(owner) > 4; }
    @Override public void start() { delay = 0; oldWater = golem.getPathfindingMalus(PathType.WATER); golem.setPathfindingMalus(PathType.WATER, 0); }
    @Override public void stop() { owner = null; golem.getNavigation().stop(); golem.setPathfindingMalus(PathType.WATER, oldWater); }
    @Override public void tick() {
        if (owner == null) return; golem.getLookControl().setLookAt(owner, 10, golem.getMaxHeadXRot());
        if (--delay > 0) return; delay = 10;
        if (golem.getNavigation().moveTo(owner, 1) || golem.isLeashed() || golem.distanceToSqr(owner) < 144) return;
        var base = owner.blockPosition().offset(-2, 0, -2);
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
            if (x >= 1 && z >= 1 && x <= 3 && z <= 3) continue;
            var pos = base.offset(x, 0, z);
            if (!golem.level().hasChunkAt(pos) || !golem.level().getBlockState(pos.below()).isSolidRender(golem.level(), pos.below())
                    || !golem.level().getBlockState(pos).getCollisionShape(golem.level(), pos).isEmpty()
                    || !golem.level().getBlockState(pos.above()).getCollisionShape(golem.level(), pos.above()).isEmpty()) continue;
            var box = golem.getBoundingBox().move(pos.getX() + .5 - golem.getX(), pos.getY() - golem.getY(), pos.getZ() + .5 - golem.getZ());
            if (!golem.level().noCollision(golem, box)) continue;
            golem.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, golem.getYRot(), golem.getXRot()); golem.getNavigation().stop(); return;
        }
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
}
