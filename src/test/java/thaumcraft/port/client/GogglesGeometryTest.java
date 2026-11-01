package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GogglesGeometryTest {
    private static final float EPSILON = .000002f;

    @Test void baubleRetainsOriginalFaceClearanceThicknessAndUprightUvs() {
        var vertices = capture(new PoseStack()).vertices;
        // Baubles' original HEAD/face transforms and TC6's permanent armor clearance.
        assertPosition(vertices.get(0), -.275f, .005f, -.336f);
        assertPosition(vertices.get(2), .275f, -.545f, -.336f);
        assertPosition(vertices.get(4), -.275f, -.545f, -.281f);
        assertEquals(0, vertices.get(0).u);
        assertEquals(1, vertices.get(0).v);
        assertEquals(1, vertices.get(2).u);
        assertEquals(0, vertices.get(2).v);
        assertEquals(-1, vertices.get(0).normal.z, EPSILON);
        assertEquals(1, vertices.get(4).normal.z, EPSILON);
        assertTrue(vertices.stream().allMatch(v -> v.light == 0x00A00070));
    }

    @Test void extrusionUsesOriginalSixteenByTwentySixPixelSlices() {
        var vertices = capture(new PoseStack()).vertices;
        assertEquals((2 + 2 * 16 + 2 * 26) * 4, vertices.size(),
                "Sides must keep the TC6 16x26 subdivisions, even though the texture is 16x16");
        for (int column = 0; column < 16; column++) {
            var left = vertices.get(8 + column * 8);
            var right = vertices.get(12 + column * 8);
            assertEquals((column + .5f) / 16, left.u, EPSILON);
            assertEquals(left.u, right.u);
            assertEquals(.55f / 16, right.position.x - left.position.x, EPSILON);
        }
        for (int row = 0; row < 26; row++) {
            var up = vertices.get(8 + 16 * 8 + row * 8);
            var down = vertices.get(12 + 16 * 8 + row * 8);
            assertEquals(1 - (row + .5f) / 26, up.v, EPSILON);
            assertEquals(up.v, down.v);
            assertEquals(.55f / 26, down.position.y - up.position.y, EPSILON);
        }
    }

    @Test void suppliedHeadPoseMovesTheWholeMeshAndIsRestored() {
        var neutral = capture(new PoseStack());
        var head = new PoseStack();
        head.translate(1.5, -.3, .4);
        head.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45));
        head.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-20));
        var before = new Matrix4f(head.last().pose());
        var turned = capture(head);
        assertEquals(before, head.last().pose());
        for (int i = 0; i < neutral.vertices.size(); i++) {
            var expected = before.transformPosition(new Vector3f(neutral.vertices.get(i).position));
            assertTrue(expected.distance(turned.vertices.get(i).position) < EPSILON);
        }
    }

    private static Capture capture(PoseStack pose) {
        var capture = new Capture();
        GogglesGeometry.emit(pose, capture, 0x00A00070);
        return capture;
    }

    private static void assertPosition(Vertex vertex, float x, float y, float z) {
        assertEquals(x, vertex.position.x, EPSILON);
        assertEquals(y, vertex.position.y, EPSILON);
        assertEquals(z, vertex.position.z, EPSILON);
    }

    private static final class Vertex {
        final Vector3f position;
        final Vector3f normal = new Vector3f();
        float u, v;
        int light;
        Vertex(float x, float y, float z) { position = new Vector3f(x, y, z); }
    }

    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        Vertex current;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex(x, y, z); vertices.add(current); return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { current.light = u | v << 16; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { current.normal.set(x, y, z); return this; }
    }
}
