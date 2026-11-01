package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InfusionMatrixRendererTest {
    @Test void longInfusionsKeepTheOriginalUnsignedByteHaloCycle() {
        // BETA26 supplies these alpha integers to BufferBuilder.color; its BYTE
        // vertex format stores (byte) alpha instead of clamping to [0,255].
        int[] ticks = {0, 1, 50, 250, 500, 501, 502, 750, 1000, 1500};
        int[] alpha = {255, 254, 229, 127, 0, 0, 255, 129, 1, 2};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(alpha[i], InfusionMatrixRenderer.haloAlpha(ticks[i]), "Ritual tick " + ticks[i]);
            var output = new Capture();
            InfusionMatrixRenderer.emitHalo(new PoseStack(), output, ticks[i], 10);
            assertEquals(90, output.vertices.size());
            for (int n = 0; n < output.vertices.size(); n += 3) {
                assertEquals(alpha[i], output.vertices.get(n).alpha);
                assertEquals(0, output.vertices.get(n + 1).alpha);
                assertEquals(0, output.vertices.get(n + 2).alpha);
            }
        }
    }

    @Test void haloPreservesItsOriginSeedAndCallerPoseAtBothGraphicsQualities() {
        for (int quality : new int[]{10, 20}) {
            var pose = new PoseStack();
            pose.translate(3, 4, -2);
            var before = new Matrix4f(pose.last().pose());
            var first = new Capture();
            var second = new Capture();
            InfusionMatrixRenderer.emitHalo(pose, first, 750, quality);
            InfusionMatrixRenderer.emitHalo(pose, second, 750, quality);
            assertEquals(before, pose.last().pose());
            assertEquals(quality * 9, first.vertices.size());
            for (int i = 0; i < first.vertices.size(); i++) {
                var a = first.vertices.get(i);
                assertEquals(a.position, second.vertices.get(i).position);
                assertEquals(a.alpha, second.vertices.get(i).alpha);
                if (i % 3 == 0) assertEquals(new Vector3f(3.5f, 4.5f, -1.5f), a.position);
                else assertTrue(a.position.distance(new Vector3f(3.5f, 4.5f, -1.5f)) < 1.3f);
            }
        }
    }

    private static final class Vertex {
        final Vector3f position;
        int alpha;
        Vertex(float x, float y, float z) { position = new Vector3f(x, y, z); }
    }

    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        private Vertex current;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex(x, y, z); vertices.add(current); return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { current.alpha = a; return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
