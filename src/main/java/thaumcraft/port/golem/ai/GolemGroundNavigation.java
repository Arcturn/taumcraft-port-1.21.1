package thaumcraft.port.golem.ai;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
/** Modern ground-node evaluator retains TC6 door entry and ordinary safe-ground routing. */
public final class GolemGroundNavigation extends GroundPathNavigation {
    public GolemGroundNavigation(Mob mob, Level level) { super(mob, level); setCanOpenDoors(false); setCanPassDoors(true); setCanFloat(false); }
}
