package thaumcraft.port.auromancy.firebat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.auromancy.firebat.FireBat;
import thaumcraft.port.auromancy.firebat.FireBatContent;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FireBatRenderer extends MobRenderer<FireBat,FireBatModel> {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.parse("thaumcraft:firebat"),"main");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/entity/firebat.png");
    public FireBatRenderer(EntityRendererProvider.Context context){super(context,new FireBatModel(context.bakeLayer(LAYER)),.25f);}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(FireBatContent.FIREBAT.get(),FireBatRenderer::new);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,FireBatModel::layer);}
    @Override public ResourceLocation getTextureLocation(FireBat bat){return TEXTURE;}
    @Override protected void scale(FireBat bat,PoseStack pose,float partial){pose.scale(.35f,.35f,.35f);}
    @Override public void render(FireBat bat,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,bat.hanging()?-.1f:Mth.cos((bat.tickCount+partial)*.3f)*.1f,0);
        super.render(bat,yaw,partial,pose,buffers,0xF000F0);pose.popPose();
    }
}
