package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.ThaumcraftPort;
import thaumcraft.port.content.PortContent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GogglesCurioRenderer implements ICurioRenderer {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(PortContent.GOGGLES.get(), GogglesCurioRenderer::new));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack,
            SlotContext context, PoseStack pose, RenderLayerParent<T, M> parent,
            MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (context.entity().isInvisible() || !(parent.getModel() instanceof HumanoidModel<?> model)) return;
        pose.pushPose();
        // Use the actual modern head pose: this also follows crouching, swimming and elytra.
        model.head.translateAndRotate(pose);
        GogglesGeometry.emit(pose, buffers.getBuffer(GogglesRenderTypes.BAUBLE), light);
        pose.popPose();
    }
}
