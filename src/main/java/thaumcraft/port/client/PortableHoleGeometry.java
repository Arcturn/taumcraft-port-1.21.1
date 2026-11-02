package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/** TileHoleRenderer's transformed unit quads, facing into the traversable tunnel. */
final class PortableHoleGeometry {
    private static final float NEAR = .001f;
    private static final float FAR = .999f;

    static void emit(Direction face, Matrix4f pose, VertexConsumer output) {
        // These are the positions after the original center translation, face
        // rotation, +/- .499 offset and final -90-degree Z rotation. Their
        // winding matters: ordinary back-face culling hides the solid side.
        float[][] points = switch (face) {
            case DOWN -> new float[][]{{1,NEAR,1},{1,NEAR,0},{0,NEAR,0},{0,NEAR,1}};
            case UP -> new float[][]{{1,FAR,0},{1,FAR,1},{0,FAR,1},{0,FAR,0}};
            case NORTH -> new float[][]{{0,0,NEAR},{1,0,NEAR},{1,1,NEAR},{0,1,NEAR}};
            case SOUTH -> new float[][]{{1,0,FAR},{0,0,FAR},{0,1,FAR},{1,1,FAR}};
            case WEST -> new float[][]{{NEAR,1,1},{NEAR,0,1},{NEAR,0,0},{NEAR,1,0}};
            case EAST -> new float[][]{{FAR,1,0},{FAR,0,0},{FAR,0,1},{FAR,1,1}};
        };
        for (var point : points) output.addVertex(pose, point[0], point[1], point[2]);
    }

    private PortableHoleGeometry() { }
}
