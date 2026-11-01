package thaumcraft.port.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThaumatoriumLayoutTest {
    @Test void oddRecipeCountsRemainReachableWithoutScrollingPastTheLastRow() {
        for (int size = 0; size <= 40; size++) {
            int last = ThaumatoriumLayout.maxRow(size);
            assertEquals(Math.max(0, (size + 1) / 2 - 3), last);
            assertEquals(last, ThaumatoriumLayout.clampRow(100, size));
            assertEquals(0, ThaumatoriumLayout.clampRow(-1, size));
            if (size > 0) {
                int visible = size - 1 - last * 2;
                assertTrue(visible >= 0 && visible < 6);
                assertEquals(size - 1, ThaumatoriumLayout.recipeAt(48 + visible % 2 * 16,
                        56 + visible / 2 * 16, last, size));
            }
        }
    }

    @Test void recipeHitAreasDoNotConsumeCatalystOrArrowClicksAndSkipEmptyLastCells() {
        assertEquals(-1, ThaumatoriumLayout.recipeAt(55, 24, 0, 7));
        assertEquals(-1, ThaumatoriumLayout.recipeAt(82, 56, 0, 7));
        assertEquals(-1, ThaumatoriumLayout.recipeAt(82, 93, 0, 7));
        assertEquals(-1, ThaumatoriumLayout.recipeAt(47.999, 56, 0, 7));
        assertEquals(-1, ThaumatoriumLayout.recipeAt(48, 104, 0, 7));
        assertEquals(0, ThaumatoriumLayout.recipeAt(48, 56, 0, 7));
        assertEquals(1, ThaumatoriumLayout.recipeAt(64, 56, 0, 7));
        assertEquals(6, ThaumatoriumLayout.recipeAt(48, 88, 1, 7));
        assertEquals(-1, ThaumatoriumLayout.recipeAt(64, 88, 1, 7));
    }

    @Test void selectedRecipeCyclesOnceASecondAndProgressUsesTheOriginalTwelvePixels() {
        assertEquals(-1, ThaumatoriumLayout.preview(1000, 0));
        assertEquals(0, ThaumatoriumLayout.preview(999, 3));
        assertEquals(1, ThaumatoriumLayout.preview(1000, 3));
        assertEquals(2, ThaumatoriumLayout.preview(2999, 3));
        assertEquals(0, ThaumatoriumLayout.preview(3000, 3));
        assertEquals(0, ThaumatoriumLayout.progress(0, 10));
        assertEquals(1, ThaumatoriumLayout.progress(1, 10));
        assertEquals(6, ThaumatoriumLayout.progress(5, 10));
        assertEquals(12, ThaumatoriumLayout.progress(10, 10));
        // TC6 preserves accumulated essentia across recipe changes, including temporary overfill.
        assertEquals(24, ThaumatoriumLayout.progress(20, 10));
    }
}
