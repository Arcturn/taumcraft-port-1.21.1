package thaumcraft.port.client;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookTooltipTest {
    @Test void markerRemovalPreservesInheritedStylesAcrossComponentBoundaries() {
        var source = Component.literal("@").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("@Теория ").withStyle(ChatFormatting.BOLD))
                .append(Component.literal("@@Auram").withStyle(ChatFormatting.GOLD));
        var cleaned = BookTooltip.withoutMarkers(source);
        assertEquals("Теория Auram", cleaned.getString());
        var styles = new ArrayList<Style>();
        cleaned.visit((style, text) -> {
            if (!text.isEmpty()) styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);
        assertTrue(styles.get(0).isBold());
        assertEquals(ChatFormatting.AQUA.getColor(), styles.get(0).getColor().getValue());
        assertEquals(ChatFormatting.GOLD.getColor(), styles.get(1).getColor().getValue());
        assertEquals("@@Теория @@Auram", source.getString(), "The caller's component must remain unchanged");
    }

    @Test void fullSizeTooltipFlipsAndKeepsItsFrameInsideTheViewport() {
        assertEquals(240, BookTooltip.wrapWidth(300, 320));
        var bounds = BookTooltip.bounds(303, 177, 300, 320, 180, 100, 3);
        assertEquals(191, bounds.x(), "Flip is based on the actual cursor, not the shifted anchor");
        assertEquals(145, bounds.y());
        assertEquals(30, bounds.height(), "Three full-size lines retain the two-pixel gap after the title");
        assertTrue(bounds.x() - 4 >= 0 && bounds.x() + 100 + 4 <= 320);
        assertTrue(bounds.y() - 4 >= 0 && bounds.y() + bounds.height() + 4 <= 180);
        assertEquals(4, BookTooltip.bounds(-5, -5, 0, 320, 180, 100, 1).y());
        assertEquals(92, BookTooltip.wrapWidth(95, 100), "Narrow windows must also fit the frame");
    }

    @Test void legacySubtitleDefaultsRemainAquaThenGrayWithoutOverridingBrowserColors() {
        assertEquals(0xff55ffff, BookTooltip.textColor(0, 11));
        assertEquals(0xffaaaaaa, BookTooltip.textColor(1, 11));
        assertEquals(0xffffffff, BookTooltip.textColor(0, -99));
        assertEquals(0xffffffff, BookTooltip.textColor(1, -99));
    }
}
