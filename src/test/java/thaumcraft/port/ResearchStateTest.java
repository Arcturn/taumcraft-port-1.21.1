package thaumcraft.port;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.research.ResearchState;
import static org.junit.jupiter.api.Assertions.*;

class ResearchStateTest {
    @Test void duplicateGenericScanCanRevealAnAspectButCannotRepeatItsDiscoveryReward() {
        var state = new ResearchState();
        assertTrue(state.recordScan("item:minecraft:stone", List.of(Aspect.EARTH)));
        assertFalse(state.recordScan("item:minecraft:stone", List.of(Aspect.MAGIC)));
        assertEquals(Set.of("terra", "praecantatio"), state.discovered());
        assertEquals(2, state.knowledge("OBSERVATION", "BASICS"));
        assertFalse(state.recordScan("item:minecraft:stone", List.of(Aspect.MAGIC)));
        assertEquals(2, state.knowledge("OBSERVATION", "BASICS"));
        assertEquals(1, state.scanned().size());
    }

    @Test void legacyVariantScansMigrateWithoutRegrantingKnowledge() {
        var state = new ResearchState(List.of("item:thaumcraft:crystal_essence/aer", "item:thaumcraft:crystal_essence/ignis",
                "item:thaumcraft:phial/aer", "item:thaumcraft:celestial_notes"), List.of("aer", "ignis"));
        state.flag("!aer"); state.flag("!ignis");
        state.addKnowledge("OBSERVATION", "BASICS", 27);
        assertFalse(state.recordScan("item:thaumcraft:crystal_essence", List.of(Aspect.AIR)));
        assertFalse(state.recordScan("item:thaumcraft:phial/filled", List.of(Aspect.AIR)));
        assertEquals(27, state.knowledge("OBSERVATION", "BASICS"));
        for (int kind = 0; kind <= 12; kind++) assertTrue(state.scanned().contains("item:thaumcraft:celestial_notes/" + kind));
        assertTrue(state.recordScan("item:thaumcraft:phial", List.of(Aspect.VOID)));
    }

    @Test void unknownObjectDoesNotPermanentlyConsumeItsScan() {
        var state = new ResearchState();
        assertFalse(state.recordScan("item:anothermod:ore", List.of()));
        assertTrue(state.recordScan("item:anothermod:ore", List.of(Aspect.METAL)));
    }

    @Test void loadedProgressSurvivesAndFiltersUnknownAspects() {
        var state = new ResearchState(List.of("entity:minecraft:cow"), List.of("bestia", "removed_aspect"));
        assertEquals(Set.of("bestia"), state.discovered());
        assertFalse(state.recordScan("entity:minecraft:cow", List.of(Aspect.BEAST)));
        assertTrue(state.recordScan("entity:minecraft:chicken", List.of(Aspect.FLIGHT, Aspect.AIR)));
    }

    @Test void exposedCollectionsCannotModifyAuthoritativeProgress() {
        var state = new ResearchState();
        state.recordScan("item:minecraft:stone", List.of(Aspect.EARTH));
        assertThrows(UnsupportedOperationException.class, () -> state.discovered().add("alienis"));
        assertThrows(UnsupportedOperationException.class, () -> state.scanned().clear());
    }
}
