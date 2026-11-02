package thaumcraft.port.golem.ai;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.golem.GolemEntity;
public final class GolemHomeGoal extends Goal {
    private final GolemEntity golem; private int idle = 10; private Vec3 destination;
    public GolemHomeGoal(GolemEntity golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
    @Override public boolean canUse() {
        if (idle > 0) { idle--; return false; } idle = 50;
        if (!golem.hasHome() || golem.getTask() != null) return false;
        double distance = golem.distanceToSqr(Vec3.atCenterOf(golem.getHomePosition())); if (distance < 5) return false;
        destination = distance > 1024 ? DefaultRandomPos.getPosTowards(golem, 16, 7, Vec3.atLowerCornerOf(golem.getHomePosition()), Math.PI / 2) : Vec3.atLowerCornerOf(golem.getHomePosition());
        return destination != null;
    }
    @Override public void start() { golem.getNavigation().moveTo(destination.x, destination.y, destination.z, golem.getGolemMoveSpeed()); }
    @Override public boolean canContinueToUse() { return golem.getTask() == null && !golem.getNavigation().isDone() && golem.distanceToSqr(Vec3.atCenterOf(golem.getHomePosition())) > 3; }
    @Override public void stop() { idle = 50; golem.getNavigation().stop(); }
}
