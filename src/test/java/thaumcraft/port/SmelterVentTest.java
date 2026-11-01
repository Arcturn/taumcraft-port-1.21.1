package thaumcraft.port;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.port.essentia.SmelterUpgrades;
import static org.junit.jupiter.api.Assertions.*;

final class SmelterVentTest {
    @Test void onlyTheVentThatRemovesEachFluxUnitReceivesAnEffect(){
        var effects=new ArrayList<Integer>();
        double[] rolls={.9,.1,.9,.9,.2,.8,.9,.9,.1};int[] next={0};
        int pollution=SmelterUpgrades.pollution(4,3,()->rolls[next[0]++],effects::add);
        assertEquals(1,pollution);
        assertEquals(List.of(1,2,0),effects);
        assertEquals(9,next[0]);
    }
    @Test void noVentEffectIsEmittedForUnfilteredPollution(){
        var effects=new ArrayList<Integer>();
        assertEquals(6,SmelterUpgrades.pollution(6,3,()->.333,effects::add));
        assertTrue(effects.isEmpty());
    }
}
