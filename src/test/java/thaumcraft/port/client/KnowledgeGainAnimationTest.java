package thaumcraft.port.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGainAnimationTest {
    @Test void observationAppearsInLowerRightQuadrantAndPulsesBeforeFlying() {
        var start = KnowledgeGainAnimation.frame(640, 360, 50, 50, 0, 621);
        var pulse = KnowledgeGainAnimation.frame(640, 360, 50, 42, .25f, 621);
        assertEquals(0, start.size(), .0001);
        assertTrue(start.x() >= 437 && start.x() <= 475);
        assertTrue(start.y() >= 197 && start.y() <= 235);
        assertEquals(start.x(), pulse.x());
        assertEquals(start.y(), pulse.y());
        assertEquals(32, pulse.size(), .0001, "Peak is twice the original 16-pixel icon");
        assertNull(pulse.flash(), "Birth flash ends before the main pulse reaches its peak");
    }

    @Test void secondPhaseShrinksAndCurvesIntoTheBottomRightBook() {
        var start = KnowledgeGainAnimation.frame(640, 360, 50, 50, 0, 621);
        var halfway = KnowledgeGainAnimation.frame(640, 360, 50, 17, .5f, 621);
        var end = KnowledgeGainAnimation.frame(640, 360, 50, 0, 0, 621);
        assertEquals(8, halfway.size(), .0001);
        assertEquals(0, end.size(), .0001);
        assertTrue(end.x() >= 628 && end.x() <= 635);
        assertTrue(end.y() >= 348 && end.y() <= 355);
        assertEquals((start.y() + end.y()) / 2, halfway.y(), .0001);
        assertEquals((end.x() - start.x()) * Math.sqrt(.5), end.x() - halfway.x(), .0001);
        assertTrue(halfway.x() > start.x() && halfway.x() < end.x());
        assertTrue(halfway.y() > start.y() && halfway.y() < end.y());
    }

    @Test void BirthAndArrivalUseTheOriginalSparkleStripWithDifferentPeakSizes() {
        var birth = KnowledgeGainAnimation.frame(640, 360, 50, 48, .5f, 621).flash();
        var arrival = KnowledgeGainAnimation.frame(640, 360, 50, 3, .5f, 621).flash();
        assertNotNull(birth);
        assertNotNull(arrival);
        assertEquals(32, birth.size(), .0001);
        assertEquals(16, arrival.size(), .0001);
        assertTrue(birth.frame() >= 320 && birth.frame() < 336);
        assertTrue(birth.green() >= 189 / 255f && birth.green() <= 1);
        assertTrue(birth.blue() >= 64 / 255f && birth.blue() <= 1);
        assertEquals(birth.frame(), arrival.frame());
        assertEquals(birth.rotation(), arrival.rotation());
        assertEquals(birth.green(), arrival.green());
        assertEquals(birth.blue(), arrival.blue());
    }

    @Test void SeedKeepsMotionStableAndCorrectsTheOriginalRotatedQuadUvs() {
        for (long seed = 0; seed < 128; seed++) {
            var start = KnowledgeGainAnimation.frame(640, 360, 50, 50, 0, seed);
            var middle = KnowledgeGainAnimation.frame(640, 360, 50, 25, .4f, seed);
            assertTrue(start.rotation() >= -5 && start.rotation() <= 6,
                    "The glyph should be upright with a slight tilt, not rotated sideways");
            assertEquals(start.rotation(), middle.rotation());
            assertEquals(middle, KnowledgeGainAnimation.frame(640, 360, 50, 25, .4f, seed));
        }
    }
}
