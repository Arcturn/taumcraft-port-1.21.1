package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GolemObjModelTest {
    // SHA-256 of BETA26 OBJ text after line-ending/trailing-space normalization only.
    private static final Map<String,String> ORIGINAL = Map.ofEntries(
            Map.entry("golem_armor.obj", "6b62912ac3cdaa68990e29cd018ee499ac421d6325460ae9d8dd53749e5f668d"),
            Map.entry("golem_arms_basic.obj", "11d51de23b9eedd522d386e303165ce3b04e4d72fa2c62cfefe5135cbc515bc3"),
            Map.entry("golem_arms_breakers.obj", "8a8cd550ea22b58076b73ce4091a014aad082cc5a8e6e032ba835cda9d16eb2e"),
            Map.entry("golem_arms_claws.obj", "0c1c8a72a4568b55ec5ece41e7b5e2a15c74eb092547c52f27e5b325803b4e48"),
            Map.entry("golem_arms_darter.obj", "64b4767e59a47e2837e2fc6d82871f44ef1b62dd6f01969b02ac24499262a950"),
            Map.entry("golem_arms_fine.obj", "cb0750af2f0a43d035177251badbe8d7f5c07c1bac76d4ef393e6acc0d4d9aff"),
            Map.entry("golem_base.obj", "fdee7d2ed52e17092abd50597145f1b973a65f7a1bea9a7397556d3d5daaca9f"),
            Map.entry("golem_hauler.obj", "2809b4bf8d5024e2eaca1dcec88baa8d3de4ea3eeb18a7ec9154a3a07addd8ce"),
            Map.entry("golem_head_basic.obj", "638549aae5cce394ef775482850d60a4b03dc1f29b7e18a565af1e4950a0ef9a"),
            Map.entry("golem_head_scout.obj", "d3bd9cb38198f2ba4730cf1a95a86961f82d515591aa1d67aa1014476e3dd46e"),
            Map.entry("golem_head_scout_smart.obj", "e6ca604905be64c16945a8e85350d62bf6ef01960a52de3151f88e3dbe54ede6"),
            Map.entry("golem_head_smart.obj", "4558d8023ea1bb8484983d11d891408022c828414e076fd7e52cad150d50c45d"),
            Map.entry("golem_head_smart_armor.obj", "e420b7f344cae065c7d3acd3cc5d964b452a3db7f22f0c75c2b76e6b1433c205"),
            Map.entry("golem_legs_climber.obj", "55e705bd4068264a3b2a76b0512f84562f5ba320482cdb9476c4ab0c4dcf3a34"),
            Map.entry("golem_legs_floater.obj", "6fcb3bca6f4a93337c1b88b18d4235494e4660408dcf0712be7c03c7df583599"),
            Map.entry("golem_legs_walker.obj", "791d1105419fdcd06f60f6215f6a6601f1aae8012dc00b35805b8c8083ac8840"),
            Map.entry("golem_legs_wheel.obj", "a46f98f2006cb7a5dd0e048ecdbbf49acb06761e1871616805d61b52a7bf5936"));
    @Test void everyOriginalPartRetainsItsCompleteGeometryAndLoadsEveryObjectGroup() throws Exception {
        assertEquals(17,ORIGINAL.size());
        for(var entry:ORIGINAL.entrySet()){
            byte[] bytes;try(var stream=getClass().getResourceAsStream("/assets/thaumcraft/models/obj/"+entry.getKey())){assertNotNull(stream,entry.getKey());bytes=stream.readAllBytes();}
            String canonical=String.join("\n",new String(bytes,java.nio.charset.StandardCharsets.UTF_8).lines().map(String::stripTrailing).toList()).stripTrailing()+"\n";
            assertEquals(entry.getValue(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8))),entry.getKey());
            var model=new GolemObjModel(new StringReader(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)),true);
            assertFalse(model.groups().isEmpty(),entry.getKey());
            for(var group:model.groups()){
                var capture=new Capture();model.render(group,new PoseStack(),capture,0xf000a0,OverlayTexture.NO_OVERLAY,-1);
                assertFalse(capture.vertices.isEmpty(),entry.getKey()+":"+group);assertEquals(0,capture.vertices.size()%4);
                for(var v:capture.vertices){assertTrue(Float.isFinite(v.x)&&Float.isFinite(v.y)&&Float.isFinite(v.z));assertTrue(Float.isFinite(v.u)&&Float.isFinite(v.v));assertEquals(0xf000a0,v.light);}
            }
        }
    }
    @Test void uvInsetNormalWindingAndOverlaySurviveActualVertexEmission() throws Exception {
        String obj="""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0 0
                vt 1 0
                vt 0 1
                g bm_face
                f 1/1 2/2 3/3
                """;
        for(boolean flip:new boolean[]{false,true}){
            var model=new GolemObjModel(new StringReader(obj),flip);var pose=new PoseStack();pose.translate(2,3,4);
            var capture=new Capture();model.render("bm_face",pose,capture,0x00e000c0,0x00010002,0x804080ff);
            assertEquals(4,capture.vertices.size());var a=capture.vertices.getFirst();
            assertEquals(2,a.x);assertEquals(3,a.y);assertEquals(4,a.z);assertEquals(.0005f,a.u,1e-6);assertEquals(flip?.9995f:.0005f,a.v,1e-6);
            assertEquals(0,a.nx);assertEquals(0,a.ny);assertEquals(1,a.nz);assertEquals(0x00010002,a.overlay);assertEquals(0x00e000c0,a.light);
            assertArrayEquals(new int[]{64,128,255,128},a.color);
            assertEquals(capture.vertices.get(2).x,capture.vertices.get(3).x);assertEquals(capture.vertices.get(2).y,capture.vertices.get(3).y);
        }
    }
    private static final class Vertex {float x,y,z,u,v,nx,ny,nz;int light,overlay;int[] color;}
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();Vertex current;
        @Override public VertexConsumer addVertex(float x,float y,float z){current=new Vertex();current.x=x;current.y=y;current.z=z;vertices.add(current);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){current.color=new int[]{r,g,b,a};return this;}
        @Override public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){current.overlay=u|v<<16;return this;}
        @Override public VertexConsumer setUv2(int u,int v){current.light=u|v<<16;return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
    }
}
