package thaumcraft.port.artifice.mirror.client;

import java.util.Random;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import thaumcraft.port.artifice.mirror.MirrorEntity;
import thaumcraft.port.client.SalisMundusParticle;

/** Original extruded frame and 16 independently projected, seeded tunnel/particle layers. */
public final class MirrorRenderer implements BlockEntityRenderer<MirrorEntity> {
    private static ResourceLocation texture(String path){return ResourceLocation.parse("thaumcraft:textures/"+path+".png");}
    private static final RenderType FRAME=Layers.layer("mirror_frame",texture("blocks/mirrorframe"),false),FRAME_ESSENTIA=Layers.layer("mirror_essentia_frame",texture("blocks/mirrorframe2"),false);
    private static final RenderType CLOSED=Layers.layer("mirror_closed",texture("blocks/mirrorpane"),false),OPEN=Layers.layer("mirror_open",texture("blocks/mirrorpanetrans"),false);
    private static final RenderType TUNNEL=Layers.layer("mirror_tunnel",texture("misc/tunnel"),false),FIELD=Layers.layer("mirror_field",texture("misc/particlefield"),true);
    public MirrorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(MirrorEntity mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();var pos=mirror.getBlockPos();
        boolean open=mirror.linked&&camera.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<1024;
        pose.pushPose();orient(pose,mirror.facing());pose.translate(0,0,-.01);
        frame(pose,buffers.getBuffer(mirror.essentia()?FRAME_ESSENTIA:FRAME),light);
        pose.popPose();
        if(open)portal(mirror,pose,buffers,camera.x-pos.getX(),camera.y-pos.getY(),camera.z-pos.getZ());
        pose.pushPose();orient(pose,mirror.facing());pose.translate(0,0,-.02);pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(.5,-.5,0);
        quad(pose,buffers.getBuffer(open?OPEN:CLOSED),light,1,1,1,-.5f,-.5f,.5f,.5f,0,0,1,1);
        pose.popPose();
    }
    static void orient(PoseStack pose,Direction facing){switch(facing){
        case DOWN->{pose.translate(0,1,1);pose.mulPose(Axis.XP.rotationDegrees(-90));}
        case UP->pose.mulPose(Axis.XP.rotationDegrees(90));
        case NORTH->pose.translate(0,0,1);
        case SOUTH->{pose.translate(1,0,0);pose.mulPose(Axis.YP.rotationDegrees(180));}
        case WEST->{pose.translate(1,0,1);pose.mulPose(Axis.YP.rotationDegrees(90));}
        case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));
    }}
    private static void frame(PoseStack pose,VertexConsumer out,int light){
        quad(pose,out,light,1,1,1,0,0,1,1,1,1,0,0);
        pose.pushPose();pose.translate(0,0,-.0625);quad(pose,out,light,1,1,1,0,0,1,1,1,1,0,0);pose.popPose();
        for(int pixel=0;pixel<16;pixel++){
            float edge=pixel/16f,next=(pixel+1)/16f,u=1-(pixel+.5f)/16f,v=1-(pixel+.5f)/16f;
            vertices(pose,out,light,1,1,1,new float[][]{{edge,0,-.0625f,u,1},{edge,0,0,u,1},{edge,1,0,u,0},{edge,1,-.0625f,u,0}});
            vertices(pose,out,light,1,1,1,new float[][]{{next,1,-.0625f,u,0},{next,1,0,u,0},{next,0,0,u,1},{next,0,-.0625f,u,1}});
            vertices(pose,out,light,1,1,1,new float[][]{{0,next,0,1,v},{1,next,0,0,v},{1,next,-.0625f,0,v},{0,next,-.0625f,1,v}});
            vertices(pose,out,light,1,1,1,new float[][]{{1,edge,0,0,v},{0,edge,0,1,v},{0,edge,-.0625f,1,v},{1,edge,-.0625f,0,v}});
        }
    }
    private static void portal(MirrorEntity mirror,PoseStack pose,MultiBufferSource buffers,double cx,double cy,double cz){
        var local=new PoseStack();orient(local,mirror.facing());local.translate(0,0,-.01);
        var inverse=new org.joml.Matrix4f(local.last().pose()).invert();var eye=inverse.transformPosition(new Vector3f((float)cx,(float)cy,(float)cz));
        pose.pushPose();orient(pose,mirror.facing());pose.translate(0,0,-.01);
        var random=new Random(31100);float scroll=System.currentTimeMillis()%700000L/250000f;
        for(int i=0;i<16;i++){
            float distance=i==0?65:16-i,scale=i==0?.125f:i==1?.5f:.0625f,brightness=i==0?.1f:1f/(17-i);
            float red=random.nextFloat()*.5f+.1f,green=random.nextFloat()*.5f+.4f,blue=random.nextFloat()*.5f+.5f;if(i==0)red=green=blue=1;
            float angle=(float)Math.toRadians((i*i*4321+i*9)*2f),cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
            float depth=Math.max(.001f,Math.abs(eye.z));var out=buffers.getBuffer(i==0?TUNNEL:FIELD);
            for(float[] vertex:new float[][]{{.1875f,.8125f},{.1875f,.1875f},{.8125f,.1875f},{.8125f,.8125f}}){
                float u=vertex[0]+(vertex[0]-eye.x)*distance/depth,v=vertex[1]+(vertex[1]-eye.y)*distance/depth;
                float a=u-.5f,b=v-.5f;u=(a*cos-b*sin+.5f)*scale;v=(a*sin+b*cos+.5f)*scale+scroll;
                out.addVertex(pose.last().pose(),vertex[0],vertex[1],0).setUv(u,v).setColor(red*brightness,green*brightness,blue*brightness,1).setLight(LightTexture.FULL_BRIGHT);
            }
        }
        pose.popPose();
    }
    private static void quad(PoseStack pose,VertexConsumer out,int light,float r,float g,float b,float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1){vertices(pose,out,light,r,g,b,new float[][]{{x0,y0,0,u0,v0},{x1,y0,0,u1,v0},{x1,y1,0,u1,v1},{x0,y1,0,u0,v1}});}
    private static void vertices(PoseStack pose,VertexConsumer out,int light,float r,float g,float b,float[][] vertices){for(var v:vertices)out.addVertex(pose.last().pose(),v[0],v[1],v[2]).setUv(v[3],v[4]).setColor(r,g,b,1).setLight(light);}
    private static final class Layers extends RenderType {
        private static final TransparencyStateShard ADD=new TransparencyStateShard("mirror_add",()->{RenderSystem.enableBlend();RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ONE);},()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();});
        static RenderType layer(String name,ResourceLocation texture,boolean additive){return create("thaumcraft_"+name,DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,4096,false,false,CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader)).setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(additive?ADD:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false));}
        private Layers(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
}
