package thaumcraft.port.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import thaumcraft.port.ThaumcraftPort;

import java.util.ArrayList;
import java.util.List;

/** Draw the complete additive glow after the world, with foreground transparency attenuation. */
@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, value = Dist.CLIENT)
public final class DeferredWispRenderer {
    private record WispDraw(Matrix4f pose, int frame, int color, float fogStart, float fogEnd, FogShape fogShape) {}
    private static final List<WispDraw> pending = new ArrayList<>();
    private static final WispDepthSnapshot opaqueDepth = new WispDepthSnapshot();
    private static boolean worldFrame;

    static boolean canEnqueue() { return worldFrame && WispRenderTypes.hasGlowShader(); }

    static void enqueue(PoseStack pose, int frame, int color) {
        pending.add(new WispDraw(new Matrix4f(pose.last().pose()), frame, color,
                RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(), RenderSystem.getShaderFogShape()));
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        // Also drops queued GUI/entity previews and unfinished frames before the next world frame.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            pending.clear();
            opaqueDepth.invalidate();
            worldFrame = true;
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (!pending.isEmpty() && Minecraft.useShaderTransparency())
                opaqueDepth.capture(Minecraft.getInstance().getMainRenderTarget());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        worldFrame = false;
        if (pending.isEmpty()) return;
        // Never draw through solids if a different renderer skipped the expected capture stage.
        if (Minecraft.useShaderTransparency() && !opaqueDepth.isValid()) {
            pending.clear();
            return;
        }
        var mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        float oldStart = RenderSystem.getShaderFogStart(), oldEnd = RenderSystem.getShaderFogEnd();
        var oldShape = RenderSystem.getShaderFogShape();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try {
            mc.getMainRenderTarget().bindWrite(false);
            WispRenderTypes.prepareTransparency(opaqueDepth.textureId());
            for (var wisp : pending) {
                RenderSystem.setShaderFogStart(wisp.fogStart());
                RenderSystem.setShaderFogEnd(wisp.fogEnd());
                RenderSystem.setShaderFogShape(wisp.fogShape());
                var pose = new PoseStack();
                // Keep Position in camera-relative world space, just as in the entity pass:
                // eyes.vsh calculates cylindrical fog before applying the view matrix.
                pose.mulPose(wisp.pose());
                draw(pose, buffers, wisp.frame(), wisp.color(), true);
                buffers.endBatch(WispRenderTypes.PARTICLES);
                buffers.endBatch(WispRenderTypes.AURA);
            }
        } finally {
            pending.clear();
            RenderSystem.setShaderFogStart(oldStart);
            RenderSystem.setShaderFogEnd(oldEnd);
            RenderSystem.setShaderFogShape(oldShape);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        pending.clear();
        worldFrame = false;
        opaqueDepth.close();
    }

    @SubscribeEvent
    public static void shutdown(GameShuttingDownEvent event) {
        pending.clear();
        worldFrame = false;
        opaqueDepth.close();
    }

    static void drawImmediate(PoseStack pose, MultiBufferSource buffers, int frame, int color) {
        draw(pose, buffers, frame, color, false);
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, int frame, int color, boolean deferred) {
        var particles = deferred ? WispRenderTypes.PARTICLES : WispRenderTypes.IMMEDIATE_PARTICLES;
        var aura = deferred ? WispRenderTypes.AURA : WispRenderTypes.IMMEDIATE_AURA;
        layer(pose, buffers, particles, 64, 512 + frame, .4f, 0xffffff, 1);
        layer(pose, buffers, particles, 64, 320 + frame, .75f, 0xffffff, .25f);
        layer(pose, buffers, aura, 32, 800 + frame, .75f, color, .5f);
    }

    private static void layer(PoseStack pose, MultiBufferSource buffers, RenderType type, int grid,
                              int frame, float size, int color, float alpha) {
        var out = buffers.getBuffer(type);
        float u0 = (frame % grid) / (float) grid, u1 = (frame % grid + 1) / (float) grid;
        float v0 = (frame / grid) / (float) grid, v1 = (frame / grid + 1) / (float) grid;
        float[][] vertices = {{-size, -size, u1, v1}, {size, -size, u0, v1},
                {size, size, u0, v0}, {-size, size, u1, v0}};
        for (var v : vertices) out.addVertex(pose.last().pose(), v[0], v[1], 0)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (alpha * 255))
                .setUv(v[2], v[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose.last(), 0, 0, 1);
    }

    private DeferredWispRenderer() {}
}
