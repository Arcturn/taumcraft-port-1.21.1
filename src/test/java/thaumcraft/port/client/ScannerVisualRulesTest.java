package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScannerVisualRulesTest {
    private static final float EPS=0.00001f;

    @Test void activationUsesOriginalDispatcherOffsetsAndSmallEntityDurations() {
        assertEquals(new Vec3(2.5,3.75,4.5),ScannerVisualRules.blockRuneOrigin(new BlockPos(2,3,4)));
        assertEquals(0,new Vec3(2,4.3,4).distanceTo(ScannerVisualRules.entityRuneOrigin(new Vec3(2,3,4),1.6f)),0.000001);
        assertEquals(3,ScannerVisualRules.entityRuneDuration(.25f));
        assertEquals(27,ScannerVisualRules.entityRuneDuration(1.8f));
        assertEquals(10,ScannerVisualRules.RUNES_PER_USE);
        assertEquals(.5f,ScannerVisualRules.SCAN_VOLUME);
        assertEquals(1,ScannerVisualRules.SCAN_PITCH);
    }

    @Test void runeOpacityPreservesTheOriginalOneFifthRiseAndGravity() {
        assertEquals(0,ScannerVisualRules.runeOpacity(0,45));
        assertEquals(.5f,ScannerVisualRules.runeOpacity(9,45),EPS);
        assertEquals(35f/90,ScannerVisualRules.runeOpacity(10,45),EPS);
        assertEquals(.1f,ScannerVisualRules.runeOpacity(36,45),EPS);
        assertEquals(0,ScannerVisualRules.runeOpacity(45,45));
        assertEquals(0,ScannerVisualRules.runeOpacity(0,0));
        double velocity=0,position=0;
        for(int i=0;i<10;i++){velocity+=ScannerVisualRules.RUNE_ACCELERATION;position+=velocity;}
        assertEquals(-.066,position,0.000001);
    }

    @Test void highlightsRetainOriginalAgeFrameMixtureFlickerAndGrowingSize() {
        int stars=0,runes=0;
        var random=RandomSource.create(0x5478ee67L);
        for(int i=0;i<1200;i++) {
            var preset=ScannerVisualRules.sparkle(random,.4f);
            assertTrue(preset.lifetime()>=16&&preset.lifetime()<=19);
            assertTrue(preset.alphaKeys().length>=6&&preset.alphaKeys().length<=11);
            assertEquals(0,preset.alphaKeys()[0]);
            assertEquals(0,preset.alphaKeys()[preset.alphaKeys().length-1]);
            for(float key:preset.alphaKeys())assertTrue(key>=0&&key<1);
            assertEquals(.04f,preset.startHalfSize(),EPS);
            assertEquals(.08f,preset.endHalfSize(),EPS);
            if(preset.firstFrame()==320)stars++;else if(preset.firstFrame()==512)runes++;else fail("Unexpected legacy atlas strip");
        }
        assertTrue(stars>150&&stars<350,"The original preset selects sparks in about20% of cases");
        assertEquals(1200,stars+runes);
    }

    @Test void worldLabelsCenterEachFiveColumnRowAndKeepOriginalGrowth() {
        float scale=.25f;
        float[] x={-.5f,-.25f,0,.25f,.5f,-.125f,.125f};
        for(int i=0;i<x.length;i++) {
            assertEquals(x[i],ScannerVisualRules.tagHorizontalOffset(i,7,scale),EPS);
            assertEquals(i<5?0:.2625f,ScannerVisualRules.tagVerticalOffset(i,scale),EPS);
        }
        assertEquals(.031f,ScannerVisualRules.nextTagScale(0,true),EPS);
        assertEquals(.0544f,ScannerVisualRules.nextTagScale(.031f,true),EPS);
        assertEquals(.245f,ScannerVisualRules.nextTagScale(.25f,false),EPS);
        float grown=0;for(int i=0;i<200;i++)grown=ScannerVisualRules.nextTagScale(grown,true);
        assertEquals(.265f,grown,EPS);
    }

    @Test void runeQuadsFaceOutwardOnAllFourSidesAndKeepOriginalTextureOrientation() {
        for(int quarter=0;quarter<4;quarter++) {
            var rotation=new Quaternionf().rotateY(quarter*(float)Math.PI/2).rotateZ((float)Math.PI/2);
            var capture=new Capture();
            ScanRuneParticle.emitQuad(capture,rotation,2,3,4,.15f,.1f,.2f,.3f,.4f,.8f,0,.6f,.25f);
            assertEquals(4,capture.vertices.size());
            var outward=new Vector3f(0,0,-1).rotate(rotation);
            var a=new Vector3f(capture.vertices.get(1).position).sub(capture.vertices.get(0).position);
            var b=new Vector3f(capture.vertices.get(2).position).sub(capture.vertices.get(0).position);
            assertTrue(a.cross(b).normalize().dot(outward)>.999f,"Runes must face away from their block, not into it");
            float[][] uv={{.2f,.4f},{.2f,.3f},{.1f,.3f},{.1f,.4f}};
            var center=new Vector3f();
            for(int i=0;i<4;i++) {
                var vertex=capture.vertices.get(i);center.add(vertex.position);
                assertEquals(uv[i][0],vertex.u,EPS);assertEquals(uv[i][1],vertex.v,EPS);
                assertArrayEquals(new int[]{204,0,153,63},vertex.color);
                assertEquals(0x00F00000,vertex.light);
            }
            assertTrue(center.div(4).distance(new Vector3f(2,3,4))<EPS);
        }
    }

    private static final class Vertex {
        final Vector3f position;float u,v;int[] color;int light;
        Vertex(float x,float y,float z){position=new Vector3f(x,y,z);}
    }
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vertex current;
        public VertexConsumer addVertex(float x,float y,float z){current=new Vertex(x,y,z);vertices.add(current);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){current.color=new int[]{r,g,b,a};return this;}
        public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){current.light=u|v<<16;return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
