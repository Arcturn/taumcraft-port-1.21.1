package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixBlueprintRendererTest {
    private static final float EPSILON = .000002f;

    @Test void idlePreviewContainsEightSeparatedCubesWithTheOriginalCentersAndSize() {
        var output = capture(new PoseStack());
        assertEquals(8 * 6 * 4, output.vertices.size(), "Idle preview must omit glow and crafting halo geometry");
        for (int cube = 0; cube < 8; cube++) {
            var center = new Vector3f((cube & 4) == 0 ? -.25f : .25f,
                    (cube & 2) == 0 ? -.25f : .25f, (cube & 1) == 0 ? -.25f : .25f);
            var average = new Vector3f();
            for (Vertex vertex : output.vertices.subList(cube * 24, cube * 24 + 24)) {
                average.add(vertex.position);
                var distance = new Vector3f(vertex.position).sub(center);
                assertEquals(.225f, Math.abs(distance.x), EPSILON);
                assertEquals(.225f, Math.abs(distance.y), EPSILON);
                assertEquals(.225f, Math.abs(distance.z), EPSILON);
            }
            assertTrue(average.div(24).distance(center) < EPSILON);
        }
        var second = capture(new PoseStack());
        for (int i = 0; i < output.vertices.size(); i++)
            assertEquals(output.vertices.get(i).position, second.vertices.get(i).position,
                    "A fresh, unticked preview matrix must not depend on the world animation clock");
    }

    @Test void runeUvsAndPerCubeQuarterTurnsFollowTheOriginalModelCube() {
        var vertices = capture(new PoseStack()).vertices;
        // ModelCube is not mirrored: each entry is the first vertex of its DOWN UV face.
        assertPosition(vertices.get(0), -.025f, -.475f, -.025f);
        assertPosition(vertices.get(24), -.025f, -.025f, .475f); // Rz90
        assertPosition(vertices.get(48), -.025f, .025f, -.475f); // Ry90
        assertPosition(vertices.get(96), .475f, -.475f, -.475f); // Rx90
        assertPosition(vertices.get(168), .475f, .475f, .475f); // Rx90 * Ry90 * Rz90
        float[][] firstUvs = {{.5f,0},{.75f,.25f},{.25f,.25f},{.5f,.25f},{.75f,.25f},{1,.25f}};
        for (int cube = 0; cube < 8; cube++) for (int face = 0; face < 6; face++) {
            Vertex first = vertices.get(cube * 24 + face * 4);
            assertEquals(firstUvs[face][0], first.u);
            assertEquals(firstUvs[face][1], first.v);
        }
        assertTrue(vertices.stream().allMatch(v -> v.u >= 0 && v.u <= 1 && v.v >= 0 && v.v <= .5f),
                "The idle model only samples the upper half of infuser_normal; lower rows are the active overlay");
    }

    @Test void legacyLightsRotateWithThePreviewAndDoNotUseWorldLightOrAnExtraItemShade() {
        var ordinary = capture(new PoseStack());
        var turned = new PoseStack();
        turned.translate(15, 20, 7);
        turned.scale(4, 4, 4);
        turned.mulPose(com.mojang.math.Axis.XP.rotationDegrees(37));
        turned.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-61));
        Matrix4f before = new Matrix4f(turned.last().pose());
        var rotated = capture(turned);
        assertEquals(before, turned.last().pose(), "Rendering a cell must not change the caller's blueprint pose");
        assertEquals(102, ordinary.vertices.get(0).red, "DOWN has only the original .4 ambient term");
        assertEquals(255, ordinary.vertices.get(4).red, "UP receives both .6 directional lights and saturates");
        for (int i = 0; i < ordinary.vertices.size(); i++) {
            var a = ordinary.vertices.get(i);
            var b = rotated.vertices.get(i);
            assertEquals(a.red, b.red, "The original installs its lights after the outer view rotation");
            assertEquals(a.red, a.green);
            assertEquals(a.red, a.blue);
            assertEquals(255, a.alpha);
        }
    }

    private static Capture capture(PoseStack pose) {
        var output = new Capture();
        MatrixBlueprintRenderer.emit(pose, output, new Matrix4f());
        return output;
    }

    private static void assertPosition(Vertex vertex, float x, float y, float z) {
        assertEquals(x, vertex.position.x, EPSILON);
        assertEquals(y, vertex.position.y, EPSILON);
        assertEquals(z, vertex.position.z, EPSILON);
    }

    private static final class Vertex {
        private final Vector3f position;
        private int red, green, blue, alpha;
        private float u, v;
        private Vertex(float x, float y, float z) { position = new Vector3f(x, y, z); }
    }

    private static final class Capture implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex(x, y, z); vertices.add(current); return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            current.red = r; current.green = g; current.blue = b; current.alpha = a; return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { fail("An idle GUI matrix does not use an overlay texture"); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { fail("The legacy blueprint lightmap is disabled"); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { fail("Do not add a second entity diffuse-light pass"); return this; }
    }
}
