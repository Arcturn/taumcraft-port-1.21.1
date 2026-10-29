package thaumcraft.port;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;

final class EssentiaExtensionsTest {
    @Test void mixedStoreHasSharedCapacityAndNoSimulationSideEffects(){var store=new MixedEssentia(10);store.insert(Aspect.AIR,6,false);assertEquals(4,store.insert(Aspect.FIRE,100,true));assertEquals(6,store.total());assertEquals(4,store.insert(Aspect.FIRE,Integer.MAX_VALUE,false));assertEquals(0,store.insert(Aspect.WATER,1,false));assertEquals(0,store.extract(Aspect.WATER,2,false));assertEquals(4,store.extract(Aspect.FIRE,100,true));assertEquals(10,store.total());}
    @Test void corruptMixedSaveStaysBounded(){var store=new MixedEssentia(10);store.restore(Map.of(Aspect.AIR,Integer.MAX_VALUE,Aspect.FIRE,-10,Aspect.WATER,20));assertEquals(10,store.total());assertEquals(0,store.amount(Aspect.FIRE));store.restore(Map.of(Aspect.FIRE,2));assertEquals(Map.of(Aspect.FIRE,2),store.values());assertEquals(0,store.insert(null,100,false));}
    @Test void mixedTransfersConserveEachAspect(){var random=new Random(726);var stores=new MixedEssentia[20];for(int i=0;i<stores.length;i++){stores[i]=new MixedEssentia(10);stores[i].insert(Aspect.values()[i%4],10,false);}var expected=totals(stores);for(int i=0;i<10000;i++){var source=stores[random.nextInt(20)];var sink=stores[random.nextInt(20)];var a=Aspect.values()[random.nextInt(4)];int n=Math.min(source.extract(a,1,true),sink.insert(a,1,true));source.extract(a,n,false);assertEquals(n,sink.insert(a,n,false));}assertEquals(expected,totals(stores));}
    private Map<Aspect,Integer> totals(MixedEssentia[] stores){var out=new EnumMap<Aspect,Integer>(Aspect.class);for(var store:stores)store.values().forEach((a,n)->out.merge(a,n,Integer::sum));return out;}
    @Test void allBufferChokeAndBellowsCombinations(){for(int b=0;b<=6;b++){assertEquals(b==0?1:b*32,MixedEssentia.bufferSuction(b,0));assertEquals(1,MixedEssentia.bufferSuction(b,1));assertEquals(0,MixedEssentia.bufferSuction(b,2));}}
    @Test void bufferPriorityRequiresCompatibleStrongerConsumer(){assertTrue(MixedEssentia.higherDemand(Aspect.AIR,32,1,Aspect.AIR,64));assertTrue(MixedEssentia.higherDemand(Aspect.AIR,32,1,null,64));assertFalse(MixedEssentia.higherDemand(Aspect.AIR,32,1,Aspect.FIRE,128));assertFalse(MixedEssentia.higherDemand(Aspect.AIR,64,1,Aspect.AIR,64));assertFalse(MixedEssentia.higherDemand(Aspect.AIR,32,96,Aspect.AIR,64));}
    @Test void bellowsSpeedMatchesOriginalThreeSideLimit(){assertEquals(32,SmelterUpgrades.cookTime(16,0));assertEquals(28,SmelterUpgrades.cookTime(16,1));assertEquals(24,SmelterUpgrades.cookTime(16,2));assertEquals(20,SmelterUpgrades.cookTime(16,3));assertEquals(20,SmelterUpgrades.cookTime(16,6));assertEquals(1,SmelterUpgrades.cookTime(1,3));}
    @Test void ventsUseIndependentTrialsAndNeverCreateFlux(){assertEquals(100,SmelterUpgrades.pollution(100,0,()->0));assertEquals(0,SmelterUpgrades.pollution(100,1,()->.332));assertEquals(100,SmelterUpgrades.pollution(100,3,()->.333));int[] calls={0};assertEquals(0,SmelterUpgrades.pollution(10,3,()->++calls[0]%3==0?0:1));assertEquals(30,calls[0]);}
}
