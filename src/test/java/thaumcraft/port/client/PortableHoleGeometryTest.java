package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortableHoleGeometryTest {
    @Test void eachFaceMatchesTheOriginalOpenGlTransformSequence() {
        // Independent reference: the four UtilsFX.renderQuadCentered vertices,
        // transformed in TileHoleRenderer's original GL call order.
        var referenceQuad = List.of(new Vector3f(-.5f,.5f,0),new Vector3f(.5f,.5f,0),
                new Vector3f(.5f,-.5f,0),new Vector3f(-.5f,-.5f,0));
        for (var face : Direction.values()) {
            var transform = new Matrix4f().translate(.5f,.5f,.5f)
                    .rotate((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ())
                    .translate(0,0,face.getStepZ()<0?-.499f:.499f);
            if (face.getStepZ()<0) transform.rotateY((float)Math.PI);
            transform.rotateZ(-(float)Math.PI/2);
            var actual = capture(face,new Matrix4f());
            assertEquals(4,actual.size());
            for (int i=0;i<4;i++) assertTrue(transform.transformPosition(new Vector3f(referenceQuad.get(i)))
                    .distance(actual.get(i))<.000001f,face+" vertex "+i+" differs from BETA26");
        }
    }

    @Test void cullingExposesTheTunnelInteriorAndHidesTheSolidSide() {
        for (var face : Direction.values()) {
            var vertices=capture(face,new Matrix4f());
            var first=new Vector3f(vertices.get(1)).sub(vertices.get(0));
            var second=new Vector3f(vertices.get(2)).sub(vertices.get(0));
            var normal=first.cross(second).normalize();
            var outside=new Vector3f(face.getStepX(),face.getStepY(),face.getStepZ());
            assertEquals(-1,normal.dot(outside),.000001,face+" faces the solid block instead of the tunnel");
        }
    }

    @Test void quadsRetainTheBlockEntityPoseWithoutAddingTextureOrLightingData() {
        var pose=new Matrix4f().translate(7,-12,19).rotateY(.7f).rotateX(-.3f);
        for (var face : Direction.values()) {
            var original=capture(face,new Matrix4f());var transformed=capture(face,pose);
            for (int i=0;i<4;i++) assertTrue(pose.transformPosition(new Vector3f(original.get(i)))
                    .distance(transformed.get(i))<.000002f);
        }
    }

    private static List<Vector3f> capture(Direction face,Matrix4f pose) {
        var vertices=new ArrayList<Vector3f>();
        PortableHoleGeometry.emit(face,pose,new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x,float y,float z){vertices.add(new Vector3f(x,y,z));return this;}
            @Override public VertexConsumer setColor(int r,int g,int b,int a){fail("Hole shader computes its own colour");return this;}
            @Override public VertexConsumer setUv(float u,float v){fail("Hole shader projects its own UVs");return this;}
            @Override public VertexConsumer setUv1(int u,int v){fail("Hole has no overlay");return this;}
            @Override public VertexConsumer setUv2(int u,int v){fail("Hole is not lightmapped");return this;}
            @Override public VertexConsumer setNormal(float x,float y,float z){fail("Hole shader does not consume normals");return this;}
        });
        return vertices;
    }
}
