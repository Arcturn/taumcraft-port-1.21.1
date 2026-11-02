package thaumcraft.port.client;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Minecraft 1.12 Particle.move order, without 1.21's permanent vertical-collision stop. */
public final class FocusParticleMotion {
    public static Vec3 collide(AABB bounds,Vec3 requested,List<VoxelShape> obstacles) {
        double y=Shapes.collide(Direction.Axis.Y,bounds,obstacles,requested.y);
        bounds=bounds.move(0,y,0);
        double x=Shapes.collide(Direction.Axis.X,bounds,obstacles,requested.x);
        bounds=bounds.move(x,0,0);
        double z=Shapes.collide(Direction.Axis.Z,bounds,obstacles,requested.z);
        return new Vec3(x,y,z);
    }
    private FocusParticleMotion() { }
}
