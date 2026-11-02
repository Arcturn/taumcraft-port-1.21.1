package thaumcraft.port.auromancy.client.media;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FocusBoltGeometryTest {
    @Test
    void angledRingsMatchTheOriginalCoreGleMath() {
        var mesh=FocusBoltGeometry.tube(List.of(node(0,0,0),node(.4,.2,.1),
                node(.6,.5,-.1),node(.8,.9,.3),node(1.2,1.2,.6)),1);
        assertEquals(2,mesh.size(),"The original first/last points guide the joins rather than extend the visible tube");
        // Golden vertices evaluated with matrix/intersect from the BETA26 JAR,
        // using CoreGLE.extrusion_angle_join's original coordinate transforms.
        double[][] first={
                {.407148281525,.215700922014,.204864711022},
                {.336524201136,.277713504026,.115983792982},
                {.353621517314,.232328664859,.005013816310},
                {.434812320217,.142266709668,.025311517036},
                {.467893679807,.131990199434,.148826162650}};
        double[][] last={
                {.768745175325,.847468894673,.379503608972},
                {.712875463870,.941285312879,.333902018764},
                {.777408900092,.978046831923,.241448990912},
                {.873162468544,.906950281964,.229911467544},
                {.867807992169,.826248678562,.315233913809}};
        for(int i=0;i<5;i++){
            assertPoint(first[i],mesh.getFirst().front()[i]);
            assertPoint(last[i],mesh.getLast().back()[i]);
            assertTrue(mesh.getFirst().back()[i].distanceTo(mesh.getLast().front()[i])<1e-10,
                    "Adjacent mitered rings must meet without cracks or overlapping separate cylinders");
        }
    }

    @Test
    void verticalTubeAndInnerCoreKeepTheSourceRadiusRatio() {
        var nodes=List.of(node(0,0,0),node(0,1,0),node(0,2,0),node(0,3,0));
        var outer=FocusBoltGeometry.tube(nodes,1).getFirst();
        var inner=FocusBoltGeometry.tube(nodes,1.0/3).getFirst();
        for(int i=0;i<5;i++){
            assertEquals(.1,outer.front()[i].distanceTo(new Vec3(0,1,0)),1e-10);
            assertEquals(.1/3,inner.front()[i].distanceTo(new Vec3(0,1,0)),1e-10);
            assertEquals(1,outer.front()[i].y,1e-10);
            assertEquals(2,outer.back()[i].y,1e-10);
        }
    }

    @Test
    void guidePointsAloneDoNotProduceAVisibleTube() {
        assertTrue(FocusBoltGeometry.tube(List.of(node(0,0,0),node(0,1,0),node(0,2,0)),1).isEmpty());
    }

    private static FocusBoltGeometry.Node node(double x,double y,double z){return new FocusBoltGeometry.Node(new Vec3(x,y,z),.1);}
    private static void assertPoint(double[] expected,Vec3 actual){assertArrayEquals(expected,new double[]{actual.x,actual.y,actual.z},1e-10);}
}
