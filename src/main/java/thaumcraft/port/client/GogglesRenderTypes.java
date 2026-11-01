package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.port.ThaumcraftPort;

/** Baubles leaves fixed-function normal rescaling off; keep its lighting, including the world lightmap. */
@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GogglesRenderTypes extends RenderType {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ThaumcraftPort.MOD_ID, "textures/items/goggles_bauble.png");
    private static ShaderInstance shader;

    static final RenderType BAUBLE = create("thaumcraft_goggles_bauble", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, true, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader != null
                            ? shader : GameRenderer.getRendertypeEntityCutoutNoCullShader()))
                    .setTextureState(new TextureStateShard(TEXTURE, false, false))
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true));

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftPort.MOD_ID, "goggles_bauble"),
                    DefaultVertexFormat.NEW_ENTITY), loaded -> shader = loaded);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Cannot load goggles bauble shader; using ordinary armor lighting", error);
        }
    }

    private GogglesRenderTypes() {
        super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                0, false, false, () -> {}, () -> {});
    }
}
