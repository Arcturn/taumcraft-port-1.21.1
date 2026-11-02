package thaumcraft.port.auromancy.focus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;

public final class FocusMediumRoot extends FocusMedium {
    private Trajectory[] trajectories;
    private HitResult[] targets;
    public FocusMediumRoot() { }
    public FocusMediumRoot(Trajectory[] trajectories, HitResult[] targets) { this.trajectories = trajectories; this.targets = targets; }
    @Override public String getResearch() { return "BASEAUROMANCY"; }
    @Override public String getKey() { return "ROOT"; }
    @Override public int getComplexity() { return 0; }
    @Override public Aspect getAspect() { return null; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[]{EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY}; }
    @Override public HitResult[] supplyTargets() { return targets; }
    @Override public Trajectory[] supplyTrajectories() { return trajectories; }
    public void setupFromCaster(LivingEntity caster) {
        trajectories = new Trajectory[]{new Trajectory(generateSourceVector(caster), caster.getLookAngle())};
        targets = new HitResult[]{new EntityHitResult(caster)};
    }
    public void setupFromCasterToTarget(LivingEntity caster, Entity target, double offset) {
        setupFromCasterToTargetLoc(caster, target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0 + offset, target.getZ());
    }
    public void setupFromCasterToTargetLoc(LivingEntity caster, double x, double y, double z) {
        var source = generateSourceVector(caster);
        trajectories = new Trajectory[]{new Trajectory(source, new Vec3(x, y, z).subtract(source).normalize())};
        targets = new HitResult[]{new EntityHitResult(caster)};
    }
    private Vec3 generateSourceVector(LivingEntity caster) { return caster.position().add(0, caster.getEyeHeight() - (double) .1f, 0); }
}
