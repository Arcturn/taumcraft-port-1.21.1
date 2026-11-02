package thaumcraft.port.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.infusion.recipe.InfusionRecipe;
import thaumcraft.port.research.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemResearchGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void allGolemancyBookRecipesResolveToLiveRegisteredOutputsAndGates(GameTestHelper helper) {
        var entries = ResearchCatalog.all().stream().filter(entry -> entry.category().equals("GOLEMANCY")).toList();
        helper.assertValueEqual(entries.size(), 29, "Original golemancy research entry missing");
        var mapping = new HashMap<String, String>();
        for (String file : new String[]{"crafting-recipe-coverage.json", "infusion-crafting-coverage.json"})
            ResearchCatalog.read("/thaumcraft-port/" + file).getAsJsonObject("recipe_mapping").entrySet()
                    .forEach(entry -> mapping.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsString()));
        mapping.put("thaumcraft:golembell", "thaumcraft:golem_bell");
        var recipes = new HashSet<String>();
        for (var entry : entries) {
            var sections = new ArrayList<>(entry.stages());
            sections.addAll(entry.addenda());
            for (var section : sections) {
                recipes.addAll(ResearchCatalog.strings(section, "recipes"));
                for (String encoded : ResearchCatalog.strings(section, "required_item")) {
                    var requirement = LegacyRequirement.parse(encoded);
                    helper.assertFalse(requirement.craftKey().startsWith("unsupported:"),
                            "Unobtainable research item mapping: " + encoded);
                    var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(requirement.craftKey())));
                    helper.assertTrue(!stack.isEmpty() && requirement.matches().test(stack),
                            "Required golemancy item is not registered: " + encoded);
                }
            }
            var advancement = helper.getLevel().getServer().getAdvancements().get(
                    ResourceLocation.parse("thaumcraft:research/" + entry.key().toLowerCase(Locale.ROOT)));
            helper.assertTrue(advancement != null, "Research advancement missing: " + entry.key());
        }
        helper.assertValueEqual(recipes.size(), 24, "Original golemancy recipe link missing");
        for (String key : recipes) {
            var pages = BookRecipeCatalog.recipes(key);
            helper.assertValueEqual(pages.size(), 1, "Unresolved or ambiguous book recipe: " + key);
            var page = pages.getFirst();
            var expected = LegacyRequirement.parse(BookRecipeCatalog.output(page));
            if (key.equals("thaumcraft:GolemPress")) {
                helper.assertTrue(page.get("type").getAsString().contains("blueprint"), "Press became an ordinary crafting recipe");
                helper.assertTrue(expected.matches().test(new ItemStack(GolemContent.BUILDER.get())), "Press blueprint points at a placeholder item");
                continue;
            }
            var id = mapping.get(key.toLowerCase(Locale.ROOT));
            helper.assertTrue(id != null, "Book recipe has no live mapping: " + key);
            var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElse(null);
            helper.assertTrue(holder != null, "Live golemancy recipe is not loaded: " + id);
            var recipe = holder.value();
            var output = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(expected.matches().test(output), "Book and crafting result disagree: " + key);
            helper.assertValueEqual(output.getCount(), expected.count(), "Book and crafting output quantity disagree: " + key);
            String gate = recipe instanceof ArcaneRecipe arcane ? arcane.research()
                    : recipe instanceof CrucibleRecipe crucible ? crucible.research()
                    : recipe instanceof InfusionRecipe infusion ? infusion.research() : "";
            helper.assertValueEqual(gate, page.has("research") ? page.get("research").getAsString() : "",
                    "Original golemancy recipe research gate changed: " + key);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void clockworkCompletionUnlocksOriginalStarterSealsAndWoodExactlyOnce(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.THAUMONOMICON.get()));
        var state = ResearchStorage.get(player.getServer()).player(player.getUUID());
        for (String parent : ResearchCatalog.get("MINDCLOCKWORK").parents()) {
            var entry = ResearchCatalog.get(ResearchView.clean(parent));
            state.stage(entry.key(), entry.stages().size() + 1, false);
        }
        state.stage("MINDCLOCKWORK", 1, false);
        state.flag("!cognitio");
        state.flag("!victus");
        state.addKnowledge("OBSERVATION", "GOLEMANCY", 16);
        ResearchProgress.advance(player, "MINDCLOCKWORK", 1);
        helper.assertValueEqual(state.stage("MINDCLOCKWORK"), 2, "First paid stage did not unlock clockwork crafting");
        helper.assertTrue(state.knows("MINDCLOCKWORK@2"), "Clockwork recipe's stage gate is unreachable");
        helper.assertValueEqual(state.knowledge("OBSERVATION", "GOLEMANCY"), 0, "First stage did not consume one observation");
        state.addKnowledge("THEORY", "ARTIFICE", 32);
        state.addKnowledge("THEORY", "GOLEMANCY", 32);
        ResearchProgress.advance(player, "MINDCLOCKWORK", 2);
        for (String key : new String[]{"MINDCLOCKWORK", "CONTROLSEALS", "MATSTUDWOOD", "SEALCOLLECT", "SEALSTORE"})
            helper.assertTrue(state.knows(key), "Clockwork completion omitted its starter research chain: " + key);
        helper.assertValueEqual(state.knowledge("THEORY", "ARTIFICE"), 0, "Artifice theory payment changed");
        helper.assertValueEqual(state.knowledge("THEORY", "GOLEMANCY"), 0, "Golemancy theory payment changed");
        var before = ResearchCodec.write(state);
        int experience = player.totalExperience;
        ResearchProgress.advance(player, "MINDCLOCKWORK", 2);
        helper.assertValueEqual(ResearchCodec.write(state), before, "Repeated completion changed research or payments");
        helper.assertValueEqual(player.totalExperience, experience, "Repeated completion rewarded XP twice");
        helper.succeed();
    }

    private GolemResearchGameTests() { }
}
