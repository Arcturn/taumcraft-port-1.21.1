package thaumcraft.port;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import thaumcraft.port.research.BookDocument;
import thaumcraft.port.research.BookRecipeCatalog;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchState;
import thaumcraft.port.research.ResearchView;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Catalog-wide presentation regressions with isolated player states, never grant-all.
 * These fixtures supply declared prerequisites and currencies; they do not claim that
 * every item or research trigger is obtainable in survival or that GPU layout matches.
 *
 * Reference: TC6 BETA26 GuiResearchPage.addRecipesToList (770-861), parsePages
 * (1663-1692), and checkRequisites (1848-1918). Recipe gates apply independently
 * of the owning research page; addenda additionally require owner completion.
 */
class BookCatalogPresentationTest {
    private static final Set<String> ORIGINAL_STALE_IDS = Set.of(
            "thaumcraft:JarLabelEssence", "thaumcraft:arcane_brick",
            "thaumcraft:arcane_stone", "thaumcraft:nitorcolor");
    private static final Set<String> STILL_UNRESOLVED = Set.of(
            "thaumcraft:JarLabelEssence", "thaumcraft:nitorcolor");
    private static final List<String> REQUIREMENT_FIELDS = List.of(
            "required_research", "required_item", "required_craft", "required_knowledge");
    // Deliberately deterministic metrics for content integrity, not a pixel-parity oracle.
    private static final ToIntFunction<String> WIDTH = value -> {
        String visible = value.replaceAll("\u00a7.", "");
        return visible.codePointCount(0, visible.length()) * 6;
    };

    private record StageCase(ResearchCatalog.Entry entry, int number, JsonObject stage) {
        String label() { return entry.key() + "@" + number; }
    }

    private static Stream<StageCase> stages() {
        return ResearchCatalog.all().stream().flatMap(entry ->
                java.util.stream.IntStream.range(0, entry.stages().size())
                        .mapToObj(index -> new StageCase(entry, index + 1, entry.stages().get(index))));
    }

    private static Set<String> references() {
        var references = new LinkedHashSet<String>();
        for (var entry : ResearchCatalog.all()) {
            for (var stage : entry.stages()) references.addAll(ResearchCatalog.strings(stage, "recipes"));
            for (var addendum : entry.addenda()) references.addAll(ResearchCatalog.strings(addendum, "recipes"));
        }
        return references;
    }

    @Test void originalCatalogScopeAndFourStaleIdsStayExplicit() {
        assertEquals(148, ResearchCatalog.all().size());
        assertEquals(271, stages().count());
        assertEquals(16, ResearchCatalog.all().stream().mapToInt(entry -> entry.addenda().size()).sum());
        assertEquals(203, references().size());
        assertTrue(references().containsAll(ORIGINAL_STALE_IDS));
        var unresolved = new LinkedHashSet<String>();
        for (String key : references()) if (BookRecipeCatalog.recipes(key).isEmpty()) unresolved.add(key);
        assertEquals(STILL_UNRESOLVED, unresolved,
                "No new missing tab may be hidden inside the original four-ID exception");
        // These two stale TC6 IDs intentionally resolve to the port's modern recipes.
        assertFalse(BookRecipeCatalog.recipes("thaumcraft:arcane_stone").isEmpty());
        assertFalse(BookRecipeCatalog.recipes("thaumcraft:arcane_brick").isEmpty());
    }

    @TestFactory Stream<DynamicTest> everyStageHasAnIsolatedInProgressPresentation() {
        return stages().map(test -> DynamicTest.dynamicTest(test.label(), () -> {
            var state = stageFixture(test);
            assertEquals(test.number(), state.stage(test.entry().key()));
            assertFalse(state.knows(test.entry().key()), "Opening or supplying requirements is not completion");
            assertTrue(state.available(test.entry()));
            assertTrue(ResearchView.visible(state, test.entry()));
            assertEquals(0, ResearchView.addenda(state, test.entry()), "Incomplete owners cannot expose addenda");
            for (String prerequisite : ResearchCatalog.strings(test.stage(), "required_research"))
                assertTrue(state.knows(prerequisite), prerequisite);
            // A presentation fixture must not manufacture craft accomplishments or
            // turn an unsupported requirement into a successful inventory check.
            assertTrue(state.crafted().isEmpty());
            assertTrue(state.scanned().isEmpty());
            for (var cost : knowledgeCosts(test.stage()).entrySet()) {
                String[] key = cost.getKey().split(";", 2);
                assertEquals(cost.getValue().intValue(), state.knowledge(key[0], key[1]));
            }
            for (String key : ResearchCatalog.strings(test.stage(), "recipes")) {
                var catalog = BookRecipeCatalog.recipes(key);
                if (catalog.isEmpty()) {
                    assertTrue(STILL_UNRESOLVED.contains(key), "Unregistered recipe tab: " + key);
                    continue;
                }
                // A stage may name a future gated tab. Empty here is legal; a tab
                // with a gate the isolated player lacks must never become visible.
                for (var recipe : BookRecipeCatalog.recipes(key, state)) {
                    assertTrue(catalog.contains(recipe), "A tab resolved outside its declared recipe group");
                    assertTrue(recipe.has("result"), key);
                    if (recipe.has("research")) assertTrue(state.knows(recipe.get("research").getAsString()), key);
                }
            }
            var copy = thaumcraft.port.research.ResearchCodec.read(thaumcraft.port.research.ResearchCodec.write(state));
            for (String key : ResearchCatalog.strings(test.stage(), "recipes"))
                assertEquals(BookRecipeCatalog.recipes(key, state), BookRecipeCatalog.recipes(key, copy),
                        "Reload changed the tabs at " + test.label());
        }));
    }

    @TestFactory Stream<DynamicTest> everyReferencedRecipeHonorsEachGateBoundary() {
        var tests = new ArrayList<DynamicTest>();
        for (String key : references()) {
            var recipes = BookRecipeCatalog.recipes(key);
            for (int index = 0; index < recipes.size(); index++) {
                var recipe = recipes.get(index);
                String label = key + "[" + index + "]";
                tests.add(DynamicTest.dynamicTest(label, () -> {
                    String gate = recipe.has("research") ? recipe.get("research").getAsString() : "";
                    var unlocked = new ResearchState();
                    satisfy(unlocked, gate);
                    assertTrue(BookRecipeCatalog.recipes(key, unlocked).contains(recipe),
                            "The recipe's own exact gate must suffice; unrelated research is not required");
                    if (gate.isBlank()) {
                        assertTrue(BookRecipeCatalog.recipes(key, new ResearchState()).contains(recipe));
                        return;
                    }
                    // Current BETA26 recipe gates are atoms or conjunctions. Reject an
                    // unexpected expression instead of quietly omitting its negative case.
                    assertFalse(gate.contains("||"), "Add an explicit OR-gate fixture for " + label);
                    var clauses = gate.split("&&");
                    for (int missing = 0; missing < clauses.length; missing++) {
                        var below = new ResearchState();
                        for (int other = 0; other < clauses.length; other++)
                            if (other != missing) satisfy(below, clauses[other]);
                        String clause = clauses[missing];
                        int at = clause.indexOf('@');
                        if (at >= 0) {
                            int threshold = Integer.parseInt(clause.substring(at + 1));
                            if (threshold > 1) below.stage(clause.substring(0, at), threshold - 1);
                        } else {
                            var required = ResearchCatalog.get(clause);
                            if (required != null) below.stage(clause, required.stages().size());
                        }
                        assertFalse(BookRecipeCatalog.recipes(key, below).contains(recipe),
                                "Premature tab at the boundary of " + clause);
                    }
                }));
            }
        }
        return tests.stream();
    }

    @Test void allSixteenAddendaRequireCompletionAndTheirOwnDiscoveries() {
        // Independent expected groups from original research JSON. PLANTWOOD opens
        // two different addenda, so counting flags instead of pages would be wrong.
        Map<String, Map<String, Integer>> expected = new LinkedHashMap<>();
        expected.put("FLUX", Map.of("f_toomuchflux", 1));
        expected.put("ORE", Map.of("!OREAMBER", 1, "!ORECINNABAR", 1, "!ORECRYSTAL", 1));
        expected.put("PLANTS", Map.of("!PLANTWOOD", 2, "!PLANTCINDERPEARL", 1,
                "!PLANTSHIMMERLEAF", 1, "!PLANTVISHROOM", 1));
        expected.put("INFERNALFURNACE", Map.of("BELLOWS", 1));
        expected.put("ESSENTIASMELTER", Map.of("BELLOWS", 1));
        for (String seal : List.of("SEALCOLLECT", "SEALSTORE", "SEALEMPTY", "SEALGUARD", "SEALBREAK"))
            expected.put(seal, Map.of("MINDBIOTHAUMIC", 1));
        assertEquals(16, expected.values().stream().flatMap(map -> map.values().stream()).mapToInt(Integer::intValue).sum());
        for (var owner : expected.entrySet()) {
            var entry = ResearchCatalog.get(owner.getKey());
            var partial = new ResearchState();
            partial.stage(entry.key(), entry.stages().size());
            owner.getValue().keySet().forEach(gate -> satisfy(partial, gate));
            assertEquals(0, ResearchView.addenda(partial, entry), entry.key());
            var completed = new ResearchState();
            completed.stage(entry.key(), entry.stages().size() + 1);
            assertEquals(0, ResearchView.addenda(completed, entry), entry.key());
            int visible = 0;
            for (var trigger : owner.getValue().entrySet()) {
                satisfy(completed, trigger.getKey());
                visible += trigger.getValue();
                assertEquals(visible, ResearchView.addenda(completed, entry), entry.key() + ": " + trigger.getKey());
            }
            for (var addendum : entry.addenda()) for (String key : ResearchCatalog.strings(addendum, "recipes")) {
                var variants = BookRecipeCatalog.recipes(key);
                assertFalse(variants.isEmpty(), key);
                assertEquals(variants, BookRecipeCatalog.recipes(key, completed), key);
                // Knowing the helper alone must not expose an advanced seal recipe.
                var helperOnly = new ResearchState();
                ResearchCatalog.strings(addendum, "required_research").forEach(gate -> satisfy(helperOnly, gate));
                assertTrue(BookRecipeCatalog.recipes(key, helperOnly).isEmpty(), key);
            }
        }
    }

    @TestFactory Stream<DynamicTest> allStageAndAddendumTranslationsPreserveTheirContents() {
        var english = ResearchCatalog.read("/assets/thaumcraft/lang/en_us.json");
        var tests = new ArrayList<DynamicTest>();
        for (String language : List.of("en_us", "ru_ru")) {
            var localized = ResearchCatalog.read("/assets/thaumcraft/lang/" + language + ".json");
            for (var entry : ResearchCatalog.all()) {
                var sections = new ArrayList<JsonObject>(entry.stages());
                sections.addAll(entry.addenda());
                for (var section : sections) {
                    String textKey = section.get("text").getAsString();
                    tests.add(DynamicTest.dynamicTest(language + ":" + entry.key() + ":" + textKey, () -> {
                        assertTrue(english.has(textKey), "Missing original English page " + textKey);
                        if (!localized.has(textKey)) {
                            assertEquals("ru_ru", language);
                            assertEquals("research.SEALBREAK.stage.ADV", textKey,
                                    "Only this documented BETA26 RU omission may use English fallback");
                        }
                        String raw = (localized.has(textKey) ? localized : english).get(textKey).getAsString();
                        assertFalse(raw.isBlank());
                        var elements = BookDocument.parse(raw, WIDTH, 9);
                        int requirementRows = (int) REQUIREMENT_FIELDS.stream()
                                .filter(field -> !ResearchCatalog.strings(section, field).isEmpty()).count();
                        int firstHeight = BookDocument.firstPageHeight(entry.key().equals("KNOWLEDGETYPES"), 2, requirementRows);
                        var pages = BookDocument.paginate(elements, firstHeight, 9);
                        assertFalse(pages.isEmpty());
                        assertEquals(contentCounts(elements), contentCounts(pages.stream().flatMap(List::stream)
                                .map(BookDocument.Placed::element).toList()), "Pagination dropped or duplicated content");
                        for (var page : pages) {
                            int bottom = 0;
                            for (var placed : page) {
                                assertTrue(placed.y() >= bottom, "Overlapping elements in " + textKey);
                                bottom = placed.y() + placed.element().height();
                                var image = placed.element().image();
                                if (image != null) {
                                    assertTrue(image.width() > 0 && image.height() > 0 && image.scale() > 0);
                                    String asset = "/assets/" + image.texture().replace(':', '/');
                                    assertNotNull(ResearchCatalog.class.getResource(asset), "Missing inline image " + asset);
                                }
                            }
                        }
                    }));
                }
            }
        }
        assertEquals(574, tests.size());
        return tests.stream();
    }

    private static Map<BookDocument.Element, Integer> contentCounts(List<BookDocument.Element> elements) {
        var counts = new HashMap<BookDocument.Element, Integer>();
        for (var element : elements) if (!element.pageBreak() && (element.text()!=null || element.image()!=null || element.height()!=0)) counts.merge(element, 1, Integer::sum);
        return counts;
    }

    private static ResearchState stageFixture(StageCase test) {
        var state = new ResearchState();
        test.entry().parents().forEach(parent -> satisfy(state, parent));
        ResearchCatalog.strings(test.stage(), "required_research").forEach(required -> satisfy(state, required));
        state.stage(test.entry().key(), test.number());
        for (var cost : knowledgeCosts(test.stage()).entrySet()) {
            String[] key = cost.getKey().split(";", 2);
            assertTrue(state.addKnowledge(key[0], key[1], cost.getValue()));
        }
        return state;
    }

    private static Map<String, Integer> knowledgeCosts(JsonObject stage) {
        var costs = new LinkedHashMap<String, Integer>();
        for (String encoded : ResearchCatalog.strings(stage, "required_knowledge")) {
            String[] parts = encoded.split(";", -1);
            assertEquals(3, parts.length, encoded);
            int progression = ResearchState.progression(parts[0]);
            assertTrue(progression > 0, encoded);
            assertTrue(ResearchCatalog.CATEGORIES.contains(parts[1]), encoded);
            int amount = Integer.parseInt(parts[2]);
            assertTrue(amount > 0, encoded);
            costs.merge(parts[0] + ";" + parts[1], Math.multiplyExact(amount, progression), Integer::sum);
        }
        return costs;
    }

    /** Supply only the named gate, without populating unrelated research/scans. */
    private static void satisfy(ResearchState state, String expression) {
        if (expression.isBlank()) return;
        if (expression.contains("||")) {
            satisfy(state, expression.split("\\|\\|", 2)[0]);
            return;
        }
        if (expression.contains("&&")) {
            for (String part : expression.split("&&")) satisfy(state, part);
            return;
        }
        String key = expression.startsWith("~") ? expression.substring(1) : expression;
        int at = key.indexOf('@');
        if (at >= 0) state.stage(key.substring(0, at), Integer.parseInt(key.substring(at + 1)));
        else {
            var entry = ResearchCatalog.get(key);
            if (entry == null) state.flag(key);
            else state.stage(key, entry.stages().size() + 1);
        }
    }
}
