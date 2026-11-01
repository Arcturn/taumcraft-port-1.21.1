package thaumcraft.port.alchemy.spa.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaLayoutTest {
    @Test void gaugeRetainsOriginalFortyEightPixelFiveBucketScale() {
        assertEquals(48, SpaLayout.coveredPixels(0));
        assertEquals(38, SpaLayout.coveredPixels(1000));
        assertEquals(24, SpaLayout.coveredPixels(2500));
        assertEquals(0, SpaLayout.coveredPixels(5000));
        assertEquals(0, SpaLayout.coveredPixels(6000));
        assertEquals(48, SpaLayout.coveredPixels(-100));
    }

    @Test void switchHasSeparateOriginalTooltipAndButtonBounds() {
        assertTrue(SpaLayout.toggle(89, 35));
        assertTrue(SpaLayout.toggle(96.99, 42.99));
        assertFalse(SpaLayout.toggle(97, 43));
        assertFalse(SpaLayout.toggle(88, 34));
        assertTrue(SpaLayout.toggleTooltip(88, 34));
        assertFalse(SpaLayout.toggleTooltip(98, 44));
        assertFalse(SpaLayout.toggle(65, 31)); // Salts slot must remain clickable.
        assertTrue(SpaLayout.fluidTooltip(104, 10));
        assertTrue(SpaLayout.fluidTooltip(113.99, 64.99));
        assertFalse(SpaLayout.fluidTooltip(114, 65));
    }
}
