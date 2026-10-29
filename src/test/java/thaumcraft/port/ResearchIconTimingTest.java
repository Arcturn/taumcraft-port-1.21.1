package thaumcraft.port;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.port.research.ResearchIconTiming;

class ResearchIconTimingTest {
    @Test void changesOnSecondBoundaryAndWraps() {
        assertEquals(0,ResearchIconTiming.frame(3,999));
        assertEquals(1,ResearchIconTiming.frame(3,1000));
        assertEquals(2,ResearchIconTiming.frame(3,2999));
        assertEquals(0,ResearchIconTiming.frame(3,3000));
    }
    @Test void sixCrystalVariantsRemainReachableAtRealWallClockTimes() {
        assertEquals(5,ResearchIconTiming.frame(6,5999));
        assertEquals(1,ResearchIconTiming.frame(6,1730000005000L));
        assertEquals(0,ResearchIconTiming.frame(1,1730000005000L));
    }
    @Test void emptySequenceCannotBecomeAValidIcon() {
        assertThrows(IllegalArgumentException.class,()->ResearchIconTiming.frame(0,1000));
        assertThrows(IllegalArgumentException.class,()->ResearchIconTiming.frame(-1,1000));
    }
}
