package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Shared centerline: revealing the rift enlarges its radius, never its position. */
final class RiftGeometry {
    static void emit(List<Vec3> points, List<Float> widths, Matrix4f matrix,
                     VertexConsumer out, float radiusScale) {
        for (int i = 0; i < points.size() - 1; i++) {
            var a = points.get(i);
            var b = points.get(i + 1);
            var axis = b.subtract(a).normalize();
            Vec3 u = axis.cross(Math.abs(axis.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            Vec3 v = axis.cross(u).normalize();
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4, next = (k + 1) * Math.PI / 4;
                Vec3 p = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                Vec3 q = u.scale(Math.cos(next)).add(v.scale(Math.sin(next)));
                for (Vec3 point : new Vec3[]{a.add(p.scale(widths.get(i) * radiusScale)),
                        a.add(q.scale(widths.get(i) * radiusScale)),
                        b.add(q.scale(widths.get(i + 1) * radiusScale)),
                        b.add(p.scale(widths.get(i + 1) * radiusScale))})
                    out.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z);
            }
        }
    }

    private RiftGeometry() {}
}
