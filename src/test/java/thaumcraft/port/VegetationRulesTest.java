package thaumcraft.port;

import java.lang.reflect.Proxy;
import java.util.Random;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.port.world.VegetationRules;
import thaumcraft.port.world.VegetationRules.ForestTree;

import static org.junit.jupiter.api.Assertions.*;

class VegetationRulesTest {
    @Test void extraTreeUsesOriginalStrictFloatThreshold() {
        assertEquals(3, attemptsAt(0f));
        assertEquals(3, attemptsAt(Math.nextDown(.1f)));
        assertEquals(2, attemptsAt(.1f));
        assertEquals(2, attemptsAt(Math.nextUp(.1f)));
        assertEquals(2, attemptsAt(Math.nextDown(1f)));
    }

    @Test void forestBudgetHasTwoTreesAndTenPercentThirdTree() {
        var random=RandomSource.create(0x7CA6L);
        int samples=100_000, extra=0;
        for(int i=0;i<samples;i++){
            int count=VegetationRules.magicalForestTreeAttempts(random);
            assertTrue(count==2||count==3,"TC6's treesPerChunk is 2, not the 6 lily pads");
            extra+=count-2;
        }
        assertBinomial(extra,samples,.1);
    }

    @Test void forestSelectionMatchesOriginalJavaRandomIncludingConsumedDraws() {
        // Independent legacy expressions recovered from BiomeGenMagicalForest and
        // vanilla BiomeDecorator. Compare RNG state too: the second species roll is lazy.
        for(long seed=0;seed<64;seed++){
            var actual=RandomSource.create(seed);
            var legacy=new Random(seed);
            for(int chunk=0;chunk<1_000;chunk++){
                int expectedTrees=2+(legacy.nextFloat()<.1f?1:0);
                assertEquals(expectedTrees,VegetationRules.magicalForestTreeAttempts(actual));
                for(int tree=0;tree<expectedTrees;tree++){
                    ForestTree expected=legacy.nextInt(18)==0?ForestTree.SILVERWOOD:
                            legacy.nextInt(12)==0?ForestTree.GREATWOOD:ForestTree.BIG_MAGIC;
                    assertEquals(expected,VegetationRules.magicalForestTree(actual));
                }
            }
            assertEquals(legacy.nextLong(),actual.nextLong(),"Selection changed random draw order");
        }
    }

    @Test void speciesMixPreservesConditionalGreatwoodProbability() {
        var random=RandomSource.create(0xB10AEL);
        int samples=1_000_000;
        int[] counts=new int[ForestTree.values().length];
        for(int i=0;i<samples;i++)counts[VegetationRules.magicalForestTree(random).ordinal()]++;
        assertBinomial(counts[ForestTree.SILVERWOOD.ordinal()],samples,1.0/18);
        assertBinomial(counts[ForestTree.GREATWOOD.ordinal()],samples,17.0/216);
        assertBinomial(counts[ForestTree.BIG_MAGIC.ordinal()],samples,187.0/216);
    }

    @Test void ordinaryForestGlobalRollsKeepOriginalRarity() {
        var random=RandomSource.create(0xC0FFEE);
        int samples=1_000_000, silverwood=0, greatwood=0;
        for(int i=0;i<samples;i++){
            if(VegetationRules.rollSilverwood(random)){
                VegetationRules.rareTreeCoordinate(0,random);
                VegetationRules.rareTreeCoordinate(0,random);
                if(random.nextFloat()<.5f)silverwood++;
            }
            if(VegetationRules.rollGreatwood(random)){
                VegetationRules.rareTreeCoordinate(0,random);
                VegetationRules.rareTreeCoordinate(0,random);
                if(random.nextFloat()<1f)greatwood++;
            }
        }
        assertBinomial(silverwood,samples,1.0/160);
        assertBinomial(greatwood,samples,1.0/25);
    }

    @Test void rareTreesUseOriginalCenteredNineByNineAreaIncludingNegativeChunks() {
        var random=RandomSource.create(0xCE47E2);
        int[] cells=new int[81];
        int samples=81_000, minX=-48, minZ=32;
        for(int i=0;i<samples;i++){
            int x=VegetationRules.rareTreeCoordinate(minX,random)-minX;
            int z=VegetationRules.rareTreeCoordinate(minZ,random)-minZ;
            assertTrue(x>=4&&x<=12&&z>=4&&z<=12);
            cells[(x-4)*9+z-4]++;
        }
        for(int count:cells)assertBinomial(count,samples,1.0/81);
    }

    @Test void globalGateAndOffsetSequenceMatchesLegacyExpressions() {
        var actual=RandomSource.create(0xCA6L);
        var legacy=new Random(0xCA6L);
        for(int chunk=0;chunk<100_000;chunk++){
            assertEquals(legacy.nextInt(80)==3,VegetationRules.rollSilverwood(actual));
            assertEquals(8+legacy.nextInt(9)-4,VegetationRules.rareTreeCoordinate(0,actual));
            assertEquals(legacy.nextInt(25)==7,VegetationRules.rollGreatwood(actual));
            assertEquals(-32+8+legacy.nextInt(9)-4,VegetationRules.rareTreeCoordinate(-32,actual));
        }
        assertEquals(legacy.nextLong(),actual.nextLong());
    }

    private static int attemptsAt(float roll) {
        var random=(RandomSource)Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                    if(method.getName().equals("nextFloat"))return roll;
                    throw new AssertionError("Unexpected random call: "+method.getName());
                });
        return VegetationRules.magicalForestTreeAttempts(random);
    }

    private static void assertBinomial(int observed,int samples,double probability) {
        double expected=samples*probability;
        double tolerance=6*Math.sqrt(samples*probability*(1-probability))+1;
        assertEquals(expected,observed,tolerance,"Unexpected tree selection frequency");
    }
}
