package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.world.*;

public final class WorldRenderers {
    public static final ModelLayerLocation SEED_LAYER=new ModelLayerLocation(WorldContent.id("taint_seed"),"main");
    public static final ModelLayerLocation CULTIST_LAYER=new ModelLayerLocation(WorldContent.id("cultist"),"main");
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(SEED_LAYER,SeedModel::layer);e.registerLayerDefinition(CULTIST_LAYER,()->LayerDefinition.create(net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32));}
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
            var points=e.points();var widths=e.widths();var out=buffers.getBuffer(RenderType.endGateway());var matrix=pose.last().pose();
            for(int i=0;i<points.size()-1;i++){
                var a=points.get(i);var b=points.get(i+1);var axis=b.subtract(a).normalize();
                Vec3 u=axis.cross(Math.abs(axis.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),v=axis.cross(u).normalize();
                for(int k=0;k<8;k++){
                    double angle=k*Math.PI/4,next=(k+1)*Math.PI/4;
                    Vec3 p=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle))),q=u.scale(Math.cos(next)).add(v.scale(Math.sin(next)));
                    for(Vec3 point:new Vec3[]{a.add(p.scale(widths.get(i))),a.add(q.scale(widths.get(i))),b.add(q.scale(widths.get(i+1))),b.add(p.scale(widths.get(i+1)))})
                        out.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z);
                }
            }
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
    public static final class SeedModel extends HierarchicalModel<TaintSeed>{
        private final ModelPart root;private final ModelPart[] sections=new ModelPart[7];
        SeedModel(ModelPart root){this.root=root;var part=root.getChild("root");for(int i=0;i<7;i++){part=part.getChild("segment"+i);sections[i]=part;}}
        public static LayerDefinition layer(){
            var mesh=new MeshDefinition();var parent=mesh.getRoot().addOrReplaceChild("root",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.offset(0,20,0));
            for(int i=0;i<7;i++){
                int side=i<4?8:4,uv=i<4?16:i==4?48:56;
                parent=parent.addOrReplaceChild("segment"+i,CubeListBuilder.create().texOffs(0,uv).addBox(-side/2f,-side/2f,-side/2f,side,side,side),PartPose.offset(0,i<=4?-8:-4,0));
            }
            return LayerDefinition.create(mesh,64,64);
        }
        @Override public ModelPart root(){return root;}
        @Override public void setupAnim(TaintSeed e,float swing,float amount,float age,float yaw,float pitch){
            for(int i=0;i<sections.length;i++){sections[i].xRot=(float)Math.sin(age*.06-i*.5)/15+e.hurtTime/200f;sections[i].zRot=(float)Math.sin(age*.05-i*.5)/15;float pulse=1+(float)Math.sin(age*.1-i)*.05f;sections[i].xScale=pulse;sections[i].zScale=pulse;}
        }
    }
    private static final class SeedRenderer extends MobRenderer<TaintSeed,SeedModel>{
        SeedRenderer(EntityRendererProvider.Context c){super(c,new SeedModel(c.bakeLayer(SEED_LAYER)),.4f);}
        @Override public ResourceLocation getTextureLocation(TaintSeed e){return WorldContent.id("textures/entity/taintseed.png");}
        @Override protected void scale(TaintSeed e,PoseStack pose,float partial){float size=e.prime()?.64f:.4f;pose.scale(size,size,size);}
    }
}
