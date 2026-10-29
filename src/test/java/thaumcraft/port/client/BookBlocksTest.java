package thaumcraft.port.client;

import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookBlocksTest {
    private static final float EPSILON = .000001f;

    @Test void combinedQuarterTurnsRotateCornersAndQueryTheOriginalFace() {
        var top = quad(Direction.UP, new float[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}});
        var source = new StubModel(Map.of(Direction.UP, top));
        var oriented = BookBlocks.orient(source, 90, 90, false);

        var result = oriented.getQuads(null, Direction.EAST, RandomSource.create(0), ModelData.EMPTY, null);
        assertEquals(Direction.UP, source.lastSide, "The world EAST query must select the original UP face");
        assertEquals(1, result.size());
        assertEquals(Direction.EAST, result.getFirst().getDirection(), "AO must use the rotated face direction");
        // Legacy X90/Y90 is -Y then -X around the block center: (x,y,z) becomes (y,z,x).
        assertPositions(result.getFirst(), new float[][]{{1,0,0},{1,1,0},{1,1,1},{1,0,1}});
    }

    @Test void yawKeepsTheBlockCenterAndDoesNotMutateCachedSourceQuads() {
        var north = quad(Direction.NORTH, new float[][]{{0,0,0},{1,0,0},{1,1,0},{0,1,0}});
        var south = quad(Direction.SOUTH, new float[][]{{0,0,1},{0,1,1},{1,1,1},{1,0,1}});
        int[] beforeNorth = north.getVertices().clone(), beforeSouth = south.getVertices().clone();
        var source = new StubModel(Map.of(Direction.NORTH, north, Direction.SOUTH, south));
        var oriented = BookBlocks.orient(source, 0, 90, false);
        var east = oriented.getQuads(null, Direction.EAST, RandomSource.create(0)).getFirst();
        var west = oriented.getQuads(null, Direction.WEST, RandomSource.create(0)).getFirst();

        assertPositions(east, new float[][]{{1,0,0},{1,0,1},{1,1,1},{1,1,0}});
        assertEquals(Direction.WEST, west.getDirection());
        float[] center = new float[3];
        for (var face : List.of(east, west)) for (int vertex = 0; vertex < 4; vertex++)
            for (int axis = 0; axis < 3; axis++) center[axis] += coordinate(face, vertex, axis) / 8;
        assertArrayEquals(new float[]{.5f,.5f,.5f}, center, EPSILON,
                "Rotating about the origin would move the whole blueprint block");
        assertArrayEquals(beforeNorth, north.getVertices());
        assertArrayEquals(beforeSouth, south.getVertices());
        assertSame(east, oriented.getQuads(null, Direction.EAST, RandomSource.create(1)).getFirst());
        assertEquals(north.getTintIndex(), east.getTintIndex());
        assertEquals(north.isShade(), east.isShade());
        assertEquals(north.hasAmbientOcclusion(), east.hasAmbientOcclusion());
        for (int vertex = 0; vertex < 4; vertex++) for (int offset : new int[]{
                IQuadTransformer.COLOR, IQuadTransformer.UV0, IQuadTransformer.UV0 + 1, IQuadTransformer.UV2})
            assertEquals(beforeNorth[vertex * IQuadTransformer.STRIDE + offset],
                    east.getVertices()[vertex * IQuadTransformer.STRIDE + offset]);
    }

    @Test void golemObjGetsItsLegacyHorizontalAnchorWithoutVerticalShift() {
        var raw = quad(Direction.DOWN, new float[][]{{-.5f,0,-.5f},{.5f,0,-.5f},{.5f,0,.5f},{-.5f,0,.5f}});
        var source = new StubModel(Map.of(Direction.DOWN, raw));
        var oriented = BookBlocks.orient(source, 0, 0, true);
        var result = oriented.getQuads(null, Direction.DOWN, RandomSource.create(0)).getFirst();

        assertPositions(result, new float[][]{{0,0,0},{1,0,0},{1,0,1},{0,0,1}});
        assertEquals(Direction.DOWN, result.getDirection());
        assertEquals(Direction.DOWN, source.lastSide);
        assertPositions(raw, new float[][]{{-.5f,0,-.5f},{.5f,0,-.5f},{.5f,0,.5f},{-.5f,0,.5f}});
    }

    private static BakedQuad quad(Direction face, float[][] positions) {
        int[] data = new int[4 * IQuadTransformer.STRIDE];
        int normal = face.getStepX() * 127 & 255
                | (face.getStepY() * 127 & 255) << 8 | (face.getStepZ() * 127 & 255) << 16;
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * IQuadTransformer.STRIDE;
            for (int axis = 0; axis < 3; axis++)
                data[base + IQuadTransformer.POSITION + axis] = Float.floatToRawIntBits(positions[vertex][axis]);
            data[base + IQuadTransformer.COLOR] = 0xff21f9dd;
            data[base + IQuadTransformer.UV0] = Float.floatToRawIntBits(vertex * .25f);
            data[base + IQuadTransformer.UV0 + 1] = Float.floatToRawIntBits(vertex * .125f);
            data[base + IQuadTransformer.UV2] = 0x00e000b0;
            data[base + IQuadTransformer.NORMAL] = normal;
        }
        return new BakedQuad(data, 3, face, null, false, true);
    }

    private static float coordinate(BakedQuad quad, int vertex, int axis) {
        return Float.intBitsToFloat(quad.getVertices()[vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION + axis]);
    }

    private static void assertPositions(BakedQuad quad, float[][] expected) {
        for (int vertex = 0; vertex < 4; vertex++) for (int axis = 0; axis < 3; axis++)
            assertEquals(expected[vertex][axis], coordinate(quad, vertex, axis), EPSILON,
                    "Vertex " + vertex + ", axis " + axis);
    }

    /** Only the production quad transformer is exercised; no client, atlas or OpenGL context is needed. */
    private static final class StubModel extends BakedModelWrapper<BakedModel> {
        private final Map<Direction, BakedQuad> faces;
        private Direction lastSide;

        private StubModel(Map<Direction, BakedQuad> faces) { super(null); this.faces = faces; }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return getQuads(state, side, random, ModelData.EMPTY, null);
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                               ModelData data, RenderType type) {
            lastSide = side;
            return side == null ? List.copyOf(faces.values()) : faces.containsKey(side) ? List.of(faces.get(side)) : List.of();
        }
    }
}
