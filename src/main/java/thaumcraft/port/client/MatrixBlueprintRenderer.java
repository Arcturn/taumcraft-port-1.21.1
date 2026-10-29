package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.EnumSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** The fresh, unticked TileInfusionMatrix rendered by TC6 GuiResearchPage.renderBluePrint. */
final class MatrixBlueprintRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "thaumcraft", "textures/blocks/infuser_normal.png");
    // ModelCube(0): addBox runs BEFORE mirror=true, so its already-created ModelBox is not mirrored.
    private static final ModelPart.Cube CUBE = new ModelPart.Cube(0, 0, -8, -8, -8,
            16, 16, 16, 0, 0, 0, false, 64, 64, EnumSet.allOf(Direction.class));
    private static final PoseStack.Pose IDENTITY = new PoseStack().last();
    private static final Vector3f LIGHT_0 = new Vector3f(.2f, 1, -.7f).normalize();
    private static final Vector3f LIGHT_1 = new Vector3f(-.2f, 1, .7f).normalize();
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    /** Called in BookBlocks' centered-cell pose, before its ordinary block-model branch. */
    static void render(GuiGraphics graphics) {
        graphics.flush();
        float[] color = RenderSystem.getShaderColor().clone();
        graphics.setColor(1, 1, 1, 1);
        try {
            emit(graphics.pose(), graphics.bufferSource().getBuffer(Layer.TYPE), RenderSystem.getModelViewMatrix());
            graphics.flush();
        } finally {
            graphics.setColor(color[0], color[1], color[2], color[3]);
        }
    }

    /** Production geometry path, also exercised without a client or OpenGL context by the mesh tests. */
    static void emit(PoseStack pose, VertexConsumer output, Matrix4f modelView) {
        Matrix4f base = pose.last().pose();
        Matrix4f lightPose = new Matrix4f(modelView).mul(base);
        // RenderHelper.enableStandardItemLighting is called under the blueprint view transform.
        Vector3f light0 = lightPose.transformDirection(LIGHT_0, new Vector3f()).normalize();
        Vector3f light1 = lightPose.transformDirection(LIGHT_1, new Vector3f()).normalize();
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) for (int c = 0; c < 2; c++) {
            Matrix4f cube = new Matrix4f(base).translate(a == 0 ? -.25f : .25f,
                    b == 0 ? -.25f : .25f, c == 0 ? -.25f : .25f);
            if (a != 0) cube.rotateX(QUARTER_TURN);
            if (b != 0) cube.rotateY(QUARTER_TURN);
            if (c != 0) cube.rotateZ(QUARTER_TURN);
            cube.scale(.45f);
            // ModelPart.Cube.compile already converts ModelCube's pixels to block units (/16).
            CUBE.compile(IDENTITY, new LitCube(output, cube, new Matrix4f(modelView).mul(cube), light0, light1),
                    0, 0, -1);
        }
    }

    private static final class LitCube implements VertexConsumer {
        private final VertexConsumer output;
        private final Matrix4f positions;
        private final Matrix3f normals;
        private final float rescale;
        private final Vector3f light0, light1;
        private final Vector3f position = new Vector3f();
        private final Vector3f normal = new Vector3f();
        private float u, v;

        private LitCube(VertexConsumer output, Matrix4f positions, Matrix4f eyePose,
                        Vector3f light0, Vector3f light1) {
            this.output = output;
            this.positions = positions;
            this.light0 = light0;
            this.light1 = light1;
            Matrix3f inverse = new Matrix3f(eyePose).invert();
            // OpenGL RESCALE_NORMAL, not NORMALIZE: inverse modelview's third ROW determines f.
            // OpenGL 2.1 specification, Normal Transformation (also glspec30 section 2.12).
            rescale = 1 / (float) Math.sqrt(inverse.m02() * inverse.m02()
                    + inverse.m12() * inverse.m12() + inverse.m22() * inverse.m22());
            normals = inverse.transpose();
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            positions.transformPosition(x, y, z, position);
            return this;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }

        @Override public VertexConsumer setNormal(float x, float y, float z) {
            normals.transform(x, y, z, normal).mul(rescale);
            float brightness = Math.min(1, .4f + .6f * (Math.max(0, normal.dot(light0)) + Math.max(0, normal.dot(light1))));
            int shade = Math.round(brightness * 255);
            output.addVertex(position.x, position.y, position.z).setColor(shade, shade, shade, 255).setUv(u, v);
            return this;
        }
    }

    /** No lightmap or entity/item diffuse shader: the original TESR lighting is already in the vertices. */
    private static final class Layer extends RenderType {
        private static final RenderType TYPE = create("thaumcraft_matrix_blueprint", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 8192, false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(TEXTURE, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));

        private Layer() { super("unused", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 0,
                false, false, () -> {}, () -> {}); }
    }

    private MatrixBlueprintRenderer() {}
}
