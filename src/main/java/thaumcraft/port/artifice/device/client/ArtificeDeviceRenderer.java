package thaumcraft.port.artifice.device.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.port.artifice.device.*;
import thaumcraft.port.client.SalisMundusParticle;

public final class ArtificeDeviceRenderer implements BlockEntityRenderer<ArtificeDeviceEntity> {
    public ArtificeDeviceRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ArtificeDeviceEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        switch(be.kind()){
            case DIOPTRA->dioptra(be,partial,pose,buffers);
            case PATTERN_CRAFTER->pattern(be,partial,pose,buffers,light);
            case REDSTONE_RELAY->relay(be,pose,buffers);
            case ARCANE_EAR,ARCANE_EAR_TOGGLE->{var mc=Minecraft.getInstance();if(mc.player!=null&&!mc.options.hideGui&&thaumcraft.port.content.RevealingEquipment.hasGoggles(mc.player)&&mc.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(be.getBlockPos()))label(be,pose,buffers,"Note: "+be.note,1.5);}
            default->{}
        }
    }
    @Override public AABB getRenderBoundingBox(ArtificeDeviceEntity be){return new AABB(be.getBlockPos()).inflate(.3).expandTowards(0,1,0);}
    private static void dioptra(ArtificeDeviceEntity be,float partial,PoseStack pose,MultiBufferSource buffers){
        var camera=Minecraft.getInstance().getCameraEntity();float t=(camera==null?0:camera.tickCount)+partial;
        float r=((float)Math.sin(t/12)*.05f+.85f)*.8f,g=(float)Math.sin(t/11)*.05f+(be.enabled()?.9f:.45f),b=(float)Math.sin(t/10)*.05f+.95f;
        pose.pushPose();pose.translate(.005,1.001,.005);pose.scale(.99f,1,.99f);var out=buffers.getBuffer(Types.GRID);
        for(int x=0;x<12;x++)for(int z=0;z<12;z++){
            float x0=x/12f,x1=(x+1)/12f,z0=z/12f,z1=(z+1)/12f;
            float h00=be.grid[x+z*13]/96f,h10=be.grid[x+1+z*13]/96f,h11=be.grid[x+1+(z+1)*13]/96f,h01=be.grid[x+(z+1)*13]/96f;
            int brightness=(int)(200+Math.sin(-Math.hypot(x-6,z-6)*10/8)*15);
            quad(out,pose,new float[][]{{x0,h01,z1},{x1,h11,z1},{x1,h10,z0},{x0,h00,z0}},0,0,1,1,r,g,b,new float[]{.9f,.9f,.9f,.9f},brightness);
            if(x==0)quad(out,pose,new float[][]{{x0,0,z0},{x0,h00,z0},{x0,h01,z1},{x0,0,z1}},0,0,1,1,r,g,b,new float[]{0,.9f,.9f,0},brightness);
            if(x==11)quad(out,pose,new float[][]{{x1,0,z1},{x1,h11,z1},{x1,h10,z0},{x1,0,z0}},0,0,1,1,r,g,b,new float[]{0,.9f,.9f,0},brightness);
            if(z==0)quad(out,pose,new float[][]{{x1,0,z0},{x1,h10,z0},{x0,h00,z0},{x0,0,z0}},0,0,1,1,r,g,b,new float[]{0,.9f,.9f,0},brightness);
            if(z==11)quad(out,pose,new float[][]{{x0,0,z1},{x0,h01,z1},{x1,h11,z1},{x1,0,z1}},0,0,1,1,r,g,b,new float[]{0,.9f,.9f,0},brightness);
        }
        pose.popPose();pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.ZP.rotationDegrees(270));out=buffers.getBuffer(Types.SIDE);
        for(int side=0;side<4;side++){pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(90*side));pose.translate(0,0,-.5);centered(out,pose,1,0,0,1,1,r/.8f,g,b,.8f,220);pose.popPose();}pose.popPose();
    }
    private static void pattern(ArtificeDeviceEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(.5,.75,.5);float angle=switch(be.facing()){case EAST->90;case WEST->270;case NORTH->180;default->0;};pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(0,0,-.5001);centered(buffers.getBuffer(Types.MODES),pose,.5f,be.pattern/10f,be.pattern,(be.pattern+1)/10f,be.pattern+1,1,1,1,1,light);pose.popPose();
        for(int sign:new int[]{-1,1}){
            pose.pushPose();pose.translate(sign*.2,-.40625,.05);pose.mulPose(Axis.ZP.rotationDegrees(sign*(be.rotation+be.rotationSpeed*partial)));pose.scale(.5f,.5f,1);
            var out=buffers.getBuffer(Types.GEAR);plane(out,pose,1,1,0,0,1,1,1,1,1,light);pose.translate(0,0,-.1);plane(out,pose,1,1,0,0,1,1,1,1,1,light);
            for(int i=0;i<16;i++){
                float q=i/16f-.5f,q1=(i+1)/16f-.5f,uv=(i+.5f)/16;
                for(float edge:new float[]{q,q1}){
                    quadUv(out,pose,new float[][]{{edge,-.5f,0},{edge,-.5f,.1f},{edge,.5f,.1f},{edge,.5f,0}},new float[][]{{1-uv,1},{1-uv,1},{1-uv,0},{1-uv,0}},1,1,1,new float[]{1,1,1,1},light);
                    quadUv(out,pose,new float[][]{{-.5f,edge,.1f},{.5f,edge,.1f},{.5f,edge,0},{-.5f,edge,0}},new float[][]{{1,1-uv},{0,1-uv},{0,1-uv},{1,1-uv}},1,1,1,new float[]{1,1,1,1},light);
                }
            }pose.popPose();
        }pose.popPose();
    }
    private static void relay(ArtificeDeviceEntity be,PoseStack pose,MultiBufferSource buffers){
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.hitResult instanceof BlockHitResult hit)||!hit.getBlockPos().equals(be.getBlockPos()))return;
        var block=(ArtificeDeviceBlock)be.getBlockState().getBlock();int knob=block.knobAt(be.getBlockState(),be.getBlockPos(),mc.player);if(knob<0)return;
        String text=(knob==0?"Out: ":"In: ")+(knob==0?be.output:be.input);label(be,pose,buffers,text,.8);
    }
    private static void label(ArtificeDeviceEntity be,PoseStack pose,MultiBufferSource buffers,String text,double height){
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera().getPosition();
        float yaw=(float)Math.toDegrees(Math.atan2(camera.x-be.getBlockPos().getX()-.5,camera.z-be.getBlockPos().getZ()-.5));
        pose.pushPose();pose.translate(.5,height,.5);pose.mulPose(Axis.YP.rotationDegrees(yaw+180));pose.mulPose(Axis.ZP.rotationDegrees(180));pose.scale(.0125f,.0125f,.0125f);
        mc.font.drawInBatch(text,1-mc.font.width(text)/2f,1,0xffffffff,true,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private static void plane(VertexConsumer out,PoseStack pose,float size,float u0,float v0,float u1,float v1,float r,float g,float b,float alpha,int light){float s=size/2;quad(out,pose,new float[][]{{-s,-s,0},{s,-s,0},{s,s,0},{-s,s,0}},u0,v1,u1,v0,r,g,b,new float[]{alpha,alpha,alpha,alpha},light);}
    /** UtilsFX.renderQuadCentered rotates texture coordinates around the face. */
    private static void centered(VertexConsumer out,PoseStack pose,float size,float u0,float v0,float u1,float v1,float r,float g,float b,float alpha,int light){float s=size/2;quadUv(out,pose,new float[][]{{-s,s,0},{s,s,0},{s,-s,0},{-s,-s,0}},new float[][]{{u1,v1},{u1,v0},{u0,v0},{u0,v1}},r,g,b,new float[]{alpha,alpha,alpha,alpha},light);}
    private static void quad(VertexConsumer out,PoseStack pose,float[][] vertices,float u0,float v0,float u1,float v1,float r,float g,float b,float[] alpha,int light){
        quadUv(out,pose,vertices,new float[][]{{u0,v0},{u1,v0},{u1,v1},{u0,v1}},r,g,b,alpha,light);
    }
    private static void quadUv(VertexConsumer out,PoseStack pose,float[][] vertices,float[][] uv,float r,float g,float b,float[] alpha,int light){
        for(int i=0;i<4;i++)out.addVertex(pose.last().pose(),vertices[i][0],vertices[i][1],vertices[i][2]).setUv(uv[i][0],uv[i][1]).setColor(r,g,b,alpha[i]).setLight(light);
    }
    private static final class Types extends RenderType {
        static final RenderType GRID=type("misc/gridblock",true),SIDE=type("models/dioptra_side",true),GEAR=type("misc/gear_brass",false),MODES=type("blocks/pattern_crafter_modes",false);
        static RenderType type(String texture,boolean additive){return create("thaumcraft_device_"+texture,DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,8192,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/"+texture+".png"),false,false))
                .setTransparencyState(additive?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(additive?COLOR_WRITE:COLOR_DEPTH_WRITE).createCompositeState(false));}
        Types(String n,VertexFormat f,VertexFormat.Mode m,int b,boolean c,boolean s,Runnable start,Runnable end){super(n,f,m,b,c,s,start,end);}
    }
}
