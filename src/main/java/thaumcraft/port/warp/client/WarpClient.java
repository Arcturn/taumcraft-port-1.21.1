package thaumcraft.port.warp.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import thaumcraft.port.config.ClientVisualConfig;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.warp.WarpEffects;
import thaumcraft.port.warp.WarpPayload;

/** TC6's private sounds, exponential mist and independent potion shader chains. */
@EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public final class WarpClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation VIGNETTE = ResourceLocation.parse("thaumcraft:textures/misc/vignette.png");
    private static final WarpVisualState STATE = new WarpVisualState();
    private static final EnumMap<Effect, Chain> CHAINS = new EnumMap<>(Effect.class);
    private static final EnumSet<Effect> FAILED = EnumSet.noneOf(Effect.class);
    private static Object level;
    private static float fogRed, fogGreen, fogBlue;

    @EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> WarpPayload.receiver = WarpClient::receive);
        }
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                closeChains();
                FAILED.clear();
            });
        }
    }

    public static void receive(int kind) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        checkLevel();
        if (kind == WarpPayload.MIST || kind == WarpPayload.SHORT_MIST) {
            STATE.mist(kind == WarpPayload.SHORT_MIST);
            return;
        }
        if (ClientVisualConfig.NO_STRESS.get()) return;
        var player = mc.player;
        var random = player.getRandom();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        switch (kind) {
            case WarpPayload.HEARTBEAT -> mc.level.playLocalSound(x, y, z, PortSounds.get("heartbeat"), SoundSource.AMBIENT, 1, 1, false);
            case WarpPayload.HISS -> mc.level.playLocalSound(x, y, z, SoundEvents.CREEPER_PRIMED, SoundSource.AMBIENT, 1, .5f, false);
            case WarpPayload.EXPLOSION -> {
                x += (random.nextFloat() - random.nextFloat()) * 10;
                y += (random.nextFloat() - random.nextFloat()) * 10;
                z += (random.nextFloat() - random.nextFloat()) * 10;
                float pitch = (1 + (random.nextFloat() - random.nextFloat()) * .2f) * .7f;
                mc.level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 4, pitch, false);
            }
            default -> {}
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        checkLevel();
        if (mc.player != null && mc.level != null && !mc.isPaused()) STATE.tick(mc.player.hasEffect(WarpEffects.DEATH_GAZE));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); level = null; }
    private static void checkLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) { reset(); level = current; }
    }
    private static void reset() { STATE.reset(); closeChains(); FAILED.clear(); }
    private static void closeChains() { CHAINS.values().forEach(chain -> chain.post.close()); CHAINS.clear(); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        fogRed = event.getRed(); fogGreen = event.getGreen(); fogBlue = event.getBlue();
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        float density = STATE.density();
        if (density <= 0) return;
        // Vanilla no longer has GL_EXP. Remove its linear fog while our depth pass
        // reproduces TC6's exponential equation with the current biome/fluid color.
        if (chain(Effect.MIST) != null) {
            event.setNearPlaneDistance(1_000_000);
            event.setFarPlaneDistance(1_000_001);
        } else {
            // A resource-pack/GPU shader failure must still leave usable mist.
            event.setNearPlaneDistance(0);
            event.setFarPlaneDistance(3 / density);
        }
        event.setCanceled(true);
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || STATE.density() <= 0) return;
        var chain = chain(Effect.MIST);
        if (chain == null) return;
        chain.post.setUniform("Density", STATE.density());
        chain.post.setUniform("ProjectionA", event.getProjectionMatrix().m22());
        chain.post.setUniform("ProjectionB", event.getProjectionMatrix().m32());
        chain.post.setUniform("FogRed", fogRed);
        chain.post.setUniform("FogGreen", fogGreen);
        chain.post.setUniform("FogBlue", fogBlue);
        process(chain, event.getPartialTick().getGameTimeDeltaTicks());
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        var graphics = event.getGuiGraphics();
        graphics.flush();
        boolean enabled = !ClientVisualConfig.DISABLE_WARP_SHADERS.get();
        apply(Effect.DEATH_GAZE, enabled && mc.player.hasEffect(WarpEffects.DEATH_GAZE), event);
        apply(Effect.BLURRED_VISION, enabled && mc.player.hasEffect(WarpEffects.BLURRED_VISION), event);
        apply(Effect.HUNGER, enabled && mc.player.hasEffect(WarpEffects.UNNATURAL_HUNGER), event);
        apply(Effect.SUN_SCORNED, enabled && mc.player.hasEffect(WarpEffects.SUN_SCORNED), event);
        if (STATE.density() <= 0) close(Effect.MIST);
        if (!STATE.hasVignette()) return;
        float brightness = STATE.vignette(mc.player.tickCount);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1);
        try {
            graphics.blit(VIGNETTE, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, 0, 256, 256, 256, 256);
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            if (depth) RenderSystem.enableDepthTest();
            if (!blend) RenderSystem.disableBlend();
        }
    }
    private static void apply(Effect effect, boolean active, RenderGuiEvent.Pre event) {
        if (!active) { close(effect); return; }
        var chain = chain(effect);
        if (chain != null) process(chain, event.getPartialTick().getGameTimeDeltaTicks());
    }
    private static void close(Effect effect) {
        var chain = CHAINS.remove(effect);
        if (chain != null) chain.post.close();
    }
    private static Chain chain(Effect effect) {
        if (FAILED.contains(effect)) return null;
        var mc = Minecraft.getInstance();
        int width = mc.getMainRenderTarget().width, height = mc.getMainRenderTarget().height;
        var chain = CHAINS.get(effect);
        try {
            if (chain == null) {
                var post = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        ResourceLocation.parse("thaumcraft:shaders/post/warp_" + effect.path + ".json"));
                post.resize(width, height);
                chain = new Chain(effect, post, width, height);
                CHAINS.put(effect, chain);
            } else if (chain.width != width || chain.height != height) {
                chain.post.resize(width, height); chain.width = width; chain.height = height;
            }
            return chain;
        } catch (IOException | RuntimeException error) {
            failed(effect, error);
            return null;
        }
    }
    private static void process(Chain chain, float partialTick) {
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.resetTextureMatrix();
        try { chain.post.process(partialTick); }
        catch (RuntimeException error) { failed(chain.effect, error); }
        finally {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            RenderSystem.defaultBlendFunc();
            if (depth) RenderSystem.enableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
    private static void failed(Effect effect, Exception error) {
        close(effect);
        FAILED.add(effect);
        LOGGER.error("Could not render Thaumcraft Warp shader {}; retrying on resource reload", effect.path, error);
    }
    private enum Effect {
        DEATH_GAZE("death_gaze"), BLURRED_VISION("blurred_vision"), HUNGER("hunger"), SUN_SCORNED("sun_scorned"), MIST("mist");
        final String path;
        Effect(String path) { this.path = path; }
    }
    private static final class Chain {
        final Effect effect;
        final PostChain post;
        int width, height;
        Chain(Effect effect, PostChain post, int width, int height) { this.effect = effect; this.post = post; this.width = width; this.height = height; }
    }
    private WarpClient() {}
}
