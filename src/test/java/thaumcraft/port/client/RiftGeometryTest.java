package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiftGeometryTest {
    @Test void revealingExpandsRadiiWithoutMovingCenterlineOrEndpoints() {
        var points = List.of(new Vec3(3, 1, -2), new Vec3(3, 3, -2), new Vec3(4, 5, 1));
        var widths = List.of(0f, .4f, 0f);
        var ordinary = capture(points, widths, new Matrix4f(), 1);
        var reveal = capture(points, widths, new Matrix4f(), 1.25f);
        assertEquals(64, ordinary.size());
        assertEquals(ordinary.size(), reveal.size());
        for (int index = 0; index < reveal.size(); index++) {
            int segment = index / 32;
            boolean end = index % 4 >= 2;
            var center = points.get(segment + (end ? 1 : 0));
            float radius = widths.get(segment + (end ? 1 : 0));
            var c = new Vector3f((float) center.x, (float) center.y, (float) center.z);
            assertEquals(radius, ordinary.get(index).distance(c), .000002f);
            assertEquals(radius * 1.25f, reveal.get(index).distance(c), .000002f);
            if (radius == 0) assertEquals(ordinary.get(index), reveal.get(index));
        }
    }

    @Test void theSharedMeshHonorsEntityPoseAndHasNoTextureOrLightingAttributes() {
        var points = List.of(Vec3.ZERO, new Vec3(1, 0, 0));
        var widths = List.of(.2f, .3f);
        var transform = new Matrix4f().translate(10, 20, 30).rotateY(.7f);
        var ordinary = capture(points, widths, new Matrix4f(), 1);
        var moved = capture(points, widths, transform, 1);
        for (int i = 0; i < ordinary.size(); i++)
            assertTrue(transform.transformPosition(new Vector3f(ordinary.get(i))).distance(moved.get(i)) < .000004f);
    }

    private static List<Vector3f> capture(List<Vec3> points, List<Float> widths, Matrix4f transform, float scale) {
        var capture = new ArrayList<Vector3f>();
        RiftGeometry.emit(points, widths, transform, new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { capture.add(new Vector3f(x, y, z)); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { fail("Portal shader expects positions only"); return this; }
            @Override public VertexConsumer setUv(float u, float v) { fail("Portal shader projects its own UVs"); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { fail("Portal shader has no overlay"); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { fail("Portal shader is not lightmapped"); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { fail("Portal shader has no normals"); return this; }
        }, scale);
        return capture;
    }
}
