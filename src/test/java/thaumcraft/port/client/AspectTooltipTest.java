package thaumcraft.port.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;

import static org.junit.jupiter.api.Assertions.*;

class AspectTooltipTest {
    @Test void shiftShowsAllItemAspectsOnlyInUncapturedInventoryScreens() {
        assertTrue(AspectTooltip.visible(true, false, true, false));
        assertFalse(AspectTooltip.visible(true, false, false, false));
        for (boolean shift : new boolean[]{false, true}) {
            for (boolean showTags : new boolean[]{false, true}) {
                assertFalse(AspectTooltip.visible(false, false, shift, showTags));
                assertFalse(AspectTooltip.visible(true, true, shift, showTags));
            }
        }
    }

    @Test void originalShowTagsOptionInvertsShiftRatherThanRequiringResearch() {
        assertTrue(AspectTooltip.visible(true, false, false, true));
        assertFalse(AspectTooltip.visible(true, false, true, true));
    }

    @Test void amountSortIsStableAndKeepsTheOriginalSixteenPixelRowGeometry() {
        var amounts = new LinkedHashMap<Aspect, Integer>();
        amounts.put(Aspect.FIRE, 3);
        amounts.put(Aspect.EARTH, 7);
        amounts.put(Aspect.WATER, 3);
        amounts.put(Aspect.AIR, 0);
        amounts.put(Aspect.ENTROPY, -1);
        var data = AspectTooltip.layout(amounts, 300);
        assertEquals(java.util.List.of(Aspect.EARTH, Aspect.FIRE, Aspect.WATER),
                data.entries().stream().map(AspectTooltip.Entry::aspect).toList());
        assertEquals(java.util.List.of(7, 3, 3), data.entries().stream().map(AspectTooltip.Entry::amount).toList());
        assertEquals(54, data.width());
        assertEquals(18, data.height());
        assertEquals(16, AspectTooltip.ICON_SIZE);
        assertEquals(18, AspectTooltip.PITCH);
        assertEquals(5, amounts.size(), "Rendering must not alter the aspect calculation result");
    }

    @Test void longModdedAspectRowsWrapWithoutLosingEntriesOrExceedingWidth() {
        var amounts = new LinkedHashMap<Aspect, Integer>();
        for (var aspect : Aspect.values()) amounts.put(aspect, 1);
        var data = AspectTooltip.layout(amounts, 100);
        assertEquals(37, data.entries().size());
        assertEquals(5, data.columns());
        assertEquals(8, data.rows());
        assertEquals(90, data.width());
        assertEquals(144, data.height());
        for (int i = 0; i < data.entries().size(); i++) {
            assertTrue(i % data.columns() * AspectTooltip.PITCH + AspectTooltip.ICON_SIZE <= 100);
            assertTrue(i / data.columns() * AspectTooltip.PITCH + 2 + AspectTooltip.ICON_SIZE <= data.height());
        }
    }

    @Test void emptyRowsReserveNoSpaceAndTooltipDataCannotBeMutatedAfterGathering() {
        var empty = AspectTooltip.layout(Map.of(), 320);
        assertEquals(0, empty.width());
        assertEquals(0, empty.height());
        var mutable = new ArrayList<AspectTooltip.Entry>();
        mutable.add(new AspectTooltip.Entry(Aspect.AIR, 2));
        var data = new AspectTooltip.Data(mutable, 100);
        mutable.clear();
        assertEquals(1, data.entries().size());
        assertEquals(1, data.columns());
        assertThrows(UnsupportedOperationException.class, () -> data.entries().clear());
        var display = new AspectTooltip.Display(data);
        assertEquals(18, display.getWidth(null));
        assertEquals(18, display.getHeight());
    }
}
