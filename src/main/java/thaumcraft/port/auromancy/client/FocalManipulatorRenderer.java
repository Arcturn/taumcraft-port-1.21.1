package thaumcraft.port.auromancy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.auromancy.focal.FocalManipulatorEntity;
import thaumcraft.port.client.BookAssets;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.content.CrystalItem;

/** The original hovering focus, orbiting vis crystals, billboards and seeded light fans. */
public final class FocalManipulatorRenderer implements BlockEntityRenderer<FocalManipulatorEntity> {
    private static final ResourceLocation PARTICLES=BookAssets.id("textures/misc/particles.png");
    public FocalManipulatorRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public void render(FocalManipulatorEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(table.getLevel()==null)return;
        var game=Minecraft.getInstance();
        int age=game.getCameraEntity()==null?(int)table.getLevel().getGameTime():game.getCameraEntity().tickCount;
        float ticks=age+partial;
        if(!table.getItem(0).isEmpty()) {
            pose.pushPose();pose.translate(.5,.8,.5);pose.mulPose(Axis.YP.rotationDegrees(ticks%360));
            groundItem(table,table.getItem(0),Mth.sin(ticks/14)*.2f+.2f,pose,buffers,light,overlay);
            pose.popPose();
        }
        if(table.crystalsSync.isEmpty())return;
        int step=360/table.crystalsSync.size(),index=0;
        for(var aspect:table.crystalsSync.keySet()) {
            float angle=ticks%720/2+step*index,bob=Mth.sin((ticks+index*10)/12)*.02f+.02f;
            int color=aspect.color();float red=((color>>16)&255)/255f,green=((color>>8)&255)/255f,blue=(color&255)/255f;
            pose.pushPose();pose.translate(.5,1.3,.5);pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(0,bob,.4);pose.mulPose(Axis.YP.rotationDegrees(-angle));
            var camera=game.gameRenderer.getMainCamera();pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            int frame=320+age%16;float u=frame%64/64f,v=frame/64/64f,s=1/64f;
            var glow=buffers.getBuffer(Layers.GLOW);
            glow.addVertex(pose.last().pose(),-.175f,-.175f,0).setColor(red,green,blue,.66f).setUv(u+s,v+s).setLight(LightTexture.FULL_BRIGHT);
            glow.addVertex(pose.last().pose(),-.175f,.175f,0).setColor(red,green,blue,.66f).setUv(u+s,v).setLight(LightTexture.FULL_BRIGHT);
            glow.addVertex(pose.last().pose(),.175f,.175f,0).setColor(red,green,blue,.66f).setUv(u,v).setLight(LightTexture.FULL_BRIGHT);
            glow.addVertex(pose.last().pose(),.175f,-.175f,0).setColor(red,green,blue,.66f).setUv(u,v+s).setLight(LightTexture.FULL_BRIGHT);
            pose.popPose();
            pose.pushPose();pose.translate(.5,1.05,.5);pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(0,bob,.4);pose.scale(.5f,.5f,.5f);
            ray(pose,buffers,angle,index,bob,red,green,blue,ticks);
            ray(pose,buffers,angle,(index+1)*5,bob,red,green,blue,ticks);
            groundItem(table,CrystalItem.stack(aspect),0,pose,buffers,light,overlay);
            pose.popPose();index++;
        }
    }
    private static void groundItem(FocalManipulatorEntity table,ItemStack stack,float phase,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var renderer=Minecraft.getInstance().getItemRenderer();var model=renderer.getModel(stack,table.getLevel(),null,0);
        pose.translate(0,Mth.sin(phase)*.1f+.1f+model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y()*.25f,0);
        pose.mulPose(Axis.YP.rotation(phase));
        renderer.renderStatic(stack.copyWithCount(1),ItemDisplayContext.GROUND,light,overlay,pose,buffers,table.getLevel(),0);
    }
    private static void ray(PoseStack pose,MultiBufferSource buffers,float angle,int number,float lift,float red,float green,float blue,float ticks) {
        var random=new Random(187L+number*number);
        float pan=Mth.sin((ticks+number*10)/15)*15,aperture=Mth.sin((ticks+number*10)/14)*2;
        pose.pushPose();pose.translate(0,.475f+lift,0);pose.mulPose(Axis.XP.rotationDegrees(-90));pose.mulPose(Axis.YP.rotationDegrees(angle));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.XP.rotationDegrees(pan));
        float amount=Math.min(ticks,10)/300,length=(random.nextFloat()*20+10)*amount,width=(random.nextFloat()*4+6+aperture)*amount;
        float[][] corners={{-.8f*width,length,-.5f*width},{.8f*width,length,-.5f*width},{0,length,width}};
        var ray=buffers.getBuffer(Layers.RAYS);
        for(int side=0;side<3;side++) {
            ray.addVertex(pose.last().pose(),0,0,0).setColor(red,green,blue,.66f);
            for(int vertex:new int[]{side,(side+1)%3})ray.addVertex(pose.last().pose(),corners[vertex][0],corners[vertex][1],corners[vertex][2]).setColor(red,green,blue,0);
        }
        pose.popPose();
    }
    @Override public AABB getRenderBoundingBox(FocalManipulatorEntity table){return new AABB(table.getBlockPos()).expandTowards(0,.7,0).inflate(.15);}
    private static final class Layers extends RenderType {
        static final RenderType GLOW=create("thaumcraft_focal_glow",DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,1536,false,false,
                CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader)).setTextureState(new TextureStateShard(PARTICLES,true,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        static final RenderType RAYS=create("thaumcraft_focal_rays",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,1536,false,false,
                CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private Layers(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
}
