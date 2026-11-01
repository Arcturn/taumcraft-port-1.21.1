package thaumcraft.port;

import org.junit.jupiter.api.Test;
import thaumcraft.port.alchemy.liquid.AlchemyRules;
import thaumcraft.port.research.ResearchState;
import static org.junit.jupiter.api.Assertions.*;

class AlchemyLiquidsTest {
    @Test void wardDurationUsesPermanentWarpSquareRootAndOriginalCap() {
        assertEquals(32000, AlchemyRules.wardDuration(0));
        assertEquals(32000, AlchemyRules.wardDuration(36));
        assertEquals(28571, AlchemyRules.wardDuration(49));
        assertEquals(10000, AlchemyRules.wardDuration(400));
    }
    @Test void washingNeedsNinetySixTicksAndBonusesStack() {
        assertFalse(AlchemyRules.finishedWashing(5));
        assertTrue(AlchemyRules.finishedWashing(4));
        assertEquals(1, AlchemyRules.soapStrength(false, false));
        assertEquals(2, AlchemyRules.soapStrength(true, false));
        assertEquals(2, AlchemyRules.soapStrength(false, true));
        assertEquals(3, AlchemyRules.soapStrength(true, true));
    }
    @Test void warpEventsUseInclusiveSquareRootChanceAndQuietDown() {
        assertFalse(AlchemyRules.warpEvent(0, 100, 0));
        assertFalse(AlchemyRules.warpEvent(100, 0, 0));
        assertTrue(AlchemyRules.warpEvent(100, 100, 10));
        assertFalse(AlchemyRules.warpEvent(100, 100, 11));
        assertEquals(80, AlchemyRules.remainingCounter(100, 0));
        assertEquals(0, AlchemyRules.remainingCounter(4, 0));
    }
    @Test void OnlyPositiveWarpGainReactivatesEventsAndCleansingPreservesPermanentWarp() {
        var state = new ResearchState();
        state.warp(20, 10, 5);
        assertEquals(35, state.warpCounter());
        state.warpCounter(7);
        state.warp(20, 7, 0);
        assertEquals(7, state.warpCounter());
        state.warp(20, 8, 0);
        assertEquals(28, state.warpCounter());
        assertEquals(20, state.permanentWarp());
    }
}
