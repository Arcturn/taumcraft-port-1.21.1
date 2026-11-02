package thaumcraft.port.auromancy.focus;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public abstract class FocusEffect extends FocusNode {
    @Override public EnumUnitType getType() { return EnumUnitType.EFFECT; }
    @Override public final EnumSupplyType[] mustBeSupplied() { return new EnumSupplyType[]{EnumSupplyType.TARGET}; }
    @Override public EnumSupplyType[] willSupply() { return null; }
    public abstract boolean execute(HitResult target, @Nullable Trajectory trajectory, float power, int index);
    public float getDamageForDisplay(float finalPower) { return 0; }
    public abstract void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz);
    public void onCast(Entity caster) { }
}
