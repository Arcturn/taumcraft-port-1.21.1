package thaumcraft.port;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.port.research.ResearchBrowserLayout;

class ResearchBrowserLayoutTest {
    @Test void boundingBoxCentersSparseAndDenseCategoriesTheSameWay() {
        var bounds = ResearchBrowserLayout.bounds(-3, 5, 640);
        assertEquals(-632, bounds.minimum());
        assertEquals(96, bounds.maximum());
        assertEquals(-268, bounds.center());
        double leftNode = 24 - 3 * 24 - bounds.center();
        double rightNode = 24 + 5 * 24 - bounds.center();
        assertEquals(316, (leftNode + rightNode) / 2,
                "Legacy framing centers extrema four pixels left of screen center");
    }

    @Test void draggingCannotLeaveTheLegacyBoundsAtEitherZoom() {
        var bounds = ResearchBrowserLayout.bounds(-3, 5, 640);
        assertEquals(-632, bounds.clamp(-10000, 1));
        assertEquals(95, bounds.clamp(10000, 1));
        assertEquals(-1264, bounds.clamp(-10000, 2));
        assertEquals(191, bounds.clamp(10000, 2));
        assertEquals(-300, bounds.clamp(-300, 2));
    }

    @Test void changingZoomPreservesLegacyMapOrigin() {
        double origin = -268;
        for (int screenExtent : new int[]{427, 640, 641}) {
            for (float zoom : new float[]{1, 1.25f, 1.5f, 1.75f, 2}) {
                double pan = ResearchBrowserLayout.pan(origin, screenExtent, zoom);
                assertEquals(origin, ResearchBrowserLayout.origin(pan, screenExtent, zoom));
                for (int grid : new int[]{-10, 0, 5, 17}) {
                    // GuiResearchBrowser places the icon at 24 + grid*24 - locX before GL scale(1/zoom).
                    // Compare that exact coordinate: rearranged post-division doubles can differ by one ULP.
                    double legacyCoordinate = 24 + grid * 24 - origin;
                    double portCoordinate = screenExtent * zoom / 2.0 + grid * 24 + pan;
                    assertEquals(legacyCoordinate, portCoordinate);
                }
            }
        }
    }

    @Test void renderingKeepsLegacyIntegerCoordinatesAtFractionalZoomAndOddScreenSizes() {
        var bounds = ResearchBrowserLayout.bounds(-3, 5, 641);
        assertEquals(-791, bounds.display(-10000, 1.25f));
        assertEquals(119, bounds.display(10000, 1.25f));
        assertEquals(-301, bounds.display(-300.2, 1.25f));
    }

    @Test void onlyUnlockableIncompleteFramesPulseOnTheSixHundredMillisecondCycle() {
        assertEquals(.75f, ResearchBrowserLayout.frameBrightness(false, true, 0), .00001f);
        assertEquals(1, ResearchBrowserLayout.frameBrightness(false, true, 150), .00001f);
        assertEquals(.5f, ResearchBrowserLayout.frameBrightness(false, true, 450), .00001f);
        assertEquals(.75f, ResearchBrowserLayout.frameBrightness(false, true, 600), .00001f);
        assertEquals(1, ResearchBrowserLayout.frameBrightness(true, true, 450));
        assertEquals(.3f, ResearchBrowserLayout.frameBrightness(false, false, 150));
    }

    @Test void panningEasesOnTicksAndInterpolatesBetweenThem() {
        var motion = new ResearchBrowserLayout.Motion();
        motion.aim(10, 0);
        motion.tick();
        assertEquals(8.5, motion.current().x());
        assertEquals(0, motion.interpolate(0).x());
        assertEquals(4.25, motion.interpolate(.5f).x());
        motion.tick();
        assertEquals(10, motion.current().x(), "The remaining distance is below two pixels");
        motion.tick();
        assertEquals(10, motion.interpolate(.5f).x(), "The settled map must not drift");
    }

    @Test void settlingThresholdUsesTheCombinedDistanceOfBothAxes() {
        var motion = new ResearchBrowserLayout.Motion();
        motion.aim(1.5, 1.5);
        motion.tick();
        assertEquals(1.275, motion.current().x(), .0000001);
        assertEquals(1.275, motion.current().y(), .0000001);
        motion.tick();
        assertEquals(new ResearchBrowserLayout.Position(1.5, 1.5), motion.current());
    }

    @Test void directDraggingHasNoInventedInputLagAndCancelsOldTarget() {
        var motion = new ResearchBrowserLayout.Motion();
        motion.snap(-50, -60);
        motion.aim(0, 0);
        motion.drag(-12, 8);
        var dragged = new ResearchBrowserLayout.Position(-62, -52);
        assertEquals(dragged, motion.interpolate(0));
        assertEquals(dragged, motion.interpolate(.5f));
        motion.tick();
        assertEquals(dragged, motion.current());
    }
}
