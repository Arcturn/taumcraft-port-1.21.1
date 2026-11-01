package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** TC6's extruded bauble sprite, in the humanoid head's local coordinates. */
final class GogglesGeometry {
    private static final int COLUMNS = 16, ROWS = 26;
    private static final float THICKNESS = .1f;

    static void emit(PoseStack pose, VertexConsumer out, int light) {
        pose.pushPose();
        // Baubles HEAD + translateToHeadLevel/Face/defaultTransforms, at neutral pose.
        // TC6 tests HEAD != null, which is always true for a 1.12 ItemStack, even EMPTY.
        // Preserve the resulting .12 * .55 clearance instead of intersecting a helmet.
        pose.translate(-.275f, .005f, -.336f);
        pose.scale(.55f, -.55f, -.55f);
        quad(pose, out, light, 0, 0, 1,
                0,0,0,0,1, 1,0,0,1,1, 1,1,0,1,0, 0,1,0,0,0);
        quad(pose, out, light, 0, 0, -1,
                0,1,-THICKNESS,0,0, 1,1,-THICKNESS,1,0,
                1,0,-THICKNESS,1,1, 0,0,-THICKNESS,0,1);
        for (int column = 0; column < COLUMNS; column++) {
            float x = column / (float) COLUMNS, next = (column + 1f) / COLUMNS;
            float u = (column + .5f) / COLUMNS;
            quad(pose, out, light, -1, 0, 0,
                    x,0,-THICKNESS,u,1, x,0,0,u,1, x,1,0,u,0, x,1,-THICKNESS,u,0);
            quad(pose, out, light, 1, 0, 0,
                    next,1,-THICKNESS,u,0, next,1,0,u,0,
                    next,0,0,u,1, next,0,-THICKNESS,u,1);
        }
        for (int row = 0; row < ROWS; row++) {
            float y = row / (float) ROWS, next = (row + 1f) / ROWS;
            float v = 1 - (row + .5f) / ROWS;
            quad(pose, out, light, 0, 1, 0,
                    0,next,0,0,v, 1,next,0,1,v,
                    1,next,-THICKNESS,1,v, 0,next,-THICKNESS,0,v);
            quad(pose, out, light, 0, -1, 0,
                    1,y,0,1,v, 0,y,0,0,v, 0,y,-THICKNESS,0,v, 1,y,-THICKNESS,1,v);
        }
        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer out, int light,
                             float nx, float ny, float nz, float... vertices) {
        for (int i = 0; i < vertices.length; i += 5)
            out.addVertex(pose.last().pose(), vertices[i], vertices[i + 1], vertices[i + 2])
                    .setColor(-1).setUv(vertices[i + 3], vertices[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
    }

    private GogglesGeometry() {}
}
