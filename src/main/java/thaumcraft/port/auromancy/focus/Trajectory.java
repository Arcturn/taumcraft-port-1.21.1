package thaumcraft.port.auromancy.focus;

import net.minecraft.world.phys.Vec3;

/** Source and normalized direction are mutable, as in the original scatter/plan API. */
public final class Trajectory {
    public Vec3 source;
    public Vec3 direction;
    public Trajectory(Vec3 source, Vec3 direction) { this.source = source; this.direction = direction; }
    public Trajectory copy() { return new Trajectory(source, direction); }
}
