package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTableGeometryTest {
    private static final float EPS = .00001f;

    @Test void quillRetainsBackwardExtrusionAndOriginalFlippedTextureCoordinates() {
        var capture = new Capture();
        ResearchTableGeometry.quill(new PoseStack(),capture,.1f,.3f,.2f,.6f,0xf000f0,0);
        assertEquals(66*4,capture.vertices.size());
        float[][] expected = {{0,0,0,.3f,.6f},{1,0,0,.1f,.6f},{1,1,0,.1f,.2f},{0,1,0,.3f,.2f},
                {0,1,-.0625f,.3f,.2f},{1,1,-.0625f,.1f,.2f},{1,0,-.0625f,.1f,.6f},{0,0,-.0625f,.3f,.6f}};
        for(int i=0;i<expected.length;i++) {
            var vertex=capture.vertices.get(i);var e=expected[i];
            assertEquals(e[0],vertex.position.x,EPS);assertEquals(e[1],vertex.position.y,EPS);
            assertEquals(e[2],vertex.position.z,EPS);assertEquals(e[3],vertex.u,EPS);assertEquals(e[4],vertex.v,EPS);
        }
        for(var vertex:capture.vertices) {
            assertTrue(vertex.position.z>=-.0625f&&vertex.position.z<=0);
            assertEquals(0xf000f0,vertex.light);
            assertArrayEquals(new int[]{255,255,255,255},vertex.color);
        }
        // Side strips sample pixel centres, not neighbouring transparent texels.
        assertEquals(.3f-.2f/32,capture.vertices.get(8).u,EPS);
        assertEquals(.6f-.4f/32,capture.vertices.get(16).v,EPS);
        for(int i=0;i<capture.vertices.size();i+=4) {
            var a=capture.vertices.get(i);var b=capture.vertices.get(i+1);var c=capture.vertices.get(i+2);
            var normal=new Vector3f(b.position).sub(a.position).cross(new Vector3f(c.position).sub(a.position)).normalize();
            assertTrue(normal.dot(a.normal)>.999f,"Every face must retain outward winding");
        }
    }

    @Test void opaqueNibPixelsSitInsideTheInkwellForAllFourFacings() {
        for(var facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
            var tablePose=new PoseStack();ResearchTableGeometry.tablePose(tablePose,facing);
            var centre=new Vector3f(-4.5f/16,-1/16f,4.5f/16).mulPosition(tablePose.last().pose());
            float expectedX=facing==Direction.NORTH||facing==Direction.WEST?.21875f:.78125f;
            float expectedZ=facing==Direction.NORTH||facing==Direction.EAST?.21875f:.78125f;
            assertEquals(expectedX,centre.x,EPS);assertEquals(1.0625f,centre.y,EPS);assertEquals(expectedZ,centre.z,EPS);
            var inverse=tablePose.last().pose().invert(new org.joml.Matrix4f());
            var quillPose=new PoseStack();ResearchTableGeometry.tablePose(quillPose,facing);ResearchTableGeometry.quillPose(quillPose);
            // Original texture's bottom row contains nib pixels at U=0 and U=1.
            // Because U is reversed, these are x=31/32 and x=29/32 in the mesh.
            for(float x:new float[]{31/32f,29/32f}) for(float z:new float[]{0,-1/16f}) {
                var tip=new Vector3f(x,1/32f,z).mulPosition(quillPose.last().pose()).mulPosition(inverse);
                assertTrue(tip.x>=-6/16f&&tip.x<=-3/16f,facing+" nib X outside inkwell");
                assertTrue(tip.y>=-2/16f&&tip.y<=0,facing+" nib Y outside inkwell");
                assertTrue(tip.z>=3/16f&&tip.z<=6/16f,facing+" nib Z outside inkwell");
            }
        }
    }

    private static final class Vertex {
        final Vector3f position;Vector3f normal;float u,v;int[] color;int light;
        Vertex(float x,float y,float z){position=new Vector3f(x,y,z);}
    }
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vertex current;
        public VertexConsumer addVertex(float x,float y,float z){current=new Vertex(x,y,z);vertices.add(current);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){current.color=new int[]{r,g,b,a};return this;}
        public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){current.light=u|v<<16;return this;}
        public VertexConsumer setNormal(float x,float y,float z){current.normal=new Vector3f(x,y,z);return this;}
    }
}
