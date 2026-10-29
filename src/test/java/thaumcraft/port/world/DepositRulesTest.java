package thaumcraft.port.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DepositRulesTest {
    @Test void originalUpperCountsUseNearestRoundingAtEverySupportedDensity() {
        for (int density=1; density<=500; density++) {
            float originalDensity=density/100.0f;
            for (int base:new int[]{8,18,20,64})
                assertEquals(Math.round(base*originalDensity),DepositRules.legacyCount(base,density));
            assertEquals(Math.round(18*originalDensity),DepositRules.upperOre(51,-64,320,density).attempts());
            assertEquals(Math.round(18*originalDensity),DepositRules.upperOre(64,-64,320,density).attempts());
        }
        assertEquals(0,DepositRules.legacyCount(18,1));
        assertEquals(1,DepositRules.legacyCount(18,3));
    }

    @Test void modernDepthAddsDensityWithoutDilutingOriginalUpperBands() {
        var cinnabar=DepositRules.upperOre(51,-64,320,100);
        var deepCinnabar=DepositRules.lowerOre(51,-64,320,100);
        var quartz=DepositRules.upperOre(64,-64,320,100);
        var deepQuartz=DepositRules.lowerOre(64,-64,320,100);
        assertEquals(new DepositRules.Band(0,51,18,0),cinnabar);
        assertEquals(new DepositRules.Band(-64,0,23,0),deepCinnabar);
        assertEquals(new DepositRules.Band(0,64,18,0),quartz);
        assertEquals(new DepositRules.Band(-64,0,18,0),deepQuartz);
        assertTrue(Math.abs(deepCinnabar.attempts()/64.0-18/51.0)<0.01);
        assertEquals(18/64.0,deepQuartz.attempts()/64.0);
        assertEquals(20,DepositRules.legacyCount(20,100));
    }

    @Test void oldHeightWorldAddsNoDeepAttemptsAndShortWorldClipsTheBands() {
        assertEquals(new DepositRules.Band(0,51,18,0),DepositRules.upperOre(51,0,256,100));
        assertEquals(0,DepositRules.lowerOre(51,0,256,100).attempts());
        assertEquals(0,DepositRules.lowerOre(64,16,128,100).layers());
        assertEquals(new DepositRules.Band(16,32,6,0),DepositRules.upperOre(51,16,32,100));
        assertEquals(0,DepositRules.upperOre(51,64,128,100).attempts());
        assertEquals(0,DepositRules.upperOre(51,-64,-16,100).layers());
        assertEquals(new DepositRules.Band(-64,-16,17,0),DepositRules.lowerOre(51,-64,-16,100));
    }

    @Test void crystalLowerBandScalesAttemptsAndBudgetTogether() {
        assertEquals(new DepositRules.Band(0,59,8,64),DepositRules.upperCrystals(-64,320,64,100));
        assertEquals(new DepositRules.Band(0,59,8,64),DepositRules.upperCrystals(0,256,64,100));
        assertEquals(new DepositRules.Band(16,59,6,47),DepositRules.upperCrystals(16,128,64,100));
        assertEquals(new DepositRules.Band(16,32,2,17),DepositRules.upperCrystals(16,32,64,100));
        assertEquals(0,DepositRules.upperCrystals(80,128,64,100).attempts());
        assertEquals(new DepositRules.Band(-63,0,9,68),DepositRules.lowerCrystals(-64,320,64,100));
        assertEquals(new DepositRules.Band(-63,0,4,34),DepositRules.lowerCrystals(-64,320,64,50));
        assertEquals(new DepositRules.Band(-63,0,17,137),DepositRules.lowerCrystals(-64,320,64,200));
        assertEquals(0,DepositRules.lowerCrystals(0,256,64,100).attempts());
        assertEquals(0,DepositRules.lowerCrystals(-64,320,-64,100).attempts());
        var lowSurface=DepositRules.lowerCrystals(-64,320,-20,100);
        assertEquals(-25,lowSurface.maxYExclusive());
        assertEquals(-63,lowSurface.minY());
        assertEquals(38,lowSurface.layers());
    }
}
