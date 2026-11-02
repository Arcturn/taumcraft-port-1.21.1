package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import thaumcraft.port.client.SalisMundusParticle;
import static org.junit.jupiter.api.Assertions.*;

class GolemParticleRenderingTest {
    @Test void emotionBirthFrameIsInvisibleAndNonAdditiveSpritesRetainSourceAlpha() throws Exception {
        var p=GolemParticle.emote(null,Vec3.ZERO,Vec3.ZERO,704,3,6,2,1,1,1);
        assertTrue(render(p,0).vertices.isEmpty());assertSame(SalisMundusParticle.legacyLayer(false),p.getRenderType());
        for(int age=1;age<=6;age++){
            setAge(p,age);var out=render(p,0);assertEquals(4,out.vertices.size());assertEquals(704+Math.min(2,age/2),cell(out,64));
            for(var v:out.vertices){assertEquals(127,v.alpha);assertEquals(255,v.red);assertEquals(.2f,Math.abs(v.x),1e-6);assertEquals(.2f,Math.abs(v.y),1e-6);}
        }
    }
    @Test void originalOrbBurstUsesSixteenGridAndAdvancesAllThirtyOneFrames() throws Exception {
        var p=GolemParticle.burst(null,Vec3.ZERO);assertSame(SalisMundusParticle.legacyLayer(true),p.getRenderType());
        for(int age=1;age<=31;age++){setAge(p,age);var out=render(p,0);assertEquals(208+Math.min(30,age),cell(out,16));
            float u0=out.vertices.stream().map(v->v.u).min(Float::compare).orElseThrow(),u1=out.vertices.stream().map(v->v.u).max(Float::compare).orElseThrow();assertEquals(1f/16,u1-u0,1e-6);}
    }
    private static void setAge(Particle p,int age)throws Exception{var f=Particle.class.getDeclaredField("age");f.setAccessible(true);f.setInt(p,age);}
    private static Capture render(GolemParticle p,float partial){var out=new Capture();p.renderFrame(out,new Quaternionf(),Vec3.ZERO,partial,0xf000a0);return out;}
    private static int cell(Capture out,int grid){float u=1,v=1;for(var a:out.vertices){u=Math.min(u,a.u);v=Math.min(v,a.v);}return Math.round(v*grid)*grid+Math.round(u*grid);}
    private static final class Vertex {float x,y,u,v;int red,alpha;}
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vertex current;
        @Override public VertexConsumer addVertex(float x,float y,float z){current=new Vertex();current.x=x;current.y=y;vertices.add(current);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){current.red=r;current.alpha=a;return this;}
        @Override public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
