package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** TC6's tooltip frame and placement, with the current Minecraft font at readable, full size. */
public final class BookTooltip {
    private static final int BACKGROUND = 0xf0100010;
    private static final int BORDER_TOP = 0x505000ff;
    private static final int BORDER_BOTTOM = 0x5028007f;

    private BookTooltip() {}

    /**
     * Anchors have TC6 UtilsFX semantics: text starts at anchor + (12, -12).
     * actualMouseX separately controls wrapping/flipping, before the caller's anchor offset.
     * subTipColor is a legacy color index (0..15), or -99 to retain the supplied colors.
     */
    public static void render(GuiGraphics graphics, List<Component> content, int anchorX, int anchorY,
                              int actualMouseX, int screenWidth, int screenHeight, int subTipColor) {
        if (content.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        int maximum = wrapWidth(actualMouseX, screenWidth);
        var lines = new ArrayList<FormattedCharSequence>();
        int widest = 0;
        for (Component component : content) {
            // Native splitting retains styles, resource-pack metrics and fractional Unicode advances.
            var wrapped = font.split(withoutMarkers(component), maximum);
            if (wrapped.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else for (var line : wrapped) {
                lines.add(line);
                widest = Math.max(widest, font.width(line));
            }
        }
        var bounds = bounds(anchorX, anchorY, actualMouseX, screenWidth, screenHeight, widest, lines.size());
        int x = bounds.x(), y = bounds.y(), total = bounds.height();
        graphics.flush();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        graphics.setColor(1, 1, 1, 1);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 400);
            graphics.fill(x - 3, y - 4, x + widest + 3, y - 3, BACKGROUND);
            graphics.fill(x - 3, y + total + 3, x + widest + 3, y + total + 4, BACKGROUND);
            graphics.fill(x - 3, y - 3, x + widest + 3, y + total + 3, BACKGROUND);
            graphics.fill(x - 4, y - 3, x - 3, y + total + 3, BACKGROUND);
            graphics.fill(x + widest + 3, y - 3, x + widest + 4, y + total + 3, BACKGROUND);
            graphics.fillGradient(x - 3, y - 2, x - 2, y + total + 2, BORDER_TOP, BORDER_BOTTOM);
            graphics.fillGradient(x + widest + 2, y - 2, x + widest + 3, y + total + 2, BORDER_TOP, BORDER_BOTTOM);
            graphics.fill(x - 3, y - 3, x + widest + 3, y - 2, BORDER_TOP);
            graphics.fill(x - 3, y + total + 2, x + widest + 3, y + total + 3, BORDER_BOTTOM);
            graphics.pose().translate(0, 0, 1);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(font, lines.get(i), x, y, textColor(i, subTipColor), true);
                y += 10;
                if (i == 0) y += 2;
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            graphics.setColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    }

    static int wrapWidth(int actualMouseX, int screenWidth) {
        int available = screenWidth - actualMouseX - 24;
        int maximum = available < 120 ? 240 : Math.min(240, available);
        return Math.max(1, Math.min(maximum, screenWidth - 8));
    }

    static Bounds bounds(int anchorX, int anchorY, int actualMouseX, int screenWidth, int screenHeight,
                         int widest, int lineCount) {
        int total = lineCount * 10 - 2 + (lineCount > 1 ? 2 : 0);
        int x = anchorX + 12;
        if (screenWidth - actualMouseX - 24 < 120) x -= widest + 24;
        x = Math.max(4, Math.min(x, screenWidth - widest - 4));
        // Keep the original five-pixel lower margin, and protect the upper edge as well.
        int y = Math.max(4, Math.min(anchorY - 12, screenHeight - total - 5));
        return new Bounds(x, y, total);
    }

    static int textColor(int visualLine, int subTipColor) {
        if (subTipColor == -99) return 0xffffffff;
        ChatFormatting color = ChatFormatting.getById(visualLine == 0 ? subTipColor : 7);
        return color == null || color.getColor() == null ? 0xffffffff : 0xff000000 | color.getColor();
    }

    /** Remove legacy @@ markers without discarding component styles or translated text. */
    static Component withoutMarkers(Component component) {
        String plain = component.getString();
        int first = plain.indexOf("@@");
        if (first < 0) return component;
        var removed = new BitSet(plain.length());
        for (int i = first; i >= 0; i = plain.indexOf("@@", i + 2)) removed.set(i, i + 2);
        var result = Component.empty();
        int[] offset = {0};
        component.visit((style, text) -> {
            var clean = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) if (!removed.get(offset[0] + i)) clean.append(text.charAt(i));
            offset[0] += text.length();
            if (!clean.isEmpty()) result.append(Component.literal(clean.toString()).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    record Bounds(int x, int y, int height) {}
}
