package thaumcraft.port.alchemy.devices;

import java.util.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;

class SprayerStateTest {
    @Test void completeCostsEveryAspectAndRetainsTheSelectedTemplate(){
        var state=new SprayerState();var recipe=Map.of(Aspect.FIRE,2,Aspect.WATER,3);state.select(recipe);
        assertEquals(Aspect.WATER,state.needed());assertFalse(state.complete());
        assertEquals(2,state.insert(Aspect.FIRE,99,false));assertFalse(state.complete());
        assertEquals(3,state.insert(Aspect.WATER,3,false));assertTrue(state.complete());
        assertEquals(1,state.charges());assertTrue(state.progress().isEmpty());assertEquals(recipe,state.recipe());
        assertTrue(state.spray());assertFalse(state.spray());assertEquals(recipe,state.recipe());
    }
    @Test void simulationWrongIngredientsAndExcessCannotSpendOrOverfill(){
        var state=new SprayerState();state.select(Map.of(Aspect.ALCHEMY,5));
        assertEquals(0,state.insert(Aspect.WATER,50,false));assertEquals(0,state.insert(Aspect.ALCHEMY,-1,false));
        assertEquals(5,state.insert(Aspect.ALCHEMY,20,true));assertTrue(state.progress().isEmpty());
        assertEquals(3,state.insert(Aspect.ALCHEMY,3,false));assertEquals(2,state.insert(Aspect.ALCHEMY,30,false));
        assertEquals(0,state.insert(Aspect.ALCHEMY,1,false));assertEquals(5,state.progress().get(Aspect.ALCHEMY));
    }
    @Test void eighthChargeStopsConsumptionUntilSprayed(){
        var state=new SprayerState();state.select(Map.of(Aspect.WATER,1));
        for(int i=0;i<8;i++){assertEquals(1,state.insert(Aspect.WATER,1,false));assertTrue(state.complete());}
        assertEquals(8,state.charges());assertEquals(0,state.insert(Aspect.WATER,1,false));assertFalse(state.complete());
        assertTrue(state.spray());assertEquals(1,state.insert(Aspect.WATER,1,false));assertTrue(state.complete());assertEquals(8,state.charges());
    }
    @Test void replacingTemplateResetsChargesAndPartialEssentia(){
        var state=new SprayerState();state.restore(Map.of(Aspect.FIRE,5),Map.of(Aspect.FIRE,4),7);state.select(Map.of(Aspect.WATER,9));
        assertEquals(0,state.charges());assertTrue(state.progress().isEmpty());assertEquals(Aspect.WATER,state.needed());
        state.select(Map.of());assertFalse(state.complete());assertFalse(state.spray());
    }
    @Test void saveValuesAreBoundedAndCannotInjectUnrequestedEssentia(){
        var state=new SprayerState();state.restore(Map.of(Aspect.FIRE,5,Aspect.WATER,10),Map.of(Aspect.FIRE,100,Aspect.WATER,-4,Aspect.LIFE,9),100);
        assertEquals(8,state.charges());assertEquals(Map.of(Aspect.FIRE,5),state.progress());
        state.restore(Map.of(),Map.of(Aspect.WATER,2),-10);assertEquals(0,state.charges());assertTrue(state.progress().isEmpty());
    }
    @Test void cullingKeepsTenKindsAndUsesOriginalPrimalWeightWithoutScaling(){
        var costs=new LinkedHashMap<Aspect,Integer>();for(var aspect:Aspect.values())costs.put(aspect,100);
        costs.put(Aspect.AIR,1);costs.put(Aspect.ALCHEMY,1);
        var result=SprayerState.cull(costs);assertEquals(10,result.size());assertFalse(result.containsKey(Aspect.AIR));assertFalse(result.containsKey(Aspect.ALCHEMY));
        assertTrue(result.values().stream().allMatch(amount->amount==100));assertEquals(37,costs.size());
        var eleven=new LinkedHashMap<Aspect,Integer>();for(var aspect:Aspect.values())if(!aspect.isPrimal()&&eleven.size()<10)eleven.put(aspect,10);eleven.put(Aspect.WATER,10);
        var kept=SprayerState.cull(eleven);assertFalse(kept.containsKey(Aspect.WATER));assertEquals(10,kept.size());
    }
}
