package thaumcraft.port.world;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;

class CrystalBiomeAffinityTest {
    private static Predicate<TagKey<Biome>> tags(String... names) {
        var wanted=Set.of(names);
        return key->wanted.contains(key.location().toString());
    }

    @Test void coldForestKeepsBothOriginalTypeAspectsInsteadOfFirstTagWinning() {
        var counts=new EnumMap<Aspect,Integer>(Aspect.class);
        var random=RandomSource.create(0xC01DF0L);
        for(int i=0;i<60_000;i++) counts.merge(CrystalBiomeAffinity.select(
                tags("c:is_cold","c:is_snowy","c:is_forest"),random),1,Integer::sum);
        assertEquals(Set.of(Aspect.ORDER,Aspect.EARTH),counts.keySet());
        assertTrue(Math.abs(counts.get(Aspect.ORDER)-40_000)<600,
                "Cold and snowy are two original biome types, so ORDER should retain double weight");
        assertTrue(Math.abs(counts.get(Aspect.EARTH)-20_000)<600,
                "The forest aspect must remain selectable despite cold/snowy tags");
    }

    @Test void aliasTagsDoNotMultiplyOneLegacyType() {
        var minimal=tags("c:is_cold","c:is_forest");
        var aliases=tags("c:is_cold","c:is_cold/overworld","c:is_forest","minecraft:is_forest");
        var first=RandomSource.create(17);var second=RandomSource.create(17);
        for(int i=0;i<10_000;i++) assertEquals(CrystalBiomeAffinity.select(minimal,first),
                CrystalBiomeAffinity.select(aliases,second));
        assertEquals(first.nextLong(),second.nextLong(),"Aliases changed the number of random choices");
    }

    @Test void dryUsesTheFinalOriginalEntropyEntryAndHotIsStillSelectable() {
        var random=RandomSource.create(42);
        var observed=java.util.EnumSet.noneOf(Aspect.class);
        for(int i=0;i<1000;i++) observed.add(CrystalBiomeAffinity.select(tags("c:is_hot","c:is_dry"),random));
        assertEquals(Set.of(Aspect.FIRE,Aspect.ENTROPY),observed);
        assertEquals(Aspect.ENTROPY,CrystalBiomeAffinity.select(tags("c:is_dry"),random));
    }

    @Test void unmappedBiomeReturnsNullWithoutInventingAnAirBiasOrConsumingRandomness() {
        var random=(RandomSource)Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                    throw new AssertionError("Unmapped biome consumed a random choice: "+method.getName());
                });
        assertNull(CrystalBiomeAffinity.select(tags("example:unmapped"),random));
    }

    @Test void aquaticAndOceanTypeWeightsAreNotCollapsedToOneAspectEntry() {
        // WATER and OCEAN both existed in the original type set; HOT adds a third type.
        var counts=new EnumMap<Aspect,Integer>(Aspect.class);
        var random=RandomSource.create(0x0CE4L);
        for(int i=0;i<60_000;i++) counts.merge(CrystalBiomeAffinity.select(
                tags("c:is_aquatic","c:is_ocean","c:is_hot"),random),1,Integer::sum);
        assertTrue(Math.abs(counts.get(Aspect.WATER)-40_000)<600);
        assertTrue(Math.abs(counts.get(Aspect.FIRE)-20_000)<600);
    }

    @Test void rareTypeRetainsItsOriginalRandomPrimalFallbackChance() {
        var random=RandomSource.create(0xA11CE);
        int fallback=0,order=0;
        for(int i=0;i<60_000;i++) {
            var aspect=CrystalBiomeAffinity.select(tags("c:is_mushroom","c:is_rare"),random);
            if(aspect==null)fallback++;else {assertEquals(Aspect.ORDER,aspect);order++;}
        }
        assertTrue(Math.abs(fallback-30_000)<600);
        assertTrue(Math.abs(order-30_000)<600);
    }

    @Test void taigaRetainsBothForestAndConiferousEarthTypeWeights() {
        var random=RandomSource.create(0x7A16A);
        int earth=0,order=0;
        for(int i=0;i<60_000;i++) {
            var aspect=CrystalBiomeAffinity.select(tags("c:is_taiga","c:is_tree/coniferous","c:is_cold"),random);
            if(aspect==Aspect.EARTH)earth++;else {assertEquals(Aspect.ORDER,aspect);order++;}
        }
        assertTrue(Math.abs(earth-40_000)<600);
        assertTrue(Math.abs(order-20_000)<600);
    }

}
