package thaumcraft.port.golem.ai;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.port.golem.GolemEntity;
public final class GolemOwnerTargetGoal extends TargetGoal {
    private static final TargetingConditions TARGETS = TargetingConditions.forCombat().ignoreLineOfSight();
    private final GolemEntity golem; private final boolean defend; private int timestamp; private LivingEntity target;
    public GolemOwnerTargetGoal(GolemEntity golem, boolean defend) { super(golem, false); this.golem = golem; this.defend = defend; setFlags(EnumSet.of(Flag.TARGET)); }
    @Override public boolean canUse() {
        var owner = golem.getOwner(); if (owner == null) return false;
        target = defend ? owner.getLastHurtByMob() : owner.getLastHurtMob();
        int time = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
        return time != timestamp && target != null && target.isAlive() && !golem.isAlliedTo(target) && canAttack(target, TARGETS);
    }
    @Override public void start() { golem.setTarget(target); targetMob = target; var owner = golem.getOwner(); if (owner != null) timestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp(); super.start(); }
}
