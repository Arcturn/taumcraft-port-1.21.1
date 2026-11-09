package thaumcraft.port.boss.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import thaumcraft.port.boss.*;
import thaumcraft.port.mob.Taintacle;
import thaumcraft.port.mob.client.TaintacleModel;
import thaumcraft.port.warp.entity.client.EldritchGuardianModel;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BossRenderers {
    public static final ModelLayerLocation GOLEM=layer("eldritch_golem"),WARDEN=layer("eldritch_warden"),LEADER=layer("cultist_leader"),TENTACLE=layer("giant_taintacle");
    private static final ResourceLocation PORTAL=id("textures/misc/cultist_portal.png"),PARTICLES=id("textures/misc/particles.png");
    private static final RenderType PORTAL_LAYER=BossRenderTypes.billboard(PORTAL,false),ORB_LAYER=BossRenderTypes.billboard(PARTICLES,true);
    private static ModelLayerLocation layer(String name){return new ModelLayerLocation(id(name),"main");}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){
        event.registerLayerDefinition(GOLEM,EldritchGolemModel::layer);event.registerLayerDefinition(WARDEN,EldritchGuardianModel::layer);
        event.registerLayerDefinition(LEADER,()->LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32));
        event.registerLayerDefinition(TENTACLE,()->TaintacleModel.layer(14));
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(BossContent.ELDRITCH_GOLEM.get(),GolemRenderer::new);event.registerEntityRenderer(BossContent.ELDRITCH_WARDEN.get(),WardenRenderer::new);
        event.registerEntityRenderer(BossContent.CULTIST_LEADER.get(),LeaderRenderer::new);event.registerEntityRenderer(BossContent.CULTIST_PORTAL_GREATER.get(),PortalRenderer::new);
        event.registerEntityRenderer(BossContent.GIANT_TAINTACLE.get(),TentacleRenderer::new);event.registerEntityRenderer(BossContent.GOLEM_ORB.get(),OrbRenderer::new);
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private LeaderArmorModel outer,inner;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity e,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){if(outer==null){outer=new LeaderArmorModel(false);inner=new LeaderArmorModel(true);}return slot==EquipmentSlot.LEGS?inner:outer;}
            @Override public void setupModelAnimations(LivingEntity e,ItemStack stack,EquipmentSlot slot,Model model,float walk,float amount,float partial,float age,float yaw,float pitch){if(model instanceof LeaderArmorModel armor)armor.animateCloth(walk,amount);}
        },BossGear.PRAETOR_HELM,BossGear.PRAETOR_CHEST,BossGear.PRAETOR_LEGS);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        BossVisuals.charge=BossParticles::charge;BossVisuals.golemTick=BossParticles::golemTick;BossVisuals.wisp=BossParticles::wisp;
        BossVisuals.sonic=BossParticles::sonic;BossVisuals.orbTrail=BossParticles::orbTrail;BossVisuals.impact=BossParticles::impact;
    });}
    private static final class GolemRenderer extends MobRenderer<EldritchGolem,EldritchGolemModel>{
        GolemRenderer(EntityRendererProvider.Context c){super(c,new EldritchGolemModel(c.bakeLayer(GOLEM)),1);}
        @Override public ResourceLocation getTextureLocation(EldritchGolem e){return id("textures/entity/eldritch_golem.png");}
        @Override protected void scale(EldritchGolem e,PoseStack pose,float partial){pose.scale(1.7f,1.7f,1.7f);}
        @Override protected RenderType getRenderType(EldritchGolem e,boolean visible,boolean translucent,boolean outline){return outline?RenderType.outline(getTextureLocation(e)):visible||translucent?RenderType.entityTranslucent(getTextureLocation(e)):null;}
    }
    private static final class WardenRenderer extends MobRenderer<EldritchWarden,EldritchWardenModel>{
        WardenRenderer(EntityRendererProvider.Context c){super(c,new EldritchWardenModel(c.bakeLayer(WARDEN)),.6f);addLayer(new RenderLayer<>(this){
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,EldritchWarden e,float walk,float amount,float partial,float age,float yaw,float pitch){
                int glow=(int)(210+Mth.sin(e.tickCount/3f)*15);getParentModel().renderEye(pose,buffers.getBuffer(RenderType.eyes(getTextureLocation(e))),glow,OverlayTexture.NO_OVERLAY);
            }
        });}
        @Override public ResourceLocation getTextureLocation(EldritchWarden e){return id("textures/entity/eldritch_warden.png");}
        @Override protected RenderType getRenderType(EldritchWarden e,boolean visible,boolean translucent,boolean outline){return outline?RenderType.outline(getTextureLocation(e)):visible||translucent?RenderType.entityTranslucent(getTextureLocation(e)):null;}
        @Override public void render(EldritchWarden e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,-e.getBbHeight()*e.getSpawnTimer()/150f,0);super.render(e,yaw,partial,pose,buffers,light);pose.popPose();}
    }
    private static final class LeaderRenderer extends HumanoidMobRenderer<CultistLeader,HumanoidModel<CultistLeader>>{
        LeaderRenderer(EntityRendererProvider.Context c){super(c,new HumanoidModel<>(c.bakeLayer(LEADER)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),c.getModelManager()));}
        @Override public ResourceLocation getTextureLocation(CultistLeader e){return id("textures/entity/cultist.png");}
        @Override protected void scale(CultistLeader e,PoseStack pose,float partial){pose.scale(1.15f,1.15f,1.15f);}
    }
    private static final class TentacleRenderer extends MobRenderer<Taintacle,TaintacleModel>{
        TentacleRenderer(EntityRendererProvider.Context c){super(c,new TaintacleModel(c.bakeLayer(TENTACLE),14),1);}
        @Override public ResourceLocation getTextureLocation(Taintacle e){return id("textures/entity/taintacle.png");}
        @Override protected RenderType getRenderType(Taintacle e,boolean visible,boolean translucent,boolean outline){return outline?RenderType.outline(getTextureLocation(e)):visible||translucent?RenderType.entityTranslucent(getTextureLocation(e)):null;}
    }
    private static final class PortalRenderer extends EntityRenderer<CultistPortalGreater>{
        PortalRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.1f;shadowStrength=.5f;}
        @Override public ResourceLocation getTextureLocation(CultistPortalGreater e){return PORTAL;}
        @Override public void render(CultistPortalGreater e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            float height=1.5f;int age=(int)Math.min(50,e.tickCount+partial);
            if(e.hurtTime>0){double pulse=Math.sin(e.hurtTime*72*Math.PI/180);height-=pulse/4;age=(int)(age+6*pulse);}
            if(e.pulse>0){double pulse=Math.sin(e.pulse*36*Math.PI/180);height+=pulse/4;age=(int)(age+12*pulse);}
            float width=age/50f*1.3f,damage=(1-e.getHealth()/e.getMaxHealth())/3;
            float bob=Mth.sin(e.tickCount/(5-12*damage))*damage+damage,bob2=Mth.sin(e.tickCount/(6-15*damage))*damage+damage;
            height-=bob/4;width-=bob2/3;float alpha=1-bob;
            pose.pushPose();pose.translate(0,e.getBbHeight()/2,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
            int frame=15-(int)(e.level().getGameTime()%16);quad(pose,buffers.getBuffer(PORTAL_LAYER),width,height,(frame+1)/16f,frame/16f,1,0,(int)(255*alpha)<<24|0xffffff,220);
            pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private static final class OrbRenderer extends EntityRenderer<GolemOrb>{
        OrbRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(GolemOrb e){return PARTICLES;}
        @Override public void render(GolemOrb e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());float size=.5f*(1.2f+Mth.sin(e.tickCount/5f)*.2f),u=(1+e.tickCount%6)/32f,v=(e.isRed()?6:7)/32f;
            quad(pose,buffers.getBuffer(ORB_LAYER),size,size,u,u+1/32f,v,v+1/32f,-1,220);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private static void quad(PoseStack pose,VertexConsumer out,float w,float h,float u0,float u1,float v0,float v1,int color,int light){
        float[][] points={{-w,-h,u0,v1},{w,-h,u1,v1},{w,h,u1,v0},{-w,h,u0,v0}};
        for(var p:points)out.addVertex(pose.last().pose(),p[0],p[1],0).setColor(color).setUv(p[2],p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,1);
    }
    private BossRenderers(){}
}
