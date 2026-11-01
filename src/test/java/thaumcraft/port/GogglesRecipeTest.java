package thaumcraft.port;

import org.junit.jupiter.api.Test;
import thaumcraft.port.research.BookRecipeCatalog;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchState;
import static org.junit.jupiter.api.Assertions.*;

class GogglesRecipeTest {
    @Test void playableRecipeMatchesTheOriginalBookContract() {
        var original = BookRecipeCatalog.recipes("thaumcraft:Goggles").getFirst();
        var playable = ResearchCatalog.read("/data/thaumcraft/recipe/goggles.json");
        for (String field : new String[]{"type", "research", "vis", "pattern"})
            assertEquals(original.get(field), playable.get(field), field);
        assertEquals(0, original.getAsJsonObject("crystals").size());
        assertEquals(0, playable.has("crystals") ? playable.getAsJsonObject("crystals").size() : 0,
                "The original recipe has no crystal costs");
        assertEquals("thaumcraft:goggles", playable.getAsJsonObject("result").get("id").getAsString());
        var key = playable.getAsJsonObject("key");
        assertEquals("thaumcraft:thaumometer", key.getAsJsonObject("T").get("item").getAsString());
        assertEquals("c:ingots/brass", key.getAsJsonObject("G").get("tag").getAsString());
        assertEquals("c:leathers", key.getAsJsonObject("L").get("tag").getAsString());
    }

    @Test void bookGogglesLinkUsesTheOriginalArtificeResearchGate() {
        var state = new ResearchState();
        var locked = BookRecipeCatalog.reference("thaumcraft:goggles;1;0", state, BookRecipeCatalog::itemIdentity);
        assertNotNull(locked);
        assertEquals("thaumcraft:Goggles", locked.recipe());
        assertTrue(locked.locked());
        state.flag("UNLOCKARTIFICE");
        var open = BookRecipeCatalog.reference("thaumcraft:goggles;1;0", state, BookRecipeCatalog::itemIdentity);
        assertNotNull(open);
        assertFalse(open.locked());
        assertEquals("thaumcraft:Goggles", open.recipe());
    }
}
