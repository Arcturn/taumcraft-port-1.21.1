package thaumcraft.port.golem.ai;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
public final class GolemFlyingNavigation extends FlyingPathNavigation {
    public GolemFlyingNavigation(Mob mob, Level level) { super(mob, level); setCanOpenDoors(false); setCanPassDoors(true); setCanFloat(true); }
    @Override protected PathFinder createPathFinder(int maxVisitedNodes) { nodeEvaluator = new GolemFlightNodeEvaluator(); return new PathFinder(nodeEvaluator, maxVisitedNodes); }
    @Override protected boolean canUpdatePath() { return true; }
    @Override protected Vec3 getTempMobPos() { return mob.position().add(0, mob.getBbHeight() * .5, 0); }
    @Override protected void followThePath() {
        var position = getTempMobPos();
        if (position.distanceToSqr(path.getNextEntityPos(mob)) < mob.getBbWidth() * mob.getBbWidth()) path.advance();
        for (int i = Math.min(path.getNextNodeIndex() + 6, path.getNodeCount() - 1); i > path.getNextNodeIndex(); i--) {
            var next = path.getEntityPosAtNode(mob, i);
            if (next.distanceToSqr(position) <= 36 && canMoveDirectly(position, next)) { path.setNextNodeIndex(i); break; }
        }
        doStuckDetection(position);
    }
    @Override protected boolean canMoveDirectly(Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to.add(0, mob.getBbHeight() * .5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
    }
}
