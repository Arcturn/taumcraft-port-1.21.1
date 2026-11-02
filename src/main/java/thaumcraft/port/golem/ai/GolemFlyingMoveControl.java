package thaumcraft.port.golem.ai;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import thaumcraft.port.golem.GolemEntity;
/** Original horizontal .033 / vertical .0125 acceleration and near-target braking. */
public final class GolemFlyingMoveControl extends MoveControl {
    private final GolemEntity golem;
    public GolemFlyingMoveControl(GolemEntity golem) { super(golem); this.golem = golem; }
    @Override public void tick() {
        if (operation != Operation.MOVE_TO) return;
        double dx = wantedX - golem.getX(), dy = wantedY - golem.getY(), dz = wantedZ - golem.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < golem.getBoundingBox().getSize()) { operation = Operation.WAIT; golem.setDeltaMovement(golem.getDeltaMovement().scale(.5)); return; }
        golem.setDeltaMovement(golem.getDeltaMovement().add(dx / distance * .033 * speedModifier, dy / distance * .0125 * speedModifier, dz / distance * .033 * speedModifier));
        var target = golem.getTarget(); double yawX = target == null ? golem.getDeltaMovement().x : target.getX() - golem.getX();
        double yawZ = target == null ? golem.getDeltaMovement().z : target.getZ() - golem.getZ();
        golem.setYRot(-(float)Mth.atan2(yawX, yawZ) * Mth.RAD_TO_DEG); golem.yBodyRot = golem.getYRot();
    }
}
