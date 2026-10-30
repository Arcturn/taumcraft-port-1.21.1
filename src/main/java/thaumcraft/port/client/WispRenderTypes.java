package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.port.ThaumcraftPort;
import thaumcraft.port.world.WorldContent;

import java.io.IOException;

/** Raw TC6 sheets retain their own filtering instead of inheriting the block atlas settings. */
@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WispRenderTypes extends RenderType {
    static final ResourceLocation PARTICLES_TEXTURE = WorldContent.id("textures/misc/particles.png");
    static final ResourceLocation AURA_TEXTURE = WorldContent.id("textures/misc/auranodes.png");
    static final RenderType PARTICLES = layer("thaumcraft_wisp_particles", PARTICLES_TEXTURE, true, true);
    static final RenderType AURA = layer("thaumcraft_wisp_aura", AURA_TEXTURE, false, true);
    static final RenderType IMMEDIATE_PARTICLES = layer("thaumcraft_wisp_particles_immediate", PARTICLES_TEXTURE, true, false);
    static final RenderType IMMEDIATE_AURA = layer("thaumcraft_wisp_aura_immediate", AURA_TEXTURE, false, false);
    private static ShaderInstance glowShader;

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) {
        glowShader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    WorldContent.id("wisp_glow"), DefaultVertexFormat.NEW_ENTITY),
                    loaded -> glowShader = loaded);
        } catch (IOException error) {
            // A resource pack with an incompatible shader must not erase the wisp.
            LogUtils.getLogger().warn("Cannot load wisp glow shader; using original entity rendering", error);
        }
    }

    public static boolean hasGlowShader() {
        return glowShader != null;
    }

    static void prepareTransparency(int opaqueDepthTexture) {
        if (glowShader == null) return;
        var mc = Minecraft.getInstance();
        var renderer = mc.levelRenderer;
        boolean fabulous = Minecraft.useShaderTransparency();
        glowShader.setSampler("OpaqueDepth", fabulous ? opaqueDepthTexture : 0);
        bindTarget("Translucent", fabulous ? renderer.getTranslucentTarget() : null);
        bindTarget("Item", fabulous ? renderer.getItemEntityTarget() : null);
        bindTarget("Particles", fabulous ? renderer.getParticlesTarget() : null);
        bindTarget("Weather", fabulous ? renderer.getWeatherTarget() : null);
        bindTarget("Clouds", fabulous ? renderer.getCloudsTarget() : null);
        glowShader.safeGetUniform("UseTransparencyLayers").set(fabulous ? 1 : 0);
        glowShader.safeGetUniform("UseClouds").set(mc.options.getCloudsType() != CloudStatus.OFF ? 1 : 0);
    }

    private static void bindTarget(String name, RenderTarget target) {
        // Clear old texture IDs too: switching out of Fabulous destroys these targets.
        glowShader.setSampler(name + "Color", target != null ? target.getColorTextureId() : 0);
        glowShader.setSampler(name + "Depth", target != null ? target.getDepthTextureId() : 0);
    }

    private static RenderType layer(String name, ResourceLocation texture, boolean blur, boolean deferred) {
        return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, true, CompositeState.builder()
                        .setShaderState(deferred ? new ShaderStateShard(() -> glowShader != null
                                ? glowShader : GameRenderer.getRendertypeEyesShader()) : RENDERTYPE_EYES_SHADER)
                        // particles.png.mcmeta enables blur in the original. Vanilla eyes()
                        // forcibly sets false, overriding that metadata on every draw.
                        .setTextureState(new TextureStateShard(texture, blur, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(new DepthTestStateShard("wisp_scene_depth", 515) {
                            @Override public void setupRenderState() {
                                super.setupRenderState();
                                // Fabulous's final blit has replaced main depth. The fragment
                                // shader tests the saved opaque depth instead in this mode.
                                if (deferred && Minecraft.useShaderTransparency()) RenderSystem.depthFunc(519);
                            }
                        })
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    private WispRenderTypes() {
        super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                0, false, false, () -> {}, () -> {});
    }
}
