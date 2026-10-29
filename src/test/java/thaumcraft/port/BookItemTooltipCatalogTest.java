package thaumcraft.port;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.BookItemPresentation;
import thaumcraft.port.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

/** Oracle: real TC6 BETA26 ItemStack.getTooltip, not modern substitute-item statistics. */
class BookItemTooltipCatalogTest {
    @Test void everyCapturedTooltipPreservesTheActualOriginalLinesInBothLanguages() {
        var catalog = ResearchCatalog.read("/thaumcraft-port/book-item-tooltips.json");
        var index = catalog.getAsJsonObject("entries");
        assertEquals(451, index.size(), "Update the actual original-client corpus when book inputs change");
        assertNotNull(catalog.getAsJsonObject("advanced_entries"), "Import the original ADVANCED snapshots as well as NORMAL");
        assertEquals(index.keySet(), catalog.getAsJsonObject("advanced_entries").keySet());
        for (String language : List.of("en_us", "ru_ru")) {
            var original = ResearchCatalog.read("/fixtures/book-item-tooltips/" + language + ".json");
            assertEquals(language, original.get("language").getAsString());
            assertEquals(index.size(), original.get("resolved").getAsInt());
            assertTrue(original.getAsJsonArray("unresolved").isEmpty());
            var translations = ResearchCatalog.read("/assets/thaumcraft/lang/" + language + ".json");
            var seen = new HashSet<String>();
            for (var value : original.getAsJsonArray("answers")) {
                var answer = value.getAsJsonObject();
                String source = answer.get("id").getAsString();
                assertTrue(seen.add(source), "Duplicate original oracle: " + source);
                assertNotEquals("minecraft:air", answer.get("resolved_item").getAsString());
                for (boolean advanced : List.of(false, true)) {
                    var actual = translated(BookItemPresentation.catalogTooltip(source, advanced), translations);
                    var expected = new ArrayList<String>();
                    answer.getAsJsonArray(advanced ? "tooltip_advanced" : "tooltip").forEach(line -> expected.add(line.getAsString()));
                    assertEquals(expected, actual, language + (advanced ? " ADVANCED: " : " NORMAL: ") + source);
                }
            }
            assertEquals(index.keySet(), seen);
        }
    }

    @Test void everyReferencedTcRecipeStackAndStageRequirementHasARealOriginalTooltip() {
        var sources = new HashSet<String>();
        collect(ResearchCatalog.read("/thaumcraft-port/book-recipes.json"), sources);
        for (var entry : ResearchCatalog.all()) {
            for (var stage : entry.stages()) {
                sources.addAll(ResearchCatalog.strings(stage, "required_item"));
                sources.addAll(ResearchCatalog.strings(stage, "required_craft"));
            }
        }
        for (String source : sources) if (source.startsWith("thaumcraft:") || source.startsWith("minecraft:thaumcraft:"))
            assertFalse(BookItemPresentation.catalogTooltip(source).isEmpty(), "Uncaptured TC tooltip: " + source);
    }

    @Test void quantityAndWildcardDamageDoNotDiscardTheRecipeSpecificModification() {
        String modified = "thaumcraft:fortress_helm;1;0;{mask:0}";
        assertEquals(modified, BookItemPresentation.canonicalSource("thaumcraft:fortress_helm;32;32767;{mask:0}"));
        var original = BookItemPresentation.catalogTooltip(modified);
        var counted = BookItemPresentation.catalogTooltip("thaumcraft:fortress_helm;32;32767;{mask:0}");
        assertEquals(original, counted);
        assertNotEquals(original, BookItemPresentation.catalogTooltip("thaumcraft:fortress_helm;1;0"));
        assertTrue(BookItemPresentation.catalogTooltip("thaumcraft:nonexistent_item").isEmpty());
    }

    @Test void everyTcOreIngredientVariantKeepsItsOriginalTooltipWhileCycling() {
        var ores = ResearchCatalog.read("/thaumcraft-port/book-ingredient-variants.json").getAsJsonObject("ingredients");
        assertEquals(46, ores.size());
        int covered = 0;
        for (var ore : ores.entrySet()) for (var value : ore.getValue().getAsJsonArray()) {
            String source = value.getAsString();
            if (!source.startsWith("thaumcraft:")) continue;
            covered++;
            for (boolean advanced : List.of(false, true)) assertFalse(BookItemPresentation.catalogTooltip(source, advanced).isEmpty(),
                    "OreIngredient " + ore.getKey() + " lost its TC tooltip for " + source);
        }
        assertTrue(covered >= 50, "Cover material metadata, six meats, and every nitor/candle color");
        assertEquals(BookItemPresentation.catalogTooltip("thaumcraft:candle_silver;1;0"),
                BookItemPresentation.catalogTooltip("thaumcraft:candle_light_gray;1;0"));
    }

    @Test void armorAttributesAndItemEffectsKeepOriginalValuesOrderAndFormatting() {
        var english = ResearchCatalog.read("/assets/thaumcraft/lang/en_us.json");
        assertEquals(List.of("Thaumium Fortress Helm§r", "§5Goggles of Revealing", "", "When on head:",
                        "§9 +3 Armor Toughness", "§9 +3 Armor"),
                translated(BookItemPresentation.catalogTooltip("thaumcraft:fortress_helm;1;32767;{goggles:1b}"), english));
        assertEquals(List.of("Verdant Heart Charm§r", "§6Lifegiver", "§eVis charge: 0", "§6Bauble (Charm)"),
                translated(BookItemPresentation.catalogTooltip("thaumcraft:verdant_charm;1;0;{type:1b}"), english));
        assertEquals(List.of("Sword of the Zephyr§r", "", "When in main hand:", " 1.6 Attack Speed", " 7 Attack Damage"),
                translated(BookItemPresentation.catalogTooltip("thaumcraft:elemental_sword;1;0"), english));
        var russian = ResearchCatalog.read("/assets/thaumcraft/lang/ru_ru.json");
        assertEquals(List.of("Кольцо жизненной силы§r", "§6Дарующий жизнь", "§eВис заряд: 0", "§6Бижутерия (Брелок)"),
                translated(BookItemPresentation.catalogTooltip("thaumcraft:verdant_charm;1;0;{type:1b}"), russian));
    }

    private static List<String> translated(List<Component> components, JsonObject translations) {
        var lines = new ArrayList<String>();
        for (var component : components) {
            assertInstanceOf(TranslatableContents.class, component.getContents(), "Resource packs must be able to override captured text");
            String key = ((TranslatableContents)component.getContents()).getKey();
            assertTrue(translations.has(key), "Missing generated original tooltip translation: " + key);
            lines.add(translations.get(key).getAsString());
        }
        return lines;
    }

    private static void collect(JsonElement value, Set<String> sources) {
        if (value.isJsonObject()) for (var member : value.getAsJsonObject().entrySet()) {
            if (Set.of("legacy", "item", "id").contains(member.getKey()) && member.getValue().isJsonPrimitive()
                    && member.getValue().getAsJsonPrimitive().isString()) sources.add(member.getValue().getAsString());
            collect(member.getValue(), sources);
        } else if (value.isJsonArray()) for (var member : value.getAsJsonArray()) collect(member, sources);
    }
}
