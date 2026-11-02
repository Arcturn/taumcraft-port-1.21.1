package thaumcraft.port.artifice.construct.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.artifice.construct.*;
import thaumcraft.port.client.SalisMundusParticle;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConstructClient {
    private static final ModelLayerLocation CROSSBOW=new ModelLayerLocation(id("turret_basic"),"main"),BORE=new ModelLayerLocation(id("arcane_bore"),"main");
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(CROSSBOW,ConstructCrossbowModel::layer);event.registerLayerDefinition(BORE,ConstructBoreModel::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ConstructContent.BASIC.get(),BasicRenderer::new);event.registerEntityRenderer(ConstructContent.ADVANCED.get(),AdvancedRenderer::new);event.registerEntityRenderer(ConstructContent.BORE.get(),BoreRenderer::new);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(ConstructContent.MENU.get(),ConstructScreen::new);}
    private static final class BasicRenderer extends MobRenderer<TurretEntity,ConstructCrossbowModel>{
        BasicRenderer(EntityRendererProvider.Context context){super(context,new ConstructCrossbowModel(context.bakeLayer(CROSSBOW)),.5f);}
        @Override public ResourceLocation getTextureLocation(TurretEntity entity){return id("textures/entity/crossbow.png");}
        @Override public void render(TurretEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){entity.yBodyRot=entity.yBodyRotO=0;super.render(entity,yaw,partial,pose,buffers,light);}
    }
    /** Advanced crossbow has its own eight-part OBJ, not the basic wooden model with another skin. */
    private static final class AdvancedRenderer extends EntityRenderer<TurretEntity>{
        private final ConstructObjModel model=new ConstructObjModel(id("models/obj/crossbow_advanced.obj"));
        AdvancedRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
        @Override public ResourceLocation getTextureLocation(TurretEntity entity){return id("textures/entity/crossbow_advanced.png");}
        @Override public void render(TurretEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var out=buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));pose.pushPose();pose.translate(0,.75,0);pose.pushPose();
            if(entity.getVehicle() instanceof net.minecraft.world.entity.vehicle.AbstractMinecart)pose.scale(.66f,.75f,.66f);
            model.render("legs",pose,out,light,-1);pose.popPose();pose.pushPose();int color=-1;
            if(entity.hurtTime>0){color=0xffff8080;float jiggle=entity.hurtTime/500f;var random=entity.getRandom();pose.translate(random.nextGaussian()*jiggle,random.nextGaussian()*jiggle,random.nextGaussian()*jiggle);}
            pose.mulPose(Axis.YN.rotationDegrees(Mth.lerp(partial,entity.yHeadRotO,entity.yHeadRot)));pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));
            for(var group:new String[]{"mech","box","shield","brain"})model.render(group,pose,out,light,color);
            pose.pushPose();pose.translate(0,0,Mth.sin(Mth.sqrt(entity.loadProgress(partial))*Mth.PI*2)/12);model.render("loader",pose,out,light,color);pose.popPose();
            float swing=Mth.sin(Mth.sqrt(entity.shotProgress(partial))*Mth.PI*2)*20;pose.translate(0,0,.375);pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(swing));model.render("bow1",pose,out,light,color);pose.popPose();pose.pushPose();pose.mulPose(Axis.YN.rotationDegrees(swing));model.render("bow2",pose,out,light,color);pose.popPose();pose.popPose();pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private static final class BoreRenderer extends MobRenderer<ArcaneBoreEntity,ConstructBoreModel>{
        BoreRenderer(EntityRendererProvider.Context context){super(context,new ConstructBoreModel(context.bakeLayer(BORE)),.5f);}
        @Override public ResourceLocation getTextureLocation(ArcaneBoreEntity entity){return id("textures/entity/arcanebore.png");}
        @Override protected RenderType getRenderType(ArcaneBoreEntity entity,boolean visible,boolean translucent,boolean glowing){return visible||translucent?RenderType.entityTranslucent(getTextureLocation(entity)):super.getRenderType(entity,false,false,glowing);}
        @Override public void render(ArcaneBoreEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            entity.yBodyRot=entity.yBodyRotO=0;super.render(entity,yaw,partial,pose,buffers,light);
            if(entity.digTarget()==null||!entity.isActive()||!entity.validInventory())return;
            Vec3 offset=new Vec3(.5,.075,0).zRot(entity.getXRot()*Mth.DEG_TO_RAD).yRot(-(entity.yHeadRot+90)*Mth.DEG_TO_RAD);
            pose.pushPose();pose.translate(offset.x,entity.getEyeHeight()+offset.y,offset.z);
            pose.pushPose();pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());var glow=buffers.getBuffer(Layers.GLOW);int frame=96+entity.tickCount%32;float u=(frame%32)/32f,v=(frame/32)/32f;
            quad(glow,pose,-.5f,-.5f,.5f,.5f,u,v,u+1/32f,v+1/32f,0,1,.4f,.8f,210);pose.popPose();
            float age=entity.tickCount+partial;float scroll=age*.2f-Mth.floor(age*.1f);float rot=(entity.level().getGameTime()%72)*5+5*partial;
            pose.mulPose(Axis.XN.rotationDegrees(90));pose.mulPose(Axis.ZN.rotationDegrees(180+Mth.lerp(partial,entity.yRotO,entity.getYRot())));pose.mulPose(Axis.XN.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));pose.mulPose(Axis.YP.rotationDegrees(rot));
            var beam=buffers.getBuffer(Layers.BEAM);
            for(int i=0;i<3;i++){pose.mulPose(Axis.YP.rotationDegrees(60));float bottom=-1+scroll+i/3f;vertex(beam,pose,0,5,0,1,5+bottom,0,1,.4f,.4f,200);vertex(beam,pose,-.15f,0,0,1,bottom,0,1,.4f,.4f,200);vertex(beam,pose,.15f,0,0,0,bottom,0,1,.4f,.4f,200);vertex(beam,pose,0,5,0,0,5+bottom,0,1,.4f,.4f,200);}
            pose.popPose();
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1,float r,float g,float b,float a,int light){vertex(out,pose,x0,y0,0,u1,v1,r,g,b,a,light);vertex(out,pose,x0,y1,0,u1,v0,r,g,b,a,light);vertex(out,pose,x1,y1,0,u0,v0,r,g,b,a,light);vertex(out,pose,x1,y0,0,u0,v1,r,g,b,a,light);}
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,float r,float g,float b,float a,int light){out.addVertex(pose.last().pose(),x,y,z).setUv(u,v).setColor(r,g,b,a).setLight(light);}
    private static final class Layers extends RenderType{
        static final RenderType GLOW=layer("bore_glow","textures/misc/auranodes.png"),BEAM=layer("bore_beam","textures/misc/beam1.png");
        private static RenderType layer(String name,String texture){return create("thaumcraft_"+name,DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,1536,false,false,CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader)).setTextureState(new TextureStateShard(id(texture),false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        private Layers(){super("unused",DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
    }
    private ConstructClient(){}
}
