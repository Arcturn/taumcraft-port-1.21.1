package thaumcraft.port.infusion.brain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrainJarRendererTest {
    private static final float EPSILON=.000002f;
    @Test void worldBrainUsesAllThreeOriginalMeshesAndTheir128By64UvRegions(){
        var brain=new Capture();var brine=new Capture();
        BrainJarRenderer.emit(new PoseStack(),brain,()->{
            assertEquals(72,brain.vertices.size(),"Changing the texture buffer before the brain is complete would flush its unfinished batch");
            return brine;
        },0,0,0xA000B0,0);
        assertEquals(72,brain.vertices.size(),"ModelBrain has three six-face meshes, not the inventory flesh cube");
        assertEquals(24,brine.vertices.size());
        var cerebrum=brain.vertices.subList(0,24);
        bounds(cerebrum,new Vector3f(.3f,.36f,.35f),new Vector3f(.7f,.61f,.65f));
        bounds(brine.vertices,new Vector3f(.25f,.0725f,.25f),new Vector3f(.75f,.6975f,.75f));
        float[][] expected={{28/128f,0},{40/128f,16/64f},{16/128f,16/64f},{28/128f,16/64f},{44/128f,16/64f},{56/128f,16/64f}};
        for(int face=0;face<6;face++){
            assertEquals(expected[face][0],cerebrum.get(face*4).u,EPSILON);
            assertEquals(expected[face][1],cerebrum.get(face*4).v,EPSILON);
        }
        assertEquals(79/128f,brain.vertices.get(24).u,EPSILON);
        assertEquals(4/128f,brain.vertices.get(48).u,EPSILON);
        assertEquals(.5f,brain.vertices.get(48).v,EPSILON);
    }
    @Test void brainBobsAndTurnsInsideStationaryBrineWithoutChangingTheCallerPose(){
        var base=new Capture();var baseBrine=new Capture();var moved=new Capture();var movedBrine=new Capture();var pose=new PoseStack();
        var before=new Matrix4f(pose.last().pose());
        BrainJarRenderer.emit(pose,base,()->baseBrine,0,0,0,0);
        BrainJarRenderer.emit(pose,moved,()->movedBrine,3.141593f/2,.06f,0,0);
        assertEquals(before,pose.last().pose());
        for(int i=0;i<base.vertices.size();i++){
            var source=base.vertices.get(i).position;var rotated=moved.vertices.get(i).position;
            assertEquals(source.y-.06f,rotated.y,EPSILON);
            assertEquals(source.distance(new Vector3f(.5f,source.y,.5f)),rotated.distance(new Vector3f(.5f,rotated.y,.5f)),EPSILON);
            assertEquals(base.vertices.get(i).u,moved.vertices.get(i).u);
            assertEquals(base.vertices.get(i).v,moved.vertices.get(i).v);
        }
        assertTrue(base.vertices.get(0).position.distance(moved.vertices.get(0).position)>.1f);
        for(int i=0;i<baseBrine.vertices.size();i++)assertEquals(baseBrine.vertices.get(i).position,movedBrine.vertices.get(i).position);
    }
    private static void bounds(List<Vertex> vertices,Vector3f expectedMin,Vector3f expectedMax){
        var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
        for(var vertex:vertices){min.min(vertex.position);max.max(vertex.position);}
        assertEquals(expectedMin.x,min.x,EPSILON);assertEquals(expectedMin.y,min.y,EPSILON);assertEquals(expectedMin.z,min.z,EPSILON);
        assertEquals(expectedMax.x,max.x,EPSILON);assertEquals(expectedMax.y,max.y,EPSILON);assertEquals(expectedMax.z,max.z,EPSILON);
    }
    private static final class Vertex {
        final Vector3f position;float u,v;
        Vertex(float x,float y,float z){position=new Vector3f(x,y,z);}
    }
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();private Vertex current;
        @Override public VertexConsumer addVertex(float x,float y,float z){current=new Vertex(x,y,z);vertices.add(current);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        @Override public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
