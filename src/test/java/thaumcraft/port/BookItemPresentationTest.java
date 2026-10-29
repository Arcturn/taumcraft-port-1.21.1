package thaumcraft.port;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.BookItemPresentation;
import thaumcraft.port.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

class BookItemPresentationTest {
    @Test void originalMetadataNamesDoNotCollapseIntoRegistryIds() {
        // Original ItemBaubles constructor order; ItemTCBase appends the variant.
        String[] baubles = {"amulet_mundane", "ring_mundane", "girdle_mundane", "ring_apprentice",
                "amulet_fancy", "ring_fancy", "girdle_fancy"};
        for (int metadata = 0; metadata < baubles.length; metadata++)
            assertEquals("item.baubles." + baubles[metadata] + ".name",
                    BookItemPresentation.nameKey("thaumcraft:baubles;1;" + metadata));
        Map<String, String> examples = Map.ofEntries(
                Map.entry("thaumcraft:mind;1;1", "item.mind.biothaumic.name"),
                Map.entry("thaumcraft:module;1;1", "item.module.aggression.name"),
                Map.entry("thaumcraft:turret;1;2", "item.turret.bore.name"),
                Map.entry("thaumcraft:cluster;1;6", "item.cluster.cinnabar.name"),
                Map.entry("thaumcraft:nugget;9;5", "item.nugget.quicksilver.name"),
                Map.entry("thaumcraft:nugget;1;10", "item.nugget.rareearth.name"),
                Map.entry("thaumcraft:chunk;1;5", "item.chunk.mutton.name"),
                Map.entry("thaumcraft:amulet_vis;1;0", "item.amulet_vis.found.name"),
                Map.entry("thaumcraft:amulet_vis;1;1", "item.amulet_vis.crafted.name"),
                Map.entry("thaumcraft:plate;3;3", "item.plate.void.name"));
        examples.forEach((source, expected) -> assertEquals(expected, BookItemPresentation.nameKey(source), source));
        assertNull(BookItemPresentation.nameKey("thaumcraft:baubles;1;7"), "Unknown metadata must not alias a valid ring");
        assertNull(BookItemPresentation.nameKey("minecraft:stone;1;0"));
    }

    @Test void sealKeySelectsTheActualOriginalVariantAndNotThePseudoRegistryName() {
        assertEquals("item.seal.blank.name", BookItemPresentation.nameKey("thaumcraft:seal;1;0"));
        assertEquals("item.seal.pickup.name", BookItemPresentation.nameKey("thaumcraft:seals;1;0;thaumcraft:pickup"));
        assertEquals("item.seal.breaker_advanced.name", BookItemPresentation.nameKey("thaumcraft:seals;1;0;thaumcraft:breaker_advanced"));
    }

    @Test void everyMappedRecipeDescriptorHasAnOriginalTranslationWithEnglishFallback() {
        var sources = new ArrayList<String>();
        legacySources(ResearchCatalog.read("/thaumcraft-port/book-recipes.json"), sources);
        var english = ResearchCatalog.read("/assets/thaumcraft/lang/en_us.json");
        for (String language : List.of("en_us", "ru_ru")) {
            var translations = ResearchCatalog.read("/assets/thaumcraft/lang/" + language + ".json");
            for (String source : sources) {
                String key = BookItemPresentation.nameKey(source);
                if (key != null) assertTrue(translations.has(key) || english.has(key), language + ": " + source + " -> " + key);
            }
        }
    }

    @Test void essentiaContainerNamesUseLatinAspectAndOriginalUncoloredAmountLine() {
        String crystal = "thaumcraft:crystal_essence;4;0;{Aspects:[{key:\"aer\",amount:1}]}";
        String phial = "thaumcraft:phial;1;1;{Aspects:[{key:\"aer\",amount:10}]}";
        String label = "thaumcraft:label;1;1;{Aspects:[{key:\"aer\",amount:1}]}";
        assertEquals("item.crystal_essence.name", key(BookItemPresentation.name(crystal)));
        assertArrayEquals(new Object[]{"Aer"}, ((TranslatableContents)BookItemPresentation.name(crystal).getContents()).getArgs());
        assertEquals("item.phial.filled.name", key(BookItemPresentation.name(phial)));
        assertArrayEquals(new Object[]{"Aer"}, ((TranslatableContents)BookItemPresentation.name(phial).getContents()).getArgs());
        assertEquals("item.label.filled.name", key(BookItemPresentation.name(label)));
        assertEquals("Aer x1", BookItemPresentation.details(crystal).getFirst().getString());
        assertEquals("Aer x10", BookItemPresentation.details(phial).getFirst().getString());
        assertEquals("Aer x1", BookItemPresentation.details(label).getFirst().getString());
        assertNull(BookItemPresentation.details(phial).getFirst().getStyle().getColor(), "Original PlayerEvents leaves the amount line uncolored");
    }

    @Test void enchantedRequirementKeepsItsInstructionAndExactEnchantment() {
        // Two original research JSON entries contain the redundant minecraft prefix.
        String silk = "thaumcraft:enchanted_placeholder;1;0;{ench:[{id:33s,lvl:1s}]}";
        String sharp = "minecraft:thaumcraft:enchanted_placeholder;1;0;{ench:[{id:16s,lvl:2s}]}";
        assertEquals("item.enchanted_placeholder.name", BookItemPresentation.nameKey(sharp));
        var silkLines = BookItemPresentation.details(silk);
        assertEquals(2, silkLines.size());
        assertEquals("item.enchanted_placeholder.text", key(silkLines.get(0)));
        assertTrue(silkLines.get(0).getStyle().isItalic());
        assertColor(ChatFormatting.DARK_AQUA, silkLines.get(0));
        assertEquals("enchantment.minecraft.silk_touch", key(silkLines.get(1)));
        assertTrue(silkLines.get(1).getSiblings().isEmpty(), "Silk Touch I has no numeral in the original tooltip");
        var sharpLine = BookItemPresentation.details(sharp).get(1);
        assertEquals("enchantment.minecraft.sharpness", key(sharpLine));
        assertEquals("enchantment.level.2", key(sharpLine.getSiblings().getLast()));
    }

    @Test void itemSpecificAndGlobalDetailsPreserveOriginalStyleWithoutInventingStatistics() {
        var amulet = BookItemPresentation.details("thaumcraft:amulet_vis;1;1");
        assertEquals(2, amulet.size());
        assertEquals("item.amulet_vis.text", key(amulet.getFirst()));
        assertColor(ChatFormatting.AQUA, amulet.getFirst());
        var life = BookItemPresentation.details("thaumcraft:verdant_charm;1;0;{type:1b}");
        assertEquals("item.verdant_charm.life.text", key(life.get(0)));
        assertColor(ChatFormatting.GOLD, life.get(0));
        assertEquals("tc.charge", key(life.get(1)));
        assertColor(ChatFormatting.YELLOW, life.get(1));
        var sustain = BookItemPresentation.details("thaumcraft:verdant_charm;1;0;{type:2b}");
        assertEquals("item.verdant_charm.sustain.text", key(sustain.get(0)));
        var goggles = BookItemPresentation.details("thaumcraft:fortress_helm;1;0;{goggles:1b}");
        assertEquals("item.goggles.name", key(goggles.getFirst()));
        assertColor(ChatFormatting.DARK_PURPLE, goggles.getFirst());
        for (int mask = 0; mask < 3; mask++) {
            var lines = BookItemPresentation.details("thaumcraft:fortress_helm;1;0;{mask:" + mask + "}");
            assertEquals("item.fortress_helm.mask." + mask, key(lines.getFirst()));
            assertColor(ChatFormatting.GOLD, lines.getFirst());
        }
        var runic = BookItemPresentation.details("thaumcraft:baubles;1;1;{TC.RUNIC:2b}");
        assertEquals("item.runic.charge", key(runic.getFirst()));
        assertColor(ChatFormatting.GOLD, runic.getFirst());
        assertEquals("name.RING", key(BookItemPresentation.details("thaumcraft:baubles;1;1").getFirst()));
        assertTrue(BookItemPresentation.details("thaumcraft:fortress_helm;1;0").isEmpty(),
                "This helper must not invent armor attributes for unported equipment");
    }

    @Test void baublesSlotComesLastAndRetainsOriginalSlotTranslationKeys() {
        // Recorded ItemStack.getTooltip(NORMAL) from the original TC6 + Baubles client.
        // Keep the Baubles keys so the imported EN/RU strings and resource packs both apply.
        Map<String, String> slots = Map.of(
                "thaumcraft:baubles;1;0", "AMULET", "thaumcraft:baubles;1;2", "BELT",
                "thaumcraft:baubles;1;3", "RING", "thaumcraft:amulet_vis;1;1", "AMULET",
                "thaumcraft:verdant_charm;1;0;{type:1b}", "CHARM");
        slots.forEach((source, slot) -> {
            var last = BookItemPresentation.details(source).getLast();
            assertEquals("name." + slot, key(last));
            assertColor(ChatFormatting.GOLD, last);
            assertEquals("Bauble (" + slot.charAt(0) + slot.substring(1).toLowerCase(java.util.Locale.ROOT) + ")",
                    ((TranslatableContents)last.getContents()).getFallback());
        });
        var apprentice = BookItemPresentation.details("thaumcraft:baubles;1;3");
        assertEquals(2, apprentice.size());
        assertEquals("tc.visdiscount", key(apprentice.getFirst()));
        assertColor(ChatFormatting.DARK_PURPLE, apprentice.getFirst());
        assertEquals(2, BookItemPresentation.details("thaumcraft:baubles;1;1;{TC.RUNIC:2b}").size());
        var russian = ResearchCatalog.read("/assets/thaumcraft/lang/ru_ru.json");
        assertEquals("Бижутерия (Кольцо)", russian.get("name.RING").getAsString());
        assertEquals("Бижутерия (Брелок)", russian.get("name.CHARM").getAsString());
    }

    @Test void infusionModificationsSurviveImportOnlyOnTheResult() {
        // ConfigRecipes 377-380, 388, 391 uses Object[]{NBT key, NBT value} outputs;
        // converting these into a copy of the central input must preserve that added tag.
        var recipes = ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        record Modification(String recipe, String tag, int value) {}
        var modifications = List.of(new Modification("HelmGoggles", "goggles", 1),
                new Modification("MaskGrinningDevil", "mask", 0), new Modification("MaskAngryGhost", "mask", 1),
                new Modification("MaskSippingFiend", "mask", 2), new Modification("VerdantHeartLife", "type", 1),
                new Modification("VerdantHeartSustain", "type", 2));
        for (var modification : modifications) {
            var recipe = recipes.getAsJsonObject("thaumcraft:" + modification.recipe());
            var input = BookItemPresentation.descriptor(recipe.getAsJsonObject("input").get("legacy").getAsString());
            var result = BookItemPresentation.descriptor(recipe.getAsJsonObject("result").get("legacy").getAsString());
            assertEquals(input.id(), result.id(), modification.recipe());
            assertEquals(input.metadata(), result.metadata(), modification.recipe());
            assertFalse(input.tag().contains(modification.tag()), "Input must not already have " + modification.recipe());
            assertTrue(result.tag().contains(modification.tag()), "Result lost " + modification.recipe());
            assertEquals(modification.value(), result.tag().getInt(modification.tag()), modification.recipe());
        }
    }

    @Test void voidseerTooltipUsesCurrentPermanentWarpWithOriginalThresholdsAndOrdering() {
        // ItemVoidseerCharm: int(min(permanentWarp,100)/100f*25), then discount/5.
        // PlayerEvents appends positive warp first, positive discount next, Baubles last.
        record Expected(int permanent, int warp, int discount) {}
        var cases = List.of(new Expected(0, 0, 0), new Expected(3, 0, 0), new Expected(4, 0, 1),
                new Expected(19, 0, 4), new Expected(20, 1, 5), new Expected(99, 4, 24),
                new Expected(100, 5, 25), new Expected(101, 5, 25), new Expected(10000, 5, 25));
        String source = "thaumcraft:voidseer_charm;1;0";
        for (boolean advanced : List.of(false, true)) {
            var captured = BookItemPresentation.catalogTooltip(source, advanced);
            for (var expected : cases) {
                var lines = BookItemPresentation.withPlayerContext(source, captured, expected.permanent());
                int extra = (expected.warp() > 0 ? 1 : 0) + (expected.discount() > 0 ? 1 : 0);
                assertEquals(captured.size() + extra, lines.size(), "permanent=" + expected.permanent());
                assertEquals(captured.getLast(), lines.getLast(), "Baubles slot must stay last, including ADVANCED mode");
                int index = captured.size() - 1;
                if (expected.warp() > 0) {
                    var line = lines.get(index++);
                    assertEquals("item.warping", key(line));
                    assertEquals(" " + expected.warp(), line.getSiblings().getLast().getString());
                    assertColor(ChatFormatting.DARK_PURPLE, line);
                }
                if (expected.discount() > 0) {
                    var line = lines.get(index);
                    assertEquals("tc.visdiscount", key(line));
                    assertEquals(": " + expected.discount() + "%", line.getSiblings().getLast().getString());
                    assertColor(ChatFormatting.DARK_PURPLE, line);
                }
            }
            assertEquals(captured, BookItemPresentation.withPlayerContext(source, captured, 0),
                    "Reading a high-warp player must not mutate the zero-warp oracle or bleed into another player");
        }
    }

    @Test void voidseerFallbackAddsNbtWarpToTheCurrentPlayerValueOnlyOnce() {
        String source = "thaumcraft:voidseer_charm;1;0;{TC.WARP:2b}";
        var base = new ArrayList<Component>();
        base.add(Component.literal("Voidseer's Pearl"));
        base.addAll(BookItemPresentation.details(source));
        assertEquals("item.voidseer_charm.text", key(base.get(1)));
        assertColor(ChatFormatting.DARK_BLUE, base.get(1));
        assertTrue(base.get(1).getStyle().isItalic());
        var actual = BookItemPresentation.withPlayerContext(source, base, 20);
        assertEquals("item.warping", key(actual.get(2)));
        assertEquals(" 3", actual.get(2).getSiblings().getLast().getString());
        assertEquals("tc.visdiscount", key(actual.get(3)));
        assertEquals("name.CHARM", key(actual.getLast()));
        assertEquals(5, actual.size());
        assertEquals(actual, BookItemPresentation.withPlayerContext(source, actual, 20));
        assertSame(base, BookItemPresentation.withPlayerContext("thaumcraft:baubles;1;1", base, 100),
                "This player-dependent rule belongs only to the original Voidseer's Pearl");
    }

    private static String key(Component text) {
        assertInstanceOf(TranslatableContents.class, text.getContents());
        return ((TranslatableContents)text.getContents()).getKey();
    }

    private static void assertColor(ChatFormatting color, Component text) {
        assertNotNull(text.getStyle().getColor());
        assertEquals(color.getColor().intValue(), text.getStyle().getColor().getValue());
    }

    private static void legacySources(JsonElement value, List<String> output) {
        if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("legacy")) output.add(object.get("legacy").getAsString());
            for (var member : object.entrySet()) legacySources(member.getValue(), output);
        } else if (value.isJsonArray()) for (var member : value.getAsJsonArray()) legacySources(member, output);
    }
}
