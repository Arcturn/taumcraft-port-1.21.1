package thaumcraft.port;

import java.util.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.research.*;
import static org.junit.jupiter.api.Assertions.*;

class ScanObservationTest {
    @Test void newPotionEffectOnKnownBottleIsStillASuccessfulScan() {
        var state = new ResearchState();
        var speed = new ScanObservation("item:minecraft:potion", Map.of(Aspect.WATER, 5), Set.of("!effect.moveSpeed"));
        assertTrue(speed.canLearn(state)); assertTrue(speed.learn(state));
        var knowledge = Map.copyOf(state.knowledge());
        var fire = new ScanObservation("item:minecraft:potion", Map.of(Aspect.WATER, 5), Set.of("!effect.fireResistance"));
        assertTrue(fire.canLearn(state)); assertTrue(fire.learn(state));
        assertEquals(knowledge, state.knowledge());
        assertEquals(1, state.scanned().size());
        assertFalse(fire.canLearn(state)); assertFalse(fire.learn(state));
    }

    @Test void specialDiscoveriesWorkWhenNoAspectsAreAssigned() {
        var state = new ResearchState();
        var shot = new ScanObservation("entity:minecraft:spectral_arrow", Map.of(), Set.of("f_arrow"));
        assertTrue(shot.canLearn(state)); assertTrue(shot.learn(state));
        assertTrue(state.knows("f_arrow")); assertTrue(state.scanned().isEmpty()); assertTrue(state.knowledge().isEmpty());
        assertFalse(shot.canLearn(state)); assertFalse(shot.learn(state));
    }

    @Test void scanningCannotRepeatTheGenericOrAspectCurrencyRewards() {
        var state = new ResearchState();
        var first = new ScanObservation("item:test:object", Map.of(Aspect.AIR, 10), Set.of());
        first.learn(state);
        assertEquals(4, state.knowledge("OBSERVATION", "BASICS")); // ceil(sqrt(10 * .5)) + first Aer.
        assertEquals(4, state.knowledge("OBSERVATION", "AUROMANCY"));
        assertEquals(1, state.knowledge("OBSERVATION", "ALCHEMY"));
        var copy = ResearchCodec.read(ResearchCodec.write(state));
        assertFalse(first.learn(copy)); assertEquals(state.knowledge(), copy.knowledge());
        var second = new ScanObservation("item:test:object", Map.of(Aspect.AIR, 100, Aspect.FIRE, 10), Set.of());
        assertTrue(second.learn(copy));
        assertEquals(5, copy.knowledge("OBSERVATION", "BASICS"));
        assertEquals(1, copy.scanned().size());
    }

    @Test void oldIncorrectFlagsMigrateWithoutRemovingKnowledgeOrOldFlags() {
        var state = new ResearchState();
        state.flag("!enchantment.efficiency"); state.flag("!potion.nausea"); state.flag("!potion.fireResistance");
        state.addKnowledge("THEORY", "BASICS", 17); state.stage("FIRSTSTEPS", 2); state.warp(1, 2, 3);
        var loaded = ResearchCodec.read(ResearchCodec.write(state));
        assertTrue(loaded.flags().containsAll(state.flags()));
        assertTrue(loaded.knows("!enchantment.digging"));
        assertTrue(loaded.knows("!effect.confusion")); assertTrue(loaded.knows("!effect.fireResistance"));
        assertEquals(state.knowledge(), loaded.knowledge()); assertEquals(2, loaded.stage("FIRSTSTEPS")); assertEquals(3, loaded.temporaryWarp());
        assertFalse(new ScanObservation("", Map.of(), Set.of("!enchantment.digging", "!effect.confusion")).canLearn(loaded));
    }

    @Test void addonNamespacesDoNotCollideWithVanillaNames() {
        assertEquals("!effect.example.speed", LegacyScanKeys.effect("example:speed", "effect.example.speed"));
        assertEquals("!effect.moveSpeed", LegacyScanKeys.effect("minecraft:speed", "effect.minecraft.speed"));
        assertEquals("!enchantment.example.power", LegacyScanKeys.enchantment("example:power", "enchantment.example.power"));
        assertEquals("!enchantment.arrowDamage", LegacyScanKeys.enchantment("minecraft:power", "enchantment.minecraft.power"));
    }

    @Test void completedDiscoveryPagesAreNotReportedAsNew() {
        var state = new ResearchState(); var ore = ResearchCatalog.get("ORE");
        state.stage("ORE", ore.stages().size() + 1);
        var observation = new ScanObservation("", Map.of(), Set.of("ORE"));
        assertFalse(observation.canLearn(state)); assertFalse(observation.learn(state));
    }

    @Test void observationsDoNotRetainMutableInputCollections() {
        var aspects = new LinkedHashMap<>(Map.of(Aspect.AIR, 1)); var flags = new HashSet<>(Set.of("f_FLY"));
        var observation = new ScanObservation("entity:test:bird", aspects, flags);
        aspects.clear(); flags.clear();
        assertEquals(Map.of(Aspect.AIR, 1), observation.aspects()); assertEquals(Set.of("f_FLY"), observation.discoveries());
        assertThrows(UnsupportedOperationException.class, () -> observation.discoveries().clear());
    }
}
