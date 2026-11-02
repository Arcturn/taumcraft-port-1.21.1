package thaumcraft.port.auromancy.client.pech;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.port.auromancy.pech.PechContent;
import thaumcraft.port.auromancy.pech.PechEntity;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PechClient {
    static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.parse("thaumcraft:pech"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,PechModel::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(PechContent.PECH.get(),Renderer::new);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(PechContent.MENU.get(),PechScreen::new);}
    private static final class Renderer extends MobRenderer<PechEntity,PechModel> {
        private static final ResourceLocation[] TEXTURES={ResourceLocation.parse("thaumcraft:textures/entity/pech_forage.png"),ResourceLocation.parse("thaumcraft:textures/entity/pech_thaum.png"),ResourceLocation.parse("thaumcraft:textures/entity/pech_stalker.png")};
        Renderer(EntityRendererProvider.Context context){super(context,new PechModel(context.bakeLayer(LAYER)),.4f);addLayer(new HeldItems(this));}
        @Override public ResourceLocation getTextureLocation(PechEntity pech){return TEXTURES[Math.clamp(pech.getPechType(),0,2)];}
        @Override public Vec3 getRenderOffset(PechEntity pech,float partial){return pech.isCrouching()?new Vec3(0,-.125,0):Vec3.ZERO;}
    }
    /** Original Pech hand positions are much shorter than ordinary humanoid arms. */
    private static final class HeldItems extends RenderLayer<PechEntity,PechModel> {
        HeldItems(Renderer parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,PechEntity pech,float walk,float amount,float partial,float age,float yaw,float pitch){
            boolean right=pech.getMainArm()==HumanoidArm.RIGHT;
            renderItem(pech,right?pech.getMainHandItem():pech.getOffhandItem(),HumanoidArm.RIGHT,pose,buffers,light);
            renderItem(pech,right?pech.getOffhandItem():pech.getMainHandItem(),HumanoidArm.LEFT,pose,buffers,light);
        }
        private void renderItem(PechEntity pech,ItemStack stack,HumanoidArm arm,PoseStack pose,MultiBufferSource buffers,int light){
            if(stack.isEmpty())return;
            pose.pushPose();getParentModel().translateToHand(arm,pose);pose.translate(0,-.1,.0625);
            if(stack.getItem() instanceof BowItem)pose.translate(-.075,-.1,0);
            if(pech.isCrouching())pose.translate(0,.2,0);
            pose.mulPose(Axis.XP.rotationDegrees(-90));pose.mulPose(Axis.YP.rotationDegrees(180));
            boolean left=arm==HumanoidArm.LEFT;pose.translate(left?-.0625:.0625,.125,-.625);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(pech,stack,left?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,left,pose,buffers,light);
            pose.popPose();
        }
    }
    private PechClient() { }
}
