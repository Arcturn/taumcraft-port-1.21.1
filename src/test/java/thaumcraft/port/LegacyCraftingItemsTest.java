package thaumcraft.port;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.LegacyCraftingItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyCraftingItemsTest {
    @Test void migratedMetadataAndCapturedSealTokensHaveTheSameIdentityAsLiveResults() {
        var examples = Map.ofEntries(
                Map.entry("thaumcraft:mind;1;0", "thaumcraft:mind_clockwork;1;0"),
                Map.entry("thaumcraft:mind;1;1", "thaumcraft:mind_biothaumic;1;0"),
                Map.entry("thaumcraft:module;1;0", "thaumcraft:module_vision;1;0"),
                Map.entry("thaumcraft:module;1;1", "thaumcraft:module_aggression;1;0"),
                Map.entry("thaumcraft:turret;1;0", "thaumcraft:turret_placer_basic;1;0"),
                Map.entry("thaumcraft:turret;1;1", "thaumcraft:turret_placer_advanced;1;0"),
                Map.entry("thaumcraft:turret;1;2", "thaumcraft:turret_placer_bore;1;0"),
                Map.entry("thaumcraft:seal;3;0", "thaumcraft:seal_blank;3;0"),
                Map.entry("thaumcraft:seal;1;7", "thaumcraft:seal_harvest;1;0"),
                Map.entry("thaumcraft:seal;1;8", "thaumcraft:seal_butcher;1;0"),
                Map.entry("thaumcraft:seal;1;12", "thaumcraft:seal_breaker;1;0"),
                Map.entry("thaumcraft:seals;1;0;thaumcraft:harvest", "thaumcraft:seal_harvest;1;0"),
                Map.entry("thaumcraft:seals;1;0;thaumcraft:butcher", "thaumcraft:seal_butcher;1;0"),
                Map.entry("thaumcraft:seals;1;0;thaumcraft:pickup", "thaumcraft:seal_pickup;1;0"),
                Map.entry("thaumcraft:seals;2;0;thaumcraft:breaker_advanced", "thaumcraft:seal_breaker_advanced;2;0"));
        examples.forEach((source, expected) -> {
            assertEquals(expected, LegacyCraftingItems.normalize(source), source);
            assertEquals(expected, LegacyCraftingItems.normalize(expected), "Live IDs must stay stable");
        });
    }

    @Test void conversionRetainsCountsAndOpaqueNbtForConservativeRequirementMatching() {
        String data = "{display:{Name:\"mind;variant\"},unknown:3b}";
        assertEquals("thaumcraft:mind_biothaumic;4;0;" + data,
                LegacyCraftingItems.normalize("thaumcraft:mind;4;1;" + data));
        assertEquals("thaumcraft:seal_blank;1;0", LegacyCraftingItems.normalize("thaumcraft:seal"));
        assertEquals("thaumcraft:seal_pickup;2;0;" + data,
                LegacyCraftingItems.normalize("thaumcraft:seal;2;1;" + data));
        // A crafted amulet must not become the original book's found-amulet subtype.
        assertEquals("thaumcraft:amulet_vis;1;1",LegacyCraftingItems.normalize("thaumcraft:amulet_vis;1;1"));
    }

    @Test void unsupportedVariantsAndMalformedMetadataNeverBecomeSupportedItems() {
        for (String source : List.of(
                "thaumcraft:mind;1;2", "thaumcraft:module;1;-1", "thaumcraft:turret;1;3",
                "thaumcraft:seal;1;-1", "thaumcraft:seal;1;99", "thaumcraft:amulet_vis;1;2",
                "thaumcraft:seals;1;0;thaumcraft:unknown", "thaumcraft:seals;1;0;other:pickup",
                "thaumcraft:seals;1;0;{type:\"thaumcraft:pickup\"}",
                "thaumcraft:mind;1;invalid", "minecraft:stone;4;0;{unknown:1b}"))
            assertEquals(source, LegacyCraftingItems.normalize(source), source);
    }
}
