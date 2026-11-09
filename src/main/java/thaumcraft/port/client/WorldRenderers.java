package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.world.*;

public final class WorldRenderers {
    public static final ModelLayerLocation SEED_LAYER=new ModelLayerLocation(WorldContent.id("taint_seed"),"main");
    public static final ModelLayerLocation CULTIST_LAYER=new ModelLayerLocation(WorldContent.id("cultist"),"main");
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(SEED_LAYER,TaintSeedModel::layer);e.registerLayerDefinition(CULTIST_LAYER,()->LayerDefinition.create(net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32));}
    public static void register(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(WorldEntities.RIFT.get(),RiftRenderer::new);
        e.registerEntityRenderer(WorldEntities.ALUMENTUM.get(),ThrownItemRenderer::new);
        e.registerEntityRenderer(WorldEntities.SEED.get(),SeedRenderer::new);
        e.registerEntityRenderer(WorldEntities.PRIME.get(),SeedRenderer::new);
        e.registerEntityRenderer(WorldEntities.WISP.get(),WispRenderer::new);
        e.registerEntityRenderer(WorldEntities.PORTAL.get(),PortalRenderer::new);
        e.registerEntityRenderer(WorldEntities.KNIGHT.get(),CultistRenderer::new);e.registerEntityRenderer(WorldEntities.CLERIC.get(),CultistRenderer::new);
        e.registerEntityRenderer(WorldEntities.ORB.get(),OrbRenderer::new);
        e.registerEntityRenderer(WorldEntities.SLIME.get(),c->new SlimeRenderer(c){
            @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Slime s){return WorldContent.id("textures/entity/tslime.png");}
        });
    }
    private static void billboard(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float width,float height,float u0,float u1,int color){
        var out=buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
        float[][] vertices={{-width,0,u0,1},{width,0,u1,1},{width,height,u1,0},{-width,height,u0,0}};
        for(var a:vertices)out.addVertex(pose.last().pose(),a[0],a[1],0).setColor((color>>16)&255,(color>>8)&255,color&255,255).setUv(a[2],a[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose.last(),0,0,1);
    }
    private static final class PortalRenderer extends EntityRenderer<CultistPortal>{
        PortalRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(CultistPortal e){return WorldContent.id("textures/misc/cultist_portal.png");}
        @Override public void render(CultistPortal e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());int frame=15-(e.tickCount/2%16);
            float scale=e.active()?1:.75f;billboard(pose,buffers,getTextureLocation(e),.75f*scale,3*scale,frame/16f,(frame+1)/16f,0xFFFFFF);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private static final class OrbRenderer extends EntityRenderer<CultistOrb>{
        OrbRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(CultistOrb e){return WorldContent.id("textures/misc/wispy.png");}
        @Override public void render(CultistOrb e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,-.25,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());billboard(pose,buffers,getTextureLocation(e),.25f,.5f,0,1,0xFF3030);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);}
    }
    private static final class CultistRenderer extends HumanoidMobRenderer<Cultist,net.minecraft.client.model.HumanoidModel<Cultist>>{
        CultistRenderer(EntityRendererProvider.Context c){super(c,new net.minecraft.client.model.HumanoidModel<>(c.bakeLayer(CULTIST_LAYER)),.5f);}
        @Override public ResourceLocation getTextureLocation(Cultist e){return WorldContent.id("textures/entity/cultist.png");}
    }
    private static final class RiftRenderer extends EntityRenderer<FluxRift>{
        RiftRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(FluxRift e){return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");}
        @Override public void render(FluxRift e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var points=e.points();var widths=e.widths();var matrix=pose.last().pose();
            if(thaumcraft.port.content.RevealingEquipment.hasGoggles(net.minecraft.client.Minecraft.getInstance().player))
                RiftGeometry.emit(points,widths,matrix,buffers.getBuffer(RiftRenderTypes.REVEALED),1.25f);
            RiftGeometry.emit(points,widths,matrix,buffers.getBuffer(RenderType.endGateway()),1);
            super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private static final class WispRenderer extends EntityRenderer<Wisp>{
        WispRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(Wisp e){return WispRenderTypes.PARTICLES_TEXTURE;}
        @Override public void render(Wisp e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            if(e.getHealth()<=0)return;
            pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
            int frame=e.tickCount%16;
            if(DeferredWispRenderer.canEnqueue()) DeferredWispRenderer.enqueue(pose,frame,e.aspect().color());
            else DeferredWispRenderer.drawImmediate(pose,buffers,frame,e.aspect().color());
            pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private static final class SeedRenderer extends MobRenderer<TaintSeed,TaintSeedModel>{
        SeedRenderer(EntityRendererProvider.Context c){super(c,new TaintSeedModel(c.bakeLayer(SEED_LAYER)),.4f);}
        @Override public ResourceLocation getTextureLocation(TaintSeed e){return WorldContent.id("textures/entity/taintseed.png");}
        @Override protected void setupRotations(TaintSeed e,PoseStack pose,float age,float yaw,float partial,float scale){super.setupRotations(e,pose,age,0,partial,scale);}
        @Override protected RenderType getRenderType(TaintSeed e,boolean visible,boolean translucent,boolean glowing){return glowing?RenderType.outline(getTextureLocation(e)):visible||translucent?RenderType.entityTranslucent(getTextureLocation(e)):null;}
    }
}
