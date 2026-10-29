package thaumcraft.port.client;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.network.JournalPayload;

/** Initial scan journal; deliberately does not pretend to implement the old research tree. */
public final class ThaumonomiconScreen extends Screen {
    private final int scannedCount;
    private final List<Aspect> discovered;
    private int page;
    private static final int PER_PAGE = 6;

    public ThaumonomiconScreen(JournalPayload payload) {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        scannedCount = payload.scannedCount();
        discovered = Arrays.stream(payload.discovered().split(","))
                .map(Aspect::byId).flatMap(java.util.Optional::stream).toList();
    }

    @Override
    protected void init() {
        int y = Math.max(8, height / 2 - 108) + 188;
        addRenderableWidget(Button.builder(Component.literal("←"), button -> {
            if (page > 0) page--;
        }).bounds(width / 2 - 125, y, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 55, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), button -> {
            if ((page + 1) * PER_PAGE < discovered.size()) page++;
        }).bounds(width / 2 + 85, y, 40, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 150;
        int top = Math.max(8, height / 2 - 108);
        graphics.fill(left - 2, top - 2, left + 302, top + 218, 0xFF583D2B);
        graphics.fill(left, top, left + 300, top + 216, 0xFFE2CF9E);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFF3F241B);
        graphics.drawString(font, Component.translatable("thaumcraft.port.journal.count", scannedCount),
                left + 15, top + 29, 0xFF3F241B, false);
        if (discovered.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("thaumcraft.port.journal.empty"),
                    left + 15, top + 55, 270, 0xFF3F241B);
        }
        for (int i = page * PER_PAGE; i < Math.min(discovered.size(), (page + 1) * PER_PAGE); i++) {
            Aspect aspect = discovered.get(i);
            int y = top + 49 + (i % PER_PAGE) * 19;
            graphics.blit(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + aspect.id() + ".png"),
                    left + 15, y, 0, 0, 16, 16, 16, 16);
            graphics.drawString(font, Component.literal(aspect.displayName()), left + 38, y + 4, 0xFF3F241B, false);
        }
        graphics.drawWordWrap(font, Component.translatable("thaumcraft.port.journal.stage"),
                left + 15, top + 163, 270, 0xFF6F4A39);
        for(var widget:renderables)widget.render(graphics,mouseX,mouseY,partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
