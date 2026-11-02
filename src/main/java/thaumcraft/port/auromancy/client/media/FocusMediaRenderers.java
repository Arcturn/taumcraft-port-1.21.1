package thaumcraft.port.auromancy.client.media;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.auromancy.entity.*;

/** Original ModelGrappler and ModelFireBat geometry, including UVs and wing oscillation. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FocusMediaRenderers {
    private static final ModelLayerLocation MINE=new ModelLayerLocation(ResourceLocation.parse("thaumcraft:focus_mine"),"main");
    private static final ModelLayerLocation BAT=new ModelLayerLocation(ResourceLocation.parse("thaumcraft:spellbat"),"main");
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AuromancyEntities.PROJECTILE.get(),ProjectileRenderer::new);
        event.registerEntityRenderer(AuromancyEntities.CLOUD.get(),NoopRenderer::new);
        event.registerEntityRenderer(AuromancyEntities.MINE.get(),MineRenderer::new);
        event.registerEntityRenderer(AuromancyEntities.BAT.get(),BatRenderer::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {event.registerLayerDefinition(MINE,FocusMediaRenderers::mineLayer);event.registerLayerDefinition(BAT,FocusMediaRenderers::batLayer);}
    private static LayerDefinition mineLayer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-1.5f,-1.5f,-1.5f,3,3,3),PartPose.ZERO);
        root.addOrReplaceChild("prong0",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.ZERO);
        root.addOrReplaceChild("prong1",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.rotation(0,Mth.HALF_PI,0));
        root.addOrReplaceChild("prong2",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.rotation(Mth.HALF_PI,Mth.HALF_PI,0));
        return LayerDefinition.create(mesh,64,32);
    }
    private static LayerDefinition batLayer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.ZERO);
        head.addOrReplaceChild("right_ear",CubeListBuilder.create().texOffs(24,0).addBox(-4,-6,-2,3,4,1),PartPose.ZERO);
        head.addOrReplaceChild("left_ear",CubeListBuilder.create().texOffs(24,0).mirror().addBox(1,-6,-2,3,4,1),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,16).addBox(-3,4,-3,6,12,6).texOffs(0,34).addBox(-5,16,0,10,6,1),PartPose.ZERO);
        var right=body.addOrReplaceChild("right",CubeListBuilder.create().texOffs(42,0).addBox(-12,1,1.5f,10,16,1),PartPose.ZERO);
        right.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).addBox(-8,1,0,8,12,1),PartPose.offset(-12,1,1.5f));
        var left=body.addOrReplaceChild("left",CubeListBuilder.create().texOffs(42,0).mirror().addBox(2,1,1.5f,10,16,1),PartPose.ZERO);
        left.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).mirror().addBox(0,1,0,8,12,1),PartPose.offset(12,1,1.5f));
        return LayerDefinition.create(mesh,64,64);
    }
    private static final class ProjectileRenderer extends EntityRenderer<EntityFocusProjectile> {
        private final WeakHashMap<EntityFocusProjectile,Integer> lastTick=new WeakHashMap<>();
        private final WeakHashMap<EntityFocusProjectile,Float> lastFrame=new WeakHashMap<>();
        ProjectileRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(EntityFocusProjectile entity){return ResourceLocation.parse("thaumcraft:textures/misc/particles.png");}
        @Override public void render(EntityFocusProjectile entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            float frame=entity.tickCount+partial;
            if(lastFrame.getOrDefault(entity,-1f)!=frame){boolean first=lastTick.getOrDefault(entity,-1)!=entity.tickCount;FocusMediaClient.projectile(entity,partial,first);lastTick.put(entity,entity.tickCount);lastFrame.put(entity,frame);}
        }
    }
    private static final class MineRenderer extends EntityRenderer<EntityFocusMine> {
        private final ModelPart model;
        MineRenderer(EntityRendererProvider.Context context){super(context);model=context.bakeLayer(MINE);}
        @Override public ResourceLocation getTextureLocation(EntityFocusMine entity){return ResourceLocation.parse("thaumcraft:textures/entity/mine.png");}
        @Override public void render(EntityFocusMine entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,entity.yRotO,entity.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));
            int tint=(int)((1-((entity.getArmingTicks()+partial)%8)/8)*255);int color=0xFFFF0000|tint<<8|tint;
            model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),15728880,OverlayTexture.NO_OVERLAY,color);pose.popPose();
        }
    }
    private static final class BatRenderer extends EntityRenderer<EntitySpellBat> {
        private final ModelPart model,head,body,right,left;
        BatRenderer(EntityRendererProvider.Context context){super(context);model=context.bakeLayer(BAT);head=model.getChild("head");body=model.getChild("body");right=body.getChild("right");left=body.getChild("left");shadowRadius=.25f;}
        @Override public ResourceLocation getTextureLocation(EntitySpellBat entity){return ResourceLocation.parse("thaumcraft:textures/entity/spellbat.png");}
        @Override public void render(EntitySpellBat entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            float age=entity.tickCount+partial;head.xRot=Mth.lerp(partial,entity.xRotO,entity.getXRot())*Mth.DEG_TO_RAD;head.yRot=Mth.rotLerp(partial,entity.yHeadRotO,entity.yHeadRot)*Mth.DEG_TO_RAD-yaw*Mth.DEG_TO_RAD;
            body.xRot=Mth.PI/4+Mth.cos(age*.1f)*.15f;right.yRot=Mth.cos(age*1.3f)*Mth.PI*.25f;left.yRot=-right.yRot;right.getChild("outer").yRot=right.yRot*.5f;left.getChild("outer").yRot=left.yRot*.5f;
            pose.pushPose();pose.translate(0,-.1,0);pose.mulPose(Axis.YP.rotationDegrees(180-yaw));pose.scale(-.35f,-.35f,.35f);pose.translate(0,-1.501,0);
            model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),15728880,OverlayTexture.NO_OVERLAY,0x80000000|FocusMediaVisuals.color(entity.getFocusPackage()));pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private FocusMediaRenderers() { }
}
