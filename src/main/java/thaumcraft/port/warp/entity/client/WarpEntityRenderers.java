package thaumcraft.port.warp.entity.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.warp.entity.*;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class WarpEntityRenderers {
    private static final ModelLayerLocation GUARDIAN=new ModelLayerLocation(id("eldritch_guardian"),"main");
    private static final ResourceLocation GUARDIAN_TEXTURE=id("textures/entity/eldritch_guardian.png");
    private static final ResourceLocation SPIDER_TEXTURE=ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");
    private static final ResourceLocation PARTICLES=id("textures/misc/particles.png");
    private static final RenderType GHOST_SPIDER=WarpRenderTypes.ghost(SPIDER_TEXTURE);
    private static final RenderType ORB=WarpRenderTypes.ghost(PARTICLES);
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(GUARDIAN,EldritchGuardianModel::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(WarpEntities.ELDRITCH_GUARDIAN.get(),GuardianRenderer::new);
        event.registerEntityRenderer(WarpEntities.MIND_SPIDER.get(),MindSpiderRenderer::new);
        event.registerEntityRenderer(WarpEntities.ELDRITCH_ORB.get(),OrbRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        WarpEntityVisuals.guardianWisp=WarpEntityParticles::wisp;
        WarpEntityVisuals.sonic=WarpEntityParticles::sonic;
    });}
    private static final class GuardianRenderer extends MobRenderer<EldritchGuardian,EldritchGuardianModel>{
        GuardianRenderer(EntityRendererProvider.Context context){super(context,new EldritchGuardianModel(context.bakeLayer(GUARDIAN)),.5f);}
        @Override public ResourceLocation getTextureLocation(EldritchGuardian entity){return GUARDIAN_TEXTURE;}
        @Override protected RenderType getRenderType(EldritchGuardian entity,boolean visible,boolean translucent,boolean glowing){return RenderType.entityTranslucent(GUARDIAN_TEXTURE);}
        @Override public void render(EldritchGuardian entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var camera=Minecraft.getInstance().getCameraEntity();
            double distance=camera==null?0:entity.distanceToSqr(camera);
            double far=entity.level().getDifficulty()==Difficulty.HARD?576:1024;
            model.opacity=(float)(.6*(1-Math.clamp((distance-256)/(far-256),0,1)));
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private static final class GhostSpiderModel extends SpiderModel<MindSpider>{
        float opacity;
        GhostSpiderModel(net.minecraft.client.model.geom.ModelPart root){super(root);}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){super.renderToBuffer(pose,buffer,light,overlay,((int)(255*opacity)<<24)|(color&0xffffff));}
    }
    private static final class MindSpiderRenderer extends MobRenderer<MindSpider,GhostSpiderModel>{
        MindSpiderRenderer(EntityRendererProvider.Context context){super(context,new GhostSpiderModel(context.bakeLayer(ModelLayers.SPIDER)),.5f);addLayer(new SpiderEyesLayer<>(this));}
        @Override public boolean shouldRender(MindSpider entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){
            var player=Minecraft.getInstance().player;return player!=null&&entity.visibleTo(player.getGameProfile().getName())&&super.shouldRender(entity,frustum,x,y,z);
        }
        @Override public ResourceLocation getTextureLocation(MindSpider entity){return SPIDER_TEXTURE;}
        @Override protected RenderType getRenderType(MindSpider entity,boolean visible,boolean translucent,boolean glowing){return GHOST_SPIDER;}
        @Override protected void scale(MindSpider entity,PoseStack pose,float partial){pose.scale(.6f,.6f,.6f);}
        @Override protected float getFlipDegrees(MindSpider entity){return 180;}
        @Override public void render(MindSpider entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var player=Minecraft.getInstance().player;
            if(player==null||!entity.visibleTo(player.getGameProfile().getName()))return;
            model.opacity=Math.min(.1f,entity.tickCount/100f);
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private static final class OrbRenderer extends EntityRenderer<EldritchOrb>{
        OrbRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(EldritchOrb entity){return PARTICLES;}
        @Override public void render(EldritchOrb entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var random=new Random(187);var rays=buffers.getBuffer(WarpRenderTypes.RAYS);
            pose.pushPose();
            for(int i=0;i<12;i++){
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360+entity.tickCount/80f*360));
                float growth=Math.min(entity.tickCount,10)/300f;
                float length=(random.nextFloat()*20+5)*growth,width=(random.nextFloat()*2+1)*growth;
                float[][] points={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
                for(int j=0;j<3;j++){
                    rays.addVertex(pose.last().pose(),0,0,0).setColor(255,255,255,255);
                    for(int k:new int[]{j,(j+1)%3})rays.addVertex(pose.last().pose(),points[k][0],points[k][1],points[k][2]).setColor(64,64,64,255);
                }
            }
            pose.popPose();pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float u=(entity.tickCount%13)/64f,v=3/64f;
            var out=buffers.getBuffer(ORB);
            float[][] corners={{-.375f,-.375f,u,v+1/64f},{.375f,-.375f,u+1/64f,v+1/64f},{.375f,.375f,u+1/64f,v},{-.375f,.375f,u,v}};
            for(var c:corners)out.addVertex(pose.last().pose(),c[0],c[1],0).setColor(-1).setUv(c[2],c[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose.last(),0,0,1);
            pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
}
