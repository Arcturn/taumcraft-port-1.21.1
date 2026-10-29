package thaumcraft.port;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import thaumcraft.port.world.AuraState;
public class AuraStateTest {
    @Test void seededGenerationIsStableAcrossUnloadReload(){for(int x=-100;x<=100;x++){var a=AuraState.generate(42,x,-x,.5);var b=AuraState.generate(42,x,-x,.5);assertEquals(a.base(),b.base());assertEquals(a.base(),a.vis());assertEquals(0,a.flux());}}
    @Test void generatedBaseNeverExceedsOriginalRange(){for(int i=0;i<1000;i++){var a=AuraState.generate(i,i,0,1.5);assertTrue(a.base()>=0&&a.base()<=500);}}
    @Test void drainsAreBoundedAndSimulationDoesNotConsume(){var a=new AuraState(250,12,8);assertEquals(12,a.drain(false,20,true));assertEquals(12,a.vis());assertEquals(12,a.drain(false,20,false));assertEquals(0,a.vis());assertEquals(8,a.drain(true,100,false));assertEquals(0,a.flux());}
    @Test void invalidEnergyCannotPoisonOrCreateAura(){var a=new AuraState(250,Float.NaN,Float.POSITIVE_INFINITY);assertEquals(0,a.vis());assertEquals(0,a.flux());a.addVis(Float.NaN);a.addFlux(-10);assertEquals(0,a.drain(false,Float.NaN,false));assertEquals(0,a.vis());assertEquals(0,a.flux());a.addFlux(40000);assertEquals(32766,a.flux());}
    @Test void diffusionMovesOneUnitAndConservesEnergy(){var a=new AuraState(250,100,80);var b=new AuraState(250,10,0);float before=a.vis()+a.flux()+b.vis()+b.flux();a.step(List.of(b),4,1,1);assertEquals(99,a.vis());assertEquals(11,b.vis());assertEquals(79,a.flux());assertEquals(1,b.flux());assertEquals(before,a.vis()+a.flux()+b.vis()+b.flux());}
    @Test void fullMoonRegeneratesButNewMoonDoesNot(){var a=new AuraState(250,100,0);var b=new AuraState(250,100,0);a.step(List.of(),0,1,1);b.step(List.of(),4,1,1);assertEquals(100.25,a.vis());assertEquals(100,b.vis());}
    @Test void fluxOccupiesRegenerationCapacity(){var a=new AuraState(100,20,100);a.step(List.of(),0,1,1);assertEquals(20,a.vis());}
    @Test void excessVisConvertsWithoutCreatingEnergy(){var a=new AuraState(100,200,0);a.step(List.of(),4,0,1);assertEquals(199.75,a.vis());assertEquals(.25,a.flux());assertEquals(200,a.vis()+a.flux());}
    @Test void riftNeedsPollutionThresholdAndSuccessfulRoll(){assertFalse(new AuraState(100,100,74).step(List.of(),2,1,0));assertTrue(new AuraState(100,100,76).step(List.of(),2,1,0));assertFalse(new AuraState(100,100,76).step(List.of(),2,1,1));}
    @Test void saturatedNeighbourCannotReceiveOverflow(){var a=new AuraState(500,100,32766);var b=new AuraState(500,100,32766);a.step(List.of(b),4,1,1);assertEquals(32766,b.flux());assertEquals(32766,a.flux());}
}
