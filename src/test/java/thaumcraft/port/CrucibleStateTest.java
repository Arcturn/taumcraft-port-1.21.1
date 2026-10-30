package thaumcraft.port;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.machine.CrucibleState;
import static org.junit.jupiter.api.Assertions.*;

class CrucibleStateTest {
    private CrucibleState boiling(){var s=new CrucibleState();s.fill(1000,false);for(int i=0;i<151;i++)s.tick(true);return s;}
    @Test void boilingRequiresWaterAndHeatStrictlyAbove150(){
        var s=new CrucibleState();for(int i=0;i<200;i++)s.tick(true);assertEquals(0,s.heat());
        s.fill(1000,false);for(int i=0;i<150;i++)s.tick(true);assertFalse(s.boiling());s.tick(true);assertTrue(s.boiling());
        s.tick(false);assertFalse(s.boiling());
    }
    @Test void paymentIsAtomicAndOneCraftUsesFiftyWater(){
        var s=boiling();s.dissolve(Map.of(Aspect.FIRE,10,Aspect.LIGHT,10));
        assertFalse(s.craft(Map.of("ignis",10,"potentia",10)));assertEquals(20,s.total());assertEquals(1000,s.water());
        assertTrue(s.craft(Map.of("ignis",10,"lux",10)));assertEquals(0,s.total());assertEquals(950,s.water());assertEquals(-250,s.timer());
    }
    @Test void pollutionPreservesTheSpecialFluxMultiplier(){
        var s=boiling();s.dissolve(Map.of(Aspect.FIRE,12,Aspect.FLUX,4));assertEquals(7,s.empty());assertEquals(0,s.water());assertEquals(0,s.total());
    }
    @Test void failedOrUnknownDissolutionDoesNotResetGracePeriod(){
        var s=boiling();s.dissolve(Map.of(Aspect.FIRE,1));assertFalse(s.dissolve(Map.of()));assertEquals(-150,s.timer());
        for(int i=0;i<249;i++)s.tick(true);assertFalse(s.spillDue());s.tick(true);assertTrue(s.spillDue());assertEquals(.25f,s.spill(0));
    }
    @Test void restoredValuesAndFluidSimulationCannotOverflow(){
        var s=new CrucibleState();s.restore(Integer.MAX_VALUE,-8,3000,Map.of(Aspect.FIRE,-1,Aspect.FLUX,Integer.MAX_VALUE));
        assertEquals(1000,s.water());assertEquals(0,s.heat());assertEquals(10000,s.total());
        assertEquals(1000,s.drain(2000,true));assertEquals(1000,s.water());assertEquals(1000,s.drain(2000,false));assertEquals(0,s.water());
        assertEquals(1000,s.fill(2000,true));assertEquals(0,s.water());
    }
    @Test void refillAndSourceRemovalPreserveThermalInertia(){
        var s=boiling();for(int i=0;i<49;i++)s.tick(true);assertEquals(200,s.heat());
        s.drain(1000,false);s.tick(true);assertEquals(199,s.heat());assertFalse(s.boiling());
        s.fill(1000,false);assertTrue(s.boiling());assertEquals(199,s.heat());
        for(int i=0;i<48;i++)s.tick(false);assertTrue(s.boiling());assertEquals(151,s.heat());
        s.tick(false);assertFalse(s.boiling());assertEquals(150,s.heat());
    }
    @Test void overflowAndPeriodicLossAreIndependentEvenWhenCold(){
        var s=new CrucibleState();s.restore(0,0,99,Map.of(Aspect.FLUX,501));s.tick(false);
        if(s.total()>500)s.spill(0);
        assertTrue(s.periodicSpillDue());s.spill(0);
        assertEquals(499,s.total());assertEquals(0,s.timer());
        assertFalse(s.periodicSpillDue());
    }
    @Test void fullAndOverflowFluidSurfacesUseOppositeSidesOfTheRim(){
        var s=new CrucibleState();s.restore(1000,200,0,Map.of());assertEquals(.8f,s.fluidHeight(),.0001f);
        s.restore(1000,200,0,Map.of(Aspect.FIRE,500));assertEquals(.9999f,s.fluidHeight(),.00001f);
        s.restore(1000,200,0,Map.of(Aspect.FIRE,501));assertEquals(1.001f,s.fluidHeight(),.00001f);
    }
    @Test void comparatorMeasuresAspectLoadAndClampsAtRedstoneMaximum(){
        var s=new CrucibleState();assertEquals(0,s.comparatorSignal());
        s.restore(1000,200,0,Map.of(Aspect.FIRE,1));assertEquals(1,s.comparatorSignal());
        s.restore(1000,200,0,Map.of(Aspect.FIRE,250));assertEquals(8,s.comparatorSignal());
        s.restore(1000,200,0,Map.of(Aspect.FIRE,600));assertEquals(15,s.comparatorSignal());
    }
    @Test void oneFinalCraftMayConsumeLessThanFiftyWaterAsInTc6(){
        var s=new CrucibleState();s.restore(1,151,-100,Map.of(Aspect.FIRE,1));
        assertTrue(s.craft(Map.of("ignis",1)));assertEquals(0,s.water());assertFalse(s.boiling());
    }
}
