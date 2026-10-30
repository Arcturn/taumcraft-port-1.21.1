package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.opengl.GL11;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.research.ResearchCatalog;

/** Server-confirmed whole knowledge points fly into the book, as in TC6. */
@EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public final class KnowledgeGainHud {
    private static final ResourceLocation BOOK = BookAssets.id("textures/items/thaumonomicon.png");
    private static final ResourceLocation OBSERVATION = BookAssets.id("textures/research/knowledge_observation.png");
    private static final ResourceLocation THEORY = BookAssets.id("textures/research/knowledge_theory.png");
    private static final ResourceLocation PARTICLES = BookAssets.id("textures/misc/particles.png");
    private static final List<Tracker> gains = new ArrayList<>();
    private static int bookFade;

    /** Called only by the explicit gain payload, never by saved journal synchronization. */
    public static void receive(String type, String category, int count) {
        var mc = Minecraft.getInstance();
        boolean theory = "THEORY".equals(type);
        if (mc.level == null || mc.player == null || count <= 0
                || (!theory && !"OBSERVATION".equals(type))
                || !ResearchCatalog.CATEGORIES.contains(category)) return;
        for (int i = 0; i < count; i++) {
            int duration = 40 + mc.level.random.nextInt(20) + (theory ? 10 : 0);
            gains.add(new Tracker(theory ? THEORY : OBSERVATION, BookAssets.category(category),
                    duration, mc.level.random.nextLong()));
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    PortSounds.get("learn"), SoundSource.AMBIENT, 1, 1, false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        // TC6 updates this UI timer even while a menu pauses the world.
        if (gains.isEmpty()) bookFade = Math.max(0, bookFade - 1);
        else {
            bookFade = Math.min(40, bookFade + 10);
            gains.removeIf(gain -> gain.progress <= 0);
            for (var gain : gains) gain.progress--;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        gains.clear();
        bookFade = 0;
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        // With an open screen this same effect is drawn after that screen instead.
        if (Minecraft.getInstance().screen == null) render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Render.Post event) {
        // NeoForge can render several stacked screens. Only the active one owns
        // the overlay, so receiving one point still produces one visual per frame.
        if (event.getScreen() == Minecraft.getInstance().screen) render(event.getGuiGraphics());
    }

    private static void render(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (bookFade <= 0 || mc.level == null || mc.player == null
                || !(mc.getCameraEntity() instanceof Player)) return;
        // Original knowledge gains are an overlay above open GUIs and remain
        // visible with F1; unlike the scanner dial they do not require holding it.
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            int width = graphics.guiWidth(), height = graphics.guiHeight();
            RenderSystem.setShaderColor(1, 1, 1, bookFade / 40f);
            graphics.blit(BOOK, width - 17, height - 17, 16, 16, 0, 0, 1, 1, 1, 1);
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            for (var gain : gains) {
                var frame = KnowledgeGainAnimation.frame(width, height, gain.max, gain.progress, partial, gain.seed);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                quad(graphics, gain.texture, frame.x(), frame.y(), frame.size(), frame.rotation(), 0, 1);
                quad(graphics, gain.category, frame.x(), frame.y(), frame.size() * .75f, frame.rotation(), 0, 1);
                var flash = frame.flash();
                if (flash != null && flash.size() > 0) {
                    // The original sheet's blur=true metadata applies here too.
                    mc.getTextureManager().getTexture(PARTICLES).setFilter(true, false);
                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    RenderSystem.setShaderColor(1, flash.green(), flash.blue(), 1);
                    quad(graphics, PARTICLES, frame.x(), frame.y(), flash.size(), flash.rotation(), flash.frame(), 64);
                    RenderSystem.defaultBlendFunc();
                }
            }
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (!blend) RenderSystem.disableBlend();
        }
    }

    private static void quad(GuiGraphics graphics, ResourceLocation texture, float x, float y,
                             float size, float rotation, int frame, int grid) {
        if (size <= 0) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        graphics.pose().scale(size / 16, size / 16, 1);
        graphics.blit(texture, -8, -8, 16, 16, frame % grid, frame / grid, 1, 1, grid, grid);
        graphics.pose().popPose();
    }

    private static final class Tracker {
        final ResourceLocation texture, category;
        final int max;
        final long seed;
        int progress;

        Tracker(ResourceLocation texture, ResourceLocation category, int duration, long seed) {
            this.texture = texture;
            this.category = category;
            this.max = this.progress = duration;
            this.seed = seed;
        }
    }

    private KnowledgeGainHud() {}
}
