package thaumcraft.port.golem.ai;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.port.golem.GolemEntity;
/** BETA26 dart cadence: 20..25 ticks scaled by distance, 16-block sight range. */
public final class GolemArrowAttackGoal extends Goal {
    private final GolemEntity golem; private int cooldown = -1, visible;
    public GolemArrowAttackGoal(GolemEntity golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { return golem.getTarget() != null; }
    @Override public boolean canContinueToUse() { return canUse() || !golem.getNavigation().isDone(); }
    @Override public void stop() { visible = 0; cooldown = -1; }
    @Override public void tick() {
        var target = golem.getTarget(); if (target == null) return;
        double distance = golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
        boolean sees = golem.getSensing().hasLineOfSight(target); visible = sees ? visible + 1 : 0;
        if (distance <= 256 && visible >= 20) golem.getNavigation().stop(); else golem.getNavigation().moveTo(target, 1);
        golem.getLookControl().setLookAt(target, 10, 30);
        if (--cooldown == 0) {
            if (distance > 256 || !sees) return;
            float ratio = (float)Math.sqrt(distance) / 16; golem.performRangedAttack(target, Mth.clamp(ratio, .1f, 1)); cooldown = Mth.floor(ratio * 5 + 20);
        } else if (cooldown < 0) cooldown = Mth.floor((float)Math.sqrt(distance) / 16 * 5 + 20);
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
}
