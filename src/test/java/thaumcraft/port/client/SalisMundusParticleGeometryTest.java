package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SalisMundusParticleGeometryTest {
    private static final float EPSILON = 0.0001f;

    @Test
    void emittedTrianglesFaceTheCameraAtDifferentAnglesAndRolls() {
        for (float yaw : new float[]{0, 1.2f, 3.14f}) {
            for (float pitch : new float[]{-.8f, 0, .6f}) {
                var camera = new Quaternionf().rotationYXZ(yaw, pitch, 0);
                Vector3f towardCamera = new Vector3f(0, 0, 1).rotate(camera);
                for (float roll : new float[]{0, .9f, 2.7f}) {
                    var output = emit(new Quaternionf(camera).rotateZ(roll));
                    assertEquals(4, output.vertices.size());
                    Vector3f origin = output.vertices.get(0).position;
                    for (int[] triangle : new int[][]{{1, 2}, {2, 3}}) {
                        Vector3f edgeA = new Vector3f(output.vertices.get(triangle[0]).position).sub(origin);
                        Vector3f edgeB = new Vector3f(output.vertices.get(triangle[1]).position).sub(origin);
                        float facing = edgeA.cross(edgeB).normalize().dot(towardCamera);
                        assertTrue(facing > .999f,
                                "Production particle quad faces away from camera; back-face culling would hide it");
                    }
                }
            }
        }
    }

    @Test
    void textureCornersRemainAlignedAndKeepVisibleColourAndLight() {
        var orientation = new Quaternionf().rotationYXZ(.4f, -.7f, 1.1f);
        var output = emit(orientation);
        var inverse = new Quaternionf(orientation).conjugate();
        var center = new Vector3f(2, 3, -4);
        var sum = new Vector3f();
        for (Vertex vertex : output.vertices) {
            sum.add(vertex.position);
            var local = new Vector3f(vertex.position).sub(center).rotate(inverse).div(.75f);
            assertEquals(1, Math.abs(local.x), EPSILON);
            assertEquals(1, Math.abs(local.y), EPSILON);
            assertEquals(0, local.z, EPSILON);
            assertEquals(local.x > 0 ? .35f : .2f, vertex.u, EPSILON);
            assertEquals(local.y > 0 ? .4f : .6f, vertex.v, EPSILON);
            assertArrayEquals(new int[]{255, 127, 63, 191}, vertex.colour);
            assertEquals(0x00F000A0, vertex.light);
        }
        assertTrue(sum.div(4).distance(center) < EPSILON, "Quad should remain centered on the particle");
    }

    private static Capture emit(Quaternionf orientation) {
        var output = new Capture();
        // Invoke the production emission path called by SalisMundusParticle.render.
        SalisMundusParticle.renderQuad(output, orientation, 2, 3, -4, .75f,
                .2f, .35f, .4f, .6f, 1, .5f, .25f, .75f, 0x00F000A0);
        return output;
    }

    private static final class Vertex {
        private final Vector3f position;
        private float u, v;
        private int[] colour;
        private int light;
        private Vertex(float x, float y, float z) { position = new Vector3f(x, y, z); }
    }

    private static final class Capture implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex(x, y, z);
            vertices.add(current);
            return this;
        }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            current.colour = new int[]{red, green, blue, alpha};
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { current.light = u | v << 16; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
