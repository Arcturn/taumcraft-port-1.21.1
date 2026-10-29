package thaumcraft.port;

import java.util.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class AspectMathTest {
    @Test void cullingPrefersCompoundAspectsAndPreservesFirstInsertionTieRule() {
        var values = new LinkedHashMap<Aspect, Integer>();
        for (var aspect : List.of(Aspect.FIRE, Aspect.AIR, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY, Aspect.MAGIC, Aspect.MAN))
            values.put(aspect, 10);
        var result = AspectMath.finish(values);
        assertEquals(7, result.size());
        assertFalse(result.containsKey(Aspect.FIRE));
        assertTrue(result.containsKey(Aspect.AIR));
        assertTrue(result.containsKey(Aspect.MAN));
        assertEquals(8, values.size());
    }

    @Test void valuesAreCulledBeforeTheFiveHundredCap() {
        var values = new LinkedHashMap<Aspect, Integer>();
        values.put(Aspect.AIR, 1000);
        for (var aspect : List.of(Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY, Aspect.MAGIC, Aspect.MAN)) values.put(aspect, 600);
        var result = AspectMath.finish(values);
        assertEquals(500, result.get(Aspect.AIR));
        assertFalse(result.containsKey(Aspect.EARTH));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test void reusableContainersCannotSubtractMoreThanAnIngredientSupplied() {
        var values = new LinkedHashMap<>(Map.of(Aspect.WATER, 5));
        AspectMath.reduce(values, Aspect.WATER, 10);
        assertEquals(5, values.get(Aspect.WATER));
        AspectMath.reduce(values, Aspect.WATER, 5);
        assertTrue(AspectMath.finish(values).isEmpty());
        AspectMath.merge(values, Aspect.TOOL, 12);
        AspectMath.merge(values, Aspect.TOOL, 9);
        assertEquals(12, values.get(Aspect.TOOL));
    }

    @Test void legacyFractionalRoundingIsRetained() {
        assertEquals(0, AspectMath.scaled(1, 1));
        assertEquals(1, AspectMath.scaled(5, 4));
        assertEquals(3, AspectMath.scaled(5, 1));
        assertEquals(0, AspectMath.scaled(5, 0));
    }

    @Test void playerAspectsUseStableNameSeedAndCanAccumulateRepeatedPicks() {
        assertEquals(AspectMath.player("Azanor"), AspectMath.player("Azanor"));
        assertEquals(49, AspectMath.player("Azanor").values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(AspectMath.player("Azanor").containsKey(Aspect.MAN));
        assertNotEquals(AspectMath.player("Azanor"), AspectMath.player("Steve"));
        boolean repeated = false;
        for (int i = 0; i < 1000; i++) if (AspectMath.player("player" + i).values().stream().anyMatch(n -> n >= 30)) repeated = true;
        assertTrue(repeated);
    }
}
