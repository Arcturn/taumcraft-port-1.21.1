package thaumcraft.port;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;

class AspectTest {
    @Test void originalBaseAspectsArePreserved() {
        assertEquals(Set.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"),
                Arrays.stream(Aspect.values()).filter(Aspect::isPrimal).map(Aspect::id)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(37, Aspect.values().length);
    }

    @Test void keyLegacyCombinationsArePreserved() {
        assertEquals(Aspect.LIFE, Aspect.combine(Aspect.EARTH, Aspect.WATER).orElseThrow());
        assertEquals(Aspect.SOUL, Aspect.combine(Aspect.LIFE, Aspect.DEATH).orElseThrow());
        assertEquals(Aspect.MAGIC, Aspect.combine(Aspect.ENERGY, Aspect.AIR).orElseThrow());
        assertEquals(Aspect.FLUX, Aspect.combine(Aspect.ENTROPY, Aspect.MAGIC).orElseThrow());
        assertTrue(Aspect.combine(Aspect.AIR, Aspect.AIR).isEmpty());
    }

    @Test void combinationsAreCommutativeAndUnique() {
        for (Aspect aspect : Aspect.values()) {
            if (!aspect.isPrimal()) {
                var a = aspect.components().get(0);
                var b = aspect.components().get(1);
                assertEquals(aspect, Aspect.combine(a, b).orElseThrow());
                assertEquals(aspect, Aspect.combine(b, a).orElseThrow());
            }
        }
    }

    @Test void decompositionAlwaysTerminatesAtPrimalAspects() {
        for (Aspect aspect : Aspect.values()) visit(aspect, new HashSet<>());
    }

    private static void visit(Aspect aspect, Set<Aspect> path) {
        assertTrue(path.add(aspect), "Cyclic aspect: " + aspect);
        for (Aspect child : aspect.components()) visit(child, path);
        path.remove(aspect);
    }
}
