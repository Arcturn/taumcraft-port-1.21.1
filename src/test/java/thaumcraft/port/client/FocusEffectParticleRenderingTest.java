package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Inspects emitted vertices from the production renderer rather than checking source text. */
class FocusEffectParticleRenderingTest {
    private static final String[] KEYS={"AIR","EARTH","FIRE","FROST","HEAL","FLUX","CURSE","BREAK","EXCHANGE","RIFT"};
    @Test void noEffectFlashesItsConstructorFrameBeforeTheOriginalFirstUpdate() throws Exception {
        for(String key:KEYS){var particle=create(key);set(Particle.class,particle,"age",0);set(FocusEffectParticle.class,particle,"delay",0);assertTrue(render(particle,.5f).vertices.isEmpty(),key+" rendered the source's still-uninitialized alpha/scale frame");}
    }
    @Test void allTenEffectsEmitVisibleCorrectAtlasCellsAndCameraFacingQuads() throws Exception {
        // BETA26 atlas regions. Ranges include randomized first cells and frame animation.
        int[][] atlas={{32,337,341},{64,75,78},{64,640,649},{64,8,8},{64,0,0},{64,128,141},{16,72,75},{64,704,715},{64,448,456},{64,384,399}};
        for(int index=0;index<KEYS.length;index++){
            var particle=create(KEYS[index]);set(FocusEffectParticle.class,particle,"delay",0);
            int age=Math.max(1,integer(Particle.class,particle,"lifetime")/2);set(Particle.class,particle,"age",age);
            var capture=render(particle,.25f);assertEquals(4,capture.vertices.size(),KEYS[index]);
            var first=capture.vertices.get(0);int grid=atlas[index][0];
            float minU=Float.MAX_VALUE,maxU=0,minV=Float.MAX_VALUE,maxV=0;
            for(var vertex:capture.vertices){minU=Math.min(minU,vertex.u);maxU=Math.max(maxU,vertex.u);minV=Math.min(minV,vertex.v);maxV=Math.max(maxV,vertex.v);assertTrue(vertex.alpha>0,KEYS[index]+" emitted a fully transparent midlife quad");assertEquals(0x00F000A0,vertex.light);}
            assertEquals(1f/grid,maxU-minU,1e-6,KEYS[index]+" wrong atlas grid width");assertEquals(1f/grid,maxV-minV,1e-6,KEYS[index]+" wrong atlas grid height");
            int cell=Math.round(minV*grid)*grid+Math.round(minU*grid);assertTrue(cell>=atlas[index][1]&&cell<=atlas[index][2],KEYS[index]+" sampled atlas cell "+cell);
            var normal=new Vector3f(capture.vertices.get(1).position).sub(first.position).cross(new Vector3f(capture.vertices.get(2).position).sub(first.position)).normalize();
            assertTrue(normal.dot(new Vector3f(0,0,1).rotate(orientation()))>.999,KEYS[index]+" quad faces away from the camera");
        }
    }
    @Test void animatedFramesAdvanceAndFluxLoopsWithoutBleedingToTheNextSpriteRow() throws Exception {
        var fire=create("FIRE");set(FocusEffectParticle.class,fire,"delay",0);
        for(int age=1;age<=10;age++){set(Particle.class,fire,"age",age);assertEquals(640+Math.min(9,age),cell(render(fire,0),64));}
        var flux=create("FLUX");set(FocusEffectParticle.class,flux,"delay",0);
        set(Particle.class,flux,"age",13);assertEquals(141,cell(render(flux,0),64));
        set(Particle.class,flux,"age",14);assertEquals(128,cell(render(flux,0),64));
    }
    @Test void frostAndBreakRetainOriginalFadeToBlackWhileFireStaysWhite() throws Exception {
        for(String key:new String[]{"FROST","BREAK","FIRE"}){
            var particle=create(key);set(FocusEffectParticle.class,particle,"delay",0);int lifetime=integer(Particle.class,particle,"lifetime");set(Particle.class,particle,"age",lifetime/2);
            var vertex=render(particle,.25f).vertices.get(0);int expected=key.equals("FIRE")?255:(int)(255*(1-(lifetime/2+.25f)/lifetime));
            assertEquals(expected,vertex.red,key);assertEquals(expected,vertex.green,key);assertEquals(expected,vertex.blue,key);
        }
    }
    @Test void delayedParticlesStayInvisibleAndEarthUsesTheNonAdditiveLayer() throws Exception {
        var delayed=create("CURSE");set(Particle.class,delayed,"age",2);set(FocusEffectParticle.class,delayed,"delay",1);assertTrue(render(delayed,0).vertices.isEmpty());
        assertSame(SalisMundusParticle.legacyLayer(false),create("EARTH").getRenderType());
        for(String key:KEYS)if(!key.equals("EARTH"))assertSame(SalisMundusParticle.legacyLayer(true),create(key).getRenderType());
    }
    private static FocusEffectParticle create(String key){return FocusEffectParticle.effect("thaumcraft."+key,null,new Vec3(2,3,4),Vec3.ZERO,RandomSource.create(58));}
    private static Quaternionf orientation(){return new Quaternionf().rotationYXZ(.7f,-.4f,0);}
    private static Capture render(FocusEffectParticle particle,float partial){var capture=new Capture();particle.renderFrame(capture,orientation(),Vec3.ZERO,partial,0x00F000A0);return capture;}
    private static int cell(Capture capture,int grid){float u=Float.MAX_VALUE,v=Float.MAX_VALUE;for(var vertex:capture.vertices){u=Math.min(u,vertex.u);v=Math.min(v,vertex.v);}return Math.round(v*grid)*grid+Math.round(u*grid);}
    private static int integer(Class<?> owner,Object object,String name)throws Exception{Field field=owner.getDeclaredField(name);field.setAccessible(true);return field.getInt(object);}
    private static void set(Class<?> owner,Object object,String name,int value)throws Exception{Field field=owner.getDeclaredField(name);field.setAccessible(true);field.setInt(object,value);}
    private static final class Vertex {final Vector3f position;float u,v;int red,green,blue,alpha,light;Vertex(float x,float y,float z){position=new Vector3f(x,y,z);}}
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vertex current;
        @Override public VertexConsumer addVertex(float x,float y,float z){current=new Vertex(x,y,z);vertices.add(current);return this;}
        @Override public VertexConsumer setColor(int red,int green,int blue,int alpha){current.red=red;current.green=green;current.blue=blue;current.alpha=alpha;return this;}
        @Override public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){current.light=u|v<<16;return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
