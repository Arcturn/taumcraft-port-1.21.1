package thaumcraft.port.warp;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class WarpRulesTest {
    @Test void onlyPositiveTwoThousandTickBoundariesRunAndWardOrConfigFreezeChecks() {
        assertFalse(WarpRules.isCheckDue(0, false, false));
        assertFalse(WarpRules.isCheckDue(-2000, false, false));
        assertFalse(WarpRules.isCheckDue(1999, false, false));
        assertTrue(WarpRules.isCheckDue(2000, false, false));
        assertFalse(WarpRules.isCheckDue(2001, false, false));
        assertTrue(WarpRules.isCheckDue(4000, false, false));
        assertFalse(WarpRules.isCheckDue(2000, true, false));
        assertFalse(WarpRules.isCheckDue(2000, false, true));
        assertFalse(WarpRules.isCheckDue(2000, true, true));
    }

    @Test void inclusiveChanceGivesTwoOfOneHundredRollsEvenAtCounterOne() {
        assertEquals(2, successfulRolls(1));
        assertEquals(2, successfulRolls(2));
        assertEquals(10, successfulRolls(99));
        assertEquals(11, successfulRolls(100));
        assertEquals(100, successfulRolls(10000));
        assertEquals(100, successfulRolls(30000));
        assertFalse(WarpRules.canTrigger(0, 100, 0), "Gear alone cannot restart an exhausted counter");
        assertFalse(WarpRules.canTrigger(100, 0, 0), "A saved counter alone cannot trigger events");
    }

    private static long successfulRolls(int counter) {
        return IntStream.range(0, 100).filter(roll -> WarpRules.canTrigger(counter, 1, roll)).count();
    }

    @Test void severityUsesTwoPartsCurrentWarpOnePartOldCounterAndAnExclusiveBound() {
        assertEquals(1, WarpRules.severityRange(1, 1));
        assertEquals(3, WarpRules.severityRange(2, 5));
        assertEquals(20, WarpRules.severityRange(10, 40));
        // Five gear warp and a residual counter can trigger a range of 36, not 5 or 100.
        assertEquals(36, WarpRules.severityRange(5, 100));
        assertEquals(100, WarpRules.severityRange(1, 30000));
        assertEquals(100, WarpRules.severityRange(30000, 1));
        assertEquals(100, WarpRules.severityRange(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(0, WarpRules.severityRange(0, 0));
    }

    @Test void gearSlowsCounterDecayButCannotRemoveItsFivePointMinimum() {
        assertEquals(80, WarpRules.remainingCounter(100, 0));
        assertEquals(90, WarpRules.remainingCounter(100, 5));
        assertEquals(95, WarpRules.remainingCounter(100, 100));
        assertEquals(35, WarpRules.remainingCounter(50, 0), "Truncate the result, not the square root");
        assertEquals(41, WarpRules.remainingCounter(50, 3));
        assertEquals(29653, WarpRules.remainingCounter(30000, 0));
        assertEquals(0, WarpRules.remainingCounter(1, 0));
        assertEquals(0, WarpRules.remainingCounter(4, 100));
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 0, NONE", "1, 4, CREEPER", "5, 8, EXPLOSION", "9, 12, UNEASE",
            "13, 16, VIS_EXHAUST", "17, 20, THAUMARHIA", "21, 24, HUNGER", "25, 28, WHISPERS",
            "29, 32, MIST_SINGLE", "33, 36, BLURRED_VISION", "37, 40, SUN_SCORNED", "41, 44, MINING_FATIGUE",
            "45, 48, INFECTIOUS", "49, 52, NIGHT_VISION", "53, 56, DEATH_GAZE", "57, 60, SPIDERS",
            "61, 64, STARES", "65, 68, MIST_MEDIUM", "69, 72, BLINDNESS", "73, 75, HUNGER_LONG",
            "76, 76, CLARITY", "77, 80, HUNGER_LONG", "81, 88, PORTAL", "89, 92, REAL_SPIDERS",
            "93, 200, MIST_STRONG"
    })
    void everySourceSeverityBandIncludesBothEndpoints(int first, int last, WarpRules.Event expected) {
        for (int severity = first; severity <= last; severity++)
            assertEquals(expected, WarpRules.eventForSeverity(severity), "severity " + severity);
    }

    @Test void zeroSeverityStillHasNoEffectAndGearCanPushBeyondTheHundredPointStrengthCap() {
        assertEquals(WarpRules.Event.NONE, WarpRules.eventForSeverity(Integer.MIN_VALUE));
        assertEquals(WarpRules.Event.NONE, WarpRules.eventForSeverity(0));
        assertEquals(WarpRules.Event.MIST_STRONG, WarpRules.eventForSeverity(Integer.MAX_VALUE));
        // The original equality deliberately gives clarity one roll, with hunger on BOTH sides.
        assertEquals(1, IntStream.range(0, 100).filter(n -> WarpRules.eventForSeverity(n) == WarpRules.Event.CLARITY).count());
        assertEquals(7, IntStream.range(0, 100).filter(n -> WarpRules.eventForSeverity(n) == WarpRules.Event.HUNGER_LONG).count());
        assertEquals(8, IntStream.range(0, 100).filter(n -> WarpRules.eventForSeverity(n) == WarpRules.Event.PORTAL).count());
    }
}
