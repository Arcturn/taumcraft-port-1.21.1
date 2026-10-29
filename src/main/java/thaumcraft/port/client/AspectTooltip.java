package thaumcraft.port.client;

import com.mojang.datafixers.util.Either;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import thaumcraft.port.ThaumcraftPort;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.config.ClientVisualConfig;

/** TC6's Shift aspect row, carried by a real tooltip component instead of padding text with spaces. */
@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, value = Dist.CLIENT)
public final class AspectTooltip {
    static final int ICON_SIZE = 16;
    static final int PITCH = 18;

    public record Entry(Aspect aspect, int amount) {}

    public record Data(List<Entry> entries, int columns) implements TooltipComponent {
        public Data {
            entries = List.copyOf(entries);
            columns = entries.isEmpty() ? 0 : Math.clamp(columns, 1, entries.size());
        }
        public int rows() { return columns == 0 ? 0 : (entries.size() + columns - 1) / columns; }
        public int width() { return columns * PITCH; }
        public int height() { return rows() * PITCH; }
    }

    @SubscribeEvent
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getItemStack().isEmpty()
                || !visible(minecraft.screen instanceof AbstractContainerScreen<?>,
                    minecraft.mouseHandler.isMouseGrabbed(), Screen.hasShiftDown(), ClientVisualConfig.SHOW_TAGS.get())) return;
        if (event.getTooltipElements().stream().anyMatch(element -> element.right().orElse(null) instanceof Data)) return;

        int available = Math.max(PITCH, event.getScreenWidth() - 16);
        if (event.getMaxWidth() > 0) available = Math.min(available, Math.max(PITCH, event.getMaxWidth()));
        var data = layout(RecipeAspects.get(event.getItemStack(), minecraft.level), available);
        if (!data.entries().isEmpty()) event.getTooltipElements().add(Either.right(data));
    }

    /** Original GUI/Shift rules deliberately contain no research or held-thaumometer requirement. */
    static boolean visible(boolean containerScreen, boolean mouseGrabbed, boolean shift, boolean showTags) {
        return containerScreen && !mouseGrabbed && shift != showTags;
    }

    static Data layout(Map<Aspect, Integer> amounts, int availableWidth) {
        // The original stable amount sort retains insertion order for equal amounts.
        var entries = amounts.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(Entry::amount).reversed()).toList();
        return new Data(entries, Math.max(1, availableWidth / PITCH));
    }

    public static final class Display implements ClientTooltipComponent {
        private final Data data;

        public Display(Data data) { this.data = data; }
        @Override public int getHeight() { return data.height(); }
        @Override public int getWidth(Font font) { return data.width(); }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            float textScale = ClientVisualConfig.LARGE_TAG_TEXT.get() ? 1 : BookTypography.smallScale();
            for (int i = 0; i < data.entries().size(); i++) {
                var entry = data.entries().get(i);
                int left = x + i % data.columns() * PITCH;
                int top = y + i / data.columns() * PITCH + 2;
                // Tooltip text is queued before image components. Flush it while the
                // color is still white, before the aspect renderer changes the shader tint.
                graphics.flush();
                ResearchPageScreen.aspect(graphics, entry.aspect(), left, top, ICON_SIZE, true);
                drawAmount(graphics, font, Integer.toString(entry.amount()), left, top, textScale);
            }
        }
    }

    private static void drawAmount(GuiGraphics graphics, Font font, String amount, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 1);
        graphics.pose().scale(scale, scale, 1);
        int left = Math.round(ICON_SIZE / scale) - font.width(amount);
        int top = Math.round(ICON_SIZE / scale) - font.lineHeight;
        graphics.drawString(font, amount, left - 1, top, 0xff000000, false);
        graphics.drawString(font, amount, left + 1, top, 0xff000000, false);
        graphics.drawString(font, amount, left, top - 1, 0xff000000, false);
        graphics.drawString(font, amount, left, top + 1, 0xff000000, false);
        graphics.drawString(font, amount, left, top, 0xffffffff, false);
        graphics.pose().popPose();
    }

    @EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Factories {
        @SubscribeEvent
        public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(Data.class, Display::new);
        }
        private Factories() {}
    }

    private AspectTooltip() {}
}
